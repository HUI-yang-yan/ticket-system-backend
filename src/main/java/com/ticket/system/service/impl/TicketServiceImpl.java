package com.ticket.system.service.impl;

import com.ticket.system.common.constant.RedisConstant;
import com.ticket.system.common.constant.TicketConstant;
import com.ticket.system.common.exception.BusinessException;
import com.ticket.system.common.exception.ErrorCode;
import com.ticket.system.common.util.RedisUtil;
import com.ticket.system.dto.request.TicketInventoryCreateDTO;
import com.ticket.system.dto.request.TicketInventoryQueryDTO;
import com.ticket.system.dto.request.TicketQueryDTO;
import com.ticket.system.dto.response.TicketInfoDTO;
import com.ticket.system.dto.response.TicketInventoryDTO;
import com.ticket.system.entity.*;
import com.ticket.system.mapper.*;
import com.ticket.system.service.TicketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
        // 1. 预查询车站信息
        Station departureStation = stationMapper.selectById(queryDTO.getDepartureStationId());
        Station arrivalStation = stationMapper.selectById(queryDTO.getArrivalStationId());

        // 2. 查询符合条件的车次
        List<Train> trains = trainMapper.selectByRoute(
                queryDTO.getDepartureStationId(),
                queryDTO.getArrivalStationId());

        if (trains.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 预取所有车次的车站索引信息
        Map<Long, TrainStationIndexes> trainStationIndexesMap = buildTrainStationIndexesMap(
                trains,
                queryDTO.getDepartureStationId(),
                queryDTO.getArrivalStationId());

        // 4. 预聚合每个 (train, seatType) 的库存信息
        boolean hasDate = queryDTO.getDepartureDate() != null;
        List<TicketStockInfo> stockInfoList = preAggregateStockInfo(
                trains,
                trainStationIndexesMap,
                hasDate,
                queryDTO.getDepartureDate());

        // 5. 只保留有库存的数据
        stockInfoList = stockInfoList.stream()
                .filter(info -> info.available > 0)
                .toList();

        // 6. 构建结果
        return buildTicketResults(
                trains,
                trainStationIndexesMap,
                stockInfoList,
                departureStation,
                arrivalStation,
                queryDTO.getDepartureDate());
    }

    /**
     * 预取所有车次的车站索引信息
     */
    private Map<Long, TrainStationIndexes> buildTrainStationIndexesMap(
            List<Train> trains,
            Long departureStationId,
            Long arrivalStationId) {

        Map<Long, TrainStationIndexes> result = new HashMap<>();
        for (Train train : trains) {
            TrainStation startStation = trainStationMapper.selectByTrainAndStation(
                    train.getId(), departureStationId);
            TrainStation endStation = trainStationMapper.selectByTrainAndStation(
                    train.getId(), arrivalStationId);

            result.put(train.getId(), new TrainStationIndexes(
                    startStation.getStationIndex(),
                    endStation.getStationIndex(),
                    startStation.getDepartureTime(),
                    endStation.getArrivalTime()));
        }
        return result;
    }

    /**
     * 预聚合每个 (train, seatType) 的库存和价格
     */
    private List<TicketStockInfo> preAggregateStockInfo(
            List<Train> trains,
            Map<Long, TrainStationIndexes> stationIndexesMap,
            boolean hasDate,
            LocalDate departureDate) {

        List<TicketStockInfo> result = new ArrayList<>();

        for (Train train : trains) {
            TrainStationIndexes indexes = stationIndexesMap.get(train.getId());
            if (indexes == null) continue;

            for (String seatType : getSeatTypes()) {
                Integer available = fetchAvailableCount(
                        train.getId(),
                        indexes,
                        seatType,
                        hasDate,
                        departureDate);

                Long segmentStockId = fetchSegmentStockId(
                        train.getId(),
                        seatType,
                        indexes.startIndex,
                        hasDate,
                        departureDate);

                BigDecimal price = calculatePrice(
                        train.getId(),
                        indexes.startIndex,
                        indexes.endIndex,
                        departureDate,
                        seatType);

                result.add(new TicketStockInfo(
                        train.getId(),
                        seatType,
                        available,
                        segmentStockId,
                        price));
            }
        }
        return result;
    }

    /**
     * 获取可用库存（优先从Redis缓存获取）
     */
    private Integer fetchAvailableCount(
            Long trainId,
            TrainStationIndexes indexes,
            String seatType,
            boolean hasDate,
            LocalDate departureDate) {

        if (hasDate) {
            String redisKey = String.format(
                    "ticket:stock:%d:%s:%s:%d-%d",
                    trainId,
                    departureDate,
                    seatType,
                    indexes.startIndex,
                    indexes.endIndex);

            Integer cached = (Integer) redisUtil.get(redisKey);
            if (cached != null) {
                return cached;
            }

            Integer available = trainSegmentStockMapper.selectMinStock(
                    trainId,
                    departureDate,
                    seatType,
                    indexes.startIndex,
                    indexes.endIndex);
            available = Math.max(available, 0);
            redisUtil.set(redisKey, available, 5, TimeUnit.MINUTES);
            return available;
        } else {
            Integer available = trainSegmentStockMapper.selectMinStockWithoutDate(
                    trainId,
                    seatType,
                    indexes.startIndex,
                    indexes.endIndex);
            return Math.max(available, 0);
        }
    }

    /**
     * 获取区间库存ID
     */
    private Long fetchSegmentStockId(
            Long trainId,
            String seatType,
            Integer startIndex,
            boolean hasDate,
            LocalDate departureDate) {

        if (hasDate) {
            return trainSegmentStockMapper.selectSegmentStockId(
                    trainId,
                    departureDate,
                    seatType,
                    startIndex);
        } else {
            return trainSegmentStockMapper.selectSegmentStockIdWithoutDate(
                    trainId,
                    seatType,
                    startIndex);
        }
    }

    /**
     * 构建查询结果
     */
    private List<TicketInfoDTO> buildTicketResults(
            List<Train> trains,
            Map<Long, TrainStationIndexes> stationIndexesMap,
            List<TicketStockInfo> stockInfoList,
            Station departureStation,
            Station arrivalStation,
            LocalDate departureDate) {

        // 构建 trainId -> Train 的映射
        Map<Long, Train> trainMap = new HashMap<>();
        for (Train train : trains) {
            trainMap.put(train.getId(), train);
        }

        List<TicketInfoDTO> result = new ArrayList<>();
        for (TicketStockInfo stockInfo : stockInfoList) {
            Train train = trainMap.get(stockInfo.trainId);
            if (train == null) continue;

            TrainStationIndexes indexes = stationIndexesMap.get(stockInfo.trainId);
            if (indexes == null) continue;

            TicketInfoDTO dto = new TicketInfoDTO();
            dto.setId(stockInfo.segmentStockId);
            dto.setTrainId(train.getId());
            dto.setTrainNumber(train.getTrainNumber());
            dto.setTrainType(train.getTrainType());
            dto.setDepartureStationId(departureStation.getId());
            dto.setDepartureStationName(departureStation.getStationName());
            dto.setArrivalStationId(arrivalStation.getId());
            dto.setArrivalStationName(arrivalStation.getStationName());
            dto.setDepartureDate(departureDate);
            dto.setDepartureTime(indexes.departureTime);
            dto.setArrivalTime(indexes.arrivalTime);
            dto.setSeatType(stockInfo.seatType);
            dto.setAvailableCount(stockInfo.available);
            dto.setPrice(stockInfo.price);

            result.add(dto);
        }
        return result;
    }

    /**
     * 车次车站索引信息
     */
    private record TrainStationIndexes(
            Integer startIndex,
            Integer endIndex,
            LocalTime departureTime,
            LocalTime arrivalTime
    ) {}

    /**
     * 票务库存信息
     */
    private record TicketStockInfo(
            Long trainId,
            String seatType,
            Integer available,
            Long segmentStockId,
            BigDecimal price
    ) {}


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
            TicketInventory inventory = ticketInventoryMapper.selectForUpdate(trainId, seatType);
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
        List<TicketInventory> inventories = ticketInventoryMapper.selectByTrainIds(ticketIds);
        // 实现补票逻辑
        for (TicketInventory inventory : inventories) {
            log.info("补票配置: trainId={}, seatType={}, totalCount={}, availableCount={}",
                    inventory.getTrainId(), inventory.getSeatType(),
                    inventory.getTotalCount(), inventory.getAvailableCount());
        }
    }

    @Override
    public List<TicketInventory> getAllTicketInventory() {
        return ticketInventoryMapper.selectAll();
    }

    @Override
    @Transactional
    public TicketInventoryDTO createTicketInventory(TicketInventoryCreateDTO dto) {
        // 检查是否已存在
        TicketInventory existing = ticketInventoryMapper.selectByTrainAndType(
                dto.getTrainId(), dto.getSeatType());
        if (existing != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该票务配置已存在");
        }

        // 检查列车是否存在
        Train train = trainMapper.selectById(dto.getTrainId());
        if (train == null) {
            throw new BusinessException(ErrorCode.TRAIN_NOT_EXIST.getCode(), "列车不存在");
        }

        TicketInventory inventory = new TicketInventory();
        BeanUtils.copyProperties(dto, inventory);
        if (inventory.getPrice() == null) {
            inventory.setPrice(new BigDecimal("0"));
        }
        inventory.setVersion(0);
        inventory.setStatus(dto.getStatus() != null ? dto.getStatus() : 0);
        inventory.setCreateTime(LocalDateTime.now());
        inventory.setUpdateTime(LocalDateTime.now());

        ticketInventoryMapper.insert(inventory);

        TicketInventoryDTO result = new TicketInventoryDTO();
        BeanUtils.copyProperties(inventory, result);
        result.setTrainNumber(train.getTrainNumber());

        log.info("创建票务配置成功: id={}, trainId={}, seatType={}",
                inventory.getId(), inventory.getTrainId(), inventory.getSeatType());
        return result;
    }

    @Override
    @Transactional
    public TicketInventoryDTO updateTicketInventory(TicketInventoryCreateDTO dto) {
        TicketInventory inventory = ticketInventoryMapper.selectByTrainAndType(
                dto.getTrainId(), dto.getSeatType());
        if (inventory == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "票务配置不存在");
        }

        if (dto.getTotalCount() != null) {
            inventory.setTotalCount(dto.getTotalCount());
        }
        if (dto.getAvailableCount() != null) {
            inventory.setAvailableCount(dto.getAvailableCount());
        }
        if (dto.getPrice() != null) {
            inventory.setPrice(dto.getPrice());
        }
        if (dto.getStatus() != null) {
            inventory.setStatus(dto.getStatus());
        }
        inventory.setUpdateTime(LocalDateTime.now());

        ticketInventoryMapper.update(inventory);

        TicketInventoryDTO result = new TicketInventoryDTO();
        BeanUtils.copyProperties(inventory, result);
        Train train = trainMapper.selectById(inventory.getTrainId());
        if (train != null) {
            result.setTrainNumber(train.getTrainNumber());
        }

        log.info("更新票务配置成功: id={}, trainId={}, seatType={}",
                inventory.getId(), inventory.getTrainId(), inventory.getSeatType());
        return result;
    }

    @Override
    @Transactional
    public boolean deleteTicketInventory(Long id) {
        TicketInventory inventory = ticketInventoryMapper.selectById(id);
        if (inventory == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "票务配置不存在");
        }
        int result = ticketInventoryMapper.delete(id);
        log.info("删除票务配置: id={}, result={}", id, result);
        return result > 0;
    }

    @Override
    public TicketInventoryDTO getTicketInventoryById(Long id) {
        TicketInventory inventory = ticketInventoryMapper.selectById(id);
        if (inventory == null) {
            return null;
        }
        TicketInventoryDTO dto = new TicketInventoryDTO();
        BeanUtils.copyProperties(inventory, dto);
        Train train = trainMapper.selectById(inventory.getTrainId());
        if (train != null) {
            dto.setTrainNumber(train.getTrainNumber());
        }
        return dto;
    }

    @Override
    public List<TicketInventoryDTO> queryTicketInventories(TicketInventoryQueryDTO queryDTO) {
        List<TicketInventory> inventories = ticketInventoryMapper.selectByCondition(
                queryDTO.getTrainId(),
                queryDTO.getSeatType(),
                queryDTO.getStatus()
        );

        List<TicketInventoryDTO> results = new ArrayList<>();
        for (TicketInventory inventory : inventories) {
            TicketInventoryDTO dto = new TicketInventoryDTO();
            BeanUtils.copyProperties(inventory, dto);
            Train train = trainMapper.selectById(inventory.getTrainId());
            if (train != null) {
                dto.setTrainNumber(train.getTrainNumber());
            }
            results.add(dto);
        }
        return results;
    }

    @Override
    @Transactional
    public void batchCreateTicketInventory(List<TicketInventoryCreateDTO> dtos) {
        for (TicketInventoryCreateDTO dto : dtos) {
            createTicketInventory(dto);
        }
        log.info("批量创建票务配置: count={}", dtos.size());
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