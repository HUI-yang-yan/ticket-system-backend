package com.ticket.system.task;


import com.ticket.system.entity.TicketInventory;
import com.ticket.system.entity.Train;
import com.ticket.system.entity.TrainSegmentStock;
import com.ticket.system.entity.TrainStation;
import com.ticket.system.mapper.TrainSegmentStockMapper;
import com.ticket.system.service.StationService;
import com.ticket.system.service.TicketService;
import com.ticket.system.service.TrainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class TicketTask {

    @Autowired
    private TicketService ticketService;
    @Autowired
    private TrainService trainService;
    @Autowired
    private StationService stationService;
    @Autowired
    private TrainSegmentStockMapper trainSegmentStockMapper;

    @Scheduled(cron = "0 0 12 * * *")
    public void addTicket(){
        //首先根据余票配置表查询具体车次
        List<TicketInventory> list = ticketService.getAllTicketInventory();
        for (TicketInventory ticketInventory : list) {
            Train train = trainService.getTrainById(ticketInventory.getTrainId());
            //查询车次后根据数据查询车次车站表
            List<TrainStation> trainStations = stationService.getTrainStationTrainId(train.getId());
            trainStations.forEach(trainStation -> {
                if(trainStation.getDepartureTime()!=null){
                    //插入分段余票表
                    List<TrainSegmentStock> trainSegmentStocks = getTrainSegmentStock(ticketInventory,trainStation,train);
                    try {
                        trainSegmentStockMapper.insertBatch(trainSegmentStocks);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    @Scheduled(cron  = "0 0 12 * * *")
    public void deleteOldTicket(){

    }

    private List<TrainSegmentStock> getTrainSegmentStock(TicketInventory ticketInventory, TrainStation trainStation, Train train) {
        List<TrainSegmentStock> trainSegmentStocks = new ArrayList<>();
        List<LocalDate> dates = dateList(LocalDate.now(), 7);
        dates.forEach(date -> {
            int endIndex = trainStation.getStationIndex() + 1;
            TrainStation trainStationEnd = stationService.getByTrainIdAndIndex(train.getId(),endIndex);
            if (trainStationEnd == null) {
                return;
            }

            TrainSegmentStock trainSegmentStock = new TrainSegmentStock();
            trainSegmentStock.setTrainId(train.getId());
            trainSegmentStock.setStartStationId(trainStation.getStationId());
            trainSegmentStock.setPrice(calculateSegmentPriceSimple(trainStation,trainStationEnd));
            trainSegmentStock.setTravelDate(date);
            trainSegmentStock.setSeatType(ticketInventory.getSeatType());
            trainSegmentStock.setStock(ticketInventory.getTotalCount());
            trainSegmentStock.setCreateTime(LocalDateTime.now());
            trainSegmentStock.setUpdateTime(LocalDateTime.now());
            trainSegmentStocks.add(trainSegmentStock);
        });
        return trainSegmentStocks;
    }

    private BigDecimal calculateSegmentPriceSimple(
            TrainStation startStation,
            TrainStation endStation
    ) {
        int distance = endStation.getDistance()
                - startStation.getDistance();

        if (distance <= 0) {
            throw new IllegalStateException("站点里程配置错误");
        }

        BigDecimal price = BigDecimal.valueOf(distance)
                .multiply(BigDecimal.valueOf(0.46));

        // 最低价保护
        if (price.compareTo(BigDecimal.ONE) < 0) {
            price = BigDecimal.ONE;
        }

        return price.setScale(2, RoundingMode.HALF_UP);
    }



    public List<LocalDate> dateList(LocalDate startDate, int days) {
        List<LocalDate> dates = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            dates.add(startDate.plusDays(i));
        }
        return dates;
    }
}
