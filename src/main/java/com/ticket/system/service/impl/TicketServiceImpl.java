package com.ticket.system.service.impl;

import com.ticket.system.common.constant.RedisConstant;
import com.ticket.system.common.constant.TicketConstant;
import com.ticket.system.common.exception.BusinessException;
import com.ticket.system.common.exception.ErrorCode;
import com.ticket.system.common.util.RedisUtil;
import com.ticket.system.dto.request.TicketQueryDTO;
import com.ticket.system.dto.response.TicketInfoDTO;
import com.ticket.system.entity.*;
import com.ticket.system.mapper.*;
import com.ticket.system.service.TicketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TicketServiceImpl implements TicketService {

    @Autowired
    private TicketInventoryMapper ticketInventoryMapper;

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private StationMapper stationMapper;

    @Autowired
    private TrainStationMapper trainStationMapper;

    @Autowired
    private TrainSegmentStockMapper  trainSegmentStockMapper;


    @Override
    public List<TicketInfoDTO> queryTickets(TicketQueryDTO queryDTO) {
        Station departureStation = stationMapper.selectById(queryDTO.getDepartureStationId());
        Station arrivalStation = stationMapper.selectById(queryDTO.getArrivalStationId());

        List<Train> trains =
                trainMapper.selectByRoute(queryDTO.getDepartureStationId(), queryDTO.getArrivalStationId());

        List<TicketInfoDTO> result = new ArrayList<>();
        boolean hasDate = queryDTO.getDepartureDate() != null;

        for (Train train : trains) {

            TrainStation startStation = trainStationMapper.selectByTrainAndStation(train.getId(), queryDTO.getDepartureStationId());
            TrainStation endStation = trainStationMapper.selectByTrainAndStation(train.getId(), queryDTO.getArrivalStationId());

            Integer startIndex = startStation.getStationIndex();

            Integer endIndex = endStation.getStationIndex();

            for (String seatType : getSeatTypes()) {

                Integer available;
                if (hasDate) {
                    String redisKey = String.format(
                            "ticket:stock:%d:%s:%s:%d-%d",
                            train.getId(),
                            queryDTO.getDepartureDate(),
                            seatType,
                            startIndex,
                            endIndex
                    );

                    available = (Integer) redisUtil.get(redisKey);

                    if (available == null) {
                        available = trainSegmentStockMapper.selectMinStock(
                                train.getId(),
                                queryDTO.getDepartureDate(),
                                seatType,
                                startIndex,
                                endIndex
                        );
                        available = Math.max(available, 0);
                        redisUtil.set(redisKey, available, 5, TimeUnit.MINUTES);
                    }
                } else {
                    available = trainSegmentStockMapper.selectMinStockWithoutDate(
                            train.getId(),
                            seatType,
                            startIndex,
                            endIndex
                    );
                    available = Math.max(available, 0);
                }

                if (available > 0) {
                    TicketInfoDTO dto = new TicketInfoDTO();
                    // 查询区间库存ID
                    Long segmentStockId;
                    if (hasDate) {
                        segmentStockId = trainSegmentStockMapper.selectSegmentStockId(
                                train.getId(),
                                queryDTO.getDepartureDate(),
                                seatType,
                                startIndex
                        );
                    } else {
                        segmentStockId = trainSegmentStockMapper.selectSegmentStockIdWithoutDate(
                                train.getId(),
                                seatType,
                                startIndex
                        );
                    }
                    dto.setId(segmentStockId);
                    dto.setTrainId(train.getId());
                    dto.setTrainNumber(train.getTrainNumber());
                    dto.setTrainType(train.getTrainType());
                    dto.setDepartureStationId(queryDTO.getDepartureStationId());
                    dto.setDepartureStationName(departureStation.getStationName());
                    dto.setArrivalStationId(queryDTO.getArrivalStationId());
                    dto.setArrivalStationName(arrivalStation.getStationName());
                    dto.setDepartureDate(queryDTO.getDepartureDate());
                    dto.setDepartureTime(startStation.getDepartureTime());
                    dto.setArrivalTime(endStation.getArrivalTime());
                    dto.setSeatType(seatType);
                    dto.setAvailableCount(available);
                    dto.setPrice(
                            calculatePrice(
                                    train.getId(),
                                    startIndex,
                                    endIndex,
                                    queryDTO.getDepartureDate(),
                                    seatType
                            )
                    );

                    result.add(dto);
                }
            }
        }
        return result;
    }


    @Override
    public TicketInfoDTO getTicketDetail(Long ticketId) {
        return null;
    }

    @Override
    public boolean unlockTicket(Long ticketId, Long userId) {
        // 释放锁定
        return true;
    }

    @Override
    @Transactional
    public boolean purchaseTicket(Long trainId, Long userId, String departureDate, String seatType,
                                   Long startStationId, Long endStationId) {
        // 1. 获取分布式锁（按车次+日期+座位类型，防止同一座位类型超卖）
        String lockKey = RedisConstant.TICKET_LOCK_PREFIX + trainId + ":" + departureDate + ":" + seatType;
        String lockValue = UUID.randomUUID().toString();
        try {
            boolean locked = redisUtil.lock(lockKey, lockValue, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(), "系统繁忙，请稍后重试");
            }

            // 2. 查询当前库存（FOR UPDATE 行锁）
            TicketInventory inventory = ticketInventoryMapper.selectForUpdate(trainId, departureDate, seatType);
            if (inventory == null) {
                throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH.getCode(), "票务信息不存在");
            }

            // 3. 检查库存是否充足
            if (inventory.getAvailableCount() <= 0) {
                throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH.getCode(), "库存不足");
            }

            // 4. 扣减库存（乐观锁）
            int updated = ticketInventoryMapper.reduceInventory(
                    inventory.getId(),
                    inventory.getAvailableCount() - 1,
                    inventory.getVersion());
            if (updated == 0) {
                throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH.getCode(), "库存扣减失败，请重试");
            }

            // 5. 更新 Redis 缓存（设置key包含起止站信息）
            String cacheKey = String.format("ticket:stock:%d:%s:%s:%d-%d",
                    trainId, departureDate, seatType, startStationId, endStationId);
            redisUtil.set(cacheKey, inventory.getAvailableCount() - 1, 5, TimeUnit.MINUTES);

            // 6. 清理座位锁定信息
            String seatLockKey = RedisConstant.TICKET_SEAT_PREFIX + "lock:" + trainId + ":" + departureDate + ":" + userId;
            redisUtil.delete(seatLockKey);

            log.info("购票成功：trainId={}, userId={}, departureDate={}, seatType={}, 剩余库存={}",
                    trainId, userId, departureDate, seatType, inventory.getAvailableCount() - 1);
            return true;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("购票异常：trainId={}, userId={}", trainId, userId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(), "购票失败：" + e.getMessage());
        } finally {
            // 释放分布式锁
            redisUtil.unlock(lockKey, lockValue);
        }
    }

    @Override
    public void syncTicketInventory() {
        // 同步票务库存到Redis
        log.info("开始同步票务库存到Redis");

        // 这里应该实现定时同步逻辑
        // 简化处理

        log.info("票务库存同步完成");
    }

    @Override
    public void addTicketsByHands(List<Long> ticketIds) {
        TicketInventory ticketInventory = ticketInventoryMapper.selectByTrainIds(ticketIds);
    }

    @Override
    public List<TicketInventory> getAllTicketInventory() {
        return ticketInventoryMapper.selectAll();
    }

    private List<String> getSeatTypes() {
        return Arrays.asList(
                TicketConstant.SEAT_TYPE_BUSINESS,
                TicketConstant.SEAT_TYPE_FIRST,
                TicketConstant.SEAT_TYPE_SECOND,
                TicketConstant.SEAT_TYPE_SOFT_SLEEPER,
                TicketConstant.SEAT_TYPE_HARD_SLEEPER
        );
    }

    private BigDecimal calculatePrice(Long trainId,
                                   Integer startIndex,
                                   Integer endIndex,
                                   LocalDate departureDate,
                                   String seatType) {
        if (departureDate != null) {
            return trainSegmentStockMapper.selectSumPrice(
                    trainId,
                    departureDate,
                    seatType,
                    startIndex,
                    endIndex
            );
        } else {
            return trainSegmentStockMapper.selectSumPriceWithoutDate(
                    trainId,
                    seatType,
                    startIndex,
                    endIndex
            );
        }
    }





}