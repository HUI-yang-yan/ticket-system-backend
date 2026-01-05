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

        for (Train train : trains) {

            TrainStation startStation = trainStationMapper.selectByTrainAndStation(train.getId(), queryDTO.getDepartureStationId());
            TrainStation endStation = trainStationMapper.selectByTrainAndStation(train.getId(), queryDTO.getArrivalStationId());

            Integer startIndex = startStation.getStationIndex();

            Integer endIndex = endStation.getStationIndex();

            for (String seatType : getSeatTypes()) {

                String redisKey = String.format(
                        "ticket:stock:%d:%s:%s:%d-%d",
                        train.getId(),
                        queryDTO.getDepartureDate(),
                        seatType,
                        startIndex,
                        endIndex
                );

                Integer available = (Integer) redisUtil.get(redisKey);

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

                if (available > 0) {
                    TicketInfoDTO dto = new TicketInfoDTO();
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
    public boolean purchaseTicket(Long trainId, Long userId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String departureDate = sdf.format(new Date());
        String seatType = TicketConstant.SEAT_TYPE_SECOND;

        // 检查是否已锁定
        String seatLockKey = RedisConstant.TICKET_SEAT_PREFIX + "lock:" + trainId + ":" + departureDate + ":" + userId;
        Object lockStatus = redisUtil.get(seatLockKey);
        if (lockStatus == null) {
            throw new BusinessException(ErrorCode.SEAT_LOCK_FIRST.getCode(), "请先锁定座位");
        }

        // 实际购买逻辑
        // 这里简化处理，实际应该创建订单等

        // 删除锁定信息
        redisUtil.delete(seatLockKey);

        return true;
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

        return trainSegmentStockMapper.selectSumPrice(
                trainId,
                departureDate,
                seatType,
                startIndex,
                endIndex
        );
    }





}