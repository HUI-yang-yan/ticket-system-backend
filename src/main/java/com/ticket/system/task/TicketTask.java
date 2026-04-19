package com.ticket.system.task;

import com.ticket.system.entity.TicketInventory;
import com.ticket.system.entity.Train;
import com.ticket.system.entity.TrainSegmentStock;
import com.ticket.system.entity.TrainStation;
import com.ticket.system.mapper.TrainSegmentStockMapper;
import com.ticket.system.service.StationService;
import com.ticket.system.service.TicketService;
import com.ticket.system.service.TrainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
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

    /**
     * 补票任务：根据 ticket_inventory 配置，生成分段余票数据到 TrainSegmentStock 表
     * 每天12点执行，根据配置生成未来7天的票
     */
    @Scheduled(cron = "0 0 12 * * *")
    public void addTicket() {
        log.info("========== 开始执行补票任务 ==========");

        // 查询所有票务配置
        List<TicketInventory> inventories = ticketService.getAllTicketInventory();
        log.info("票务配置总数: {}", inventories.size());

        int successCount = 0;
        int failCount = 0;

        for (TicketInventory inventory : inventories) {
            try {
                // 检查配置是否启用
                if (inventory.getStatus() != null && inventory.getStatus() != 0) {
                    log.debug("票务配置已禁用，跳过: trainId={}, seatType={}",
                            inventory.getTrainId(), inventory.getSeatType());
                    continue;
                }

                // 生成未来7天的票
                List<TrainSegmentStock> stocks = generateSegmentStocks(inventory);
                if (!stocks.isEmpty()) {
                    trainSegmentStockMapper.insertBatch(stocks);
                    successCount++;
                    log.info("补票成功: trainId={}, seatType={}, 生成{}条分段余票",
                            inventory.getTrainId(), inventory.getSeatType(), stocks.size());
                }
            } catch (Exception e) {
                failCount++;
                log.error("补票失败: trainId={}, seatType={}, error={}",
                        inventory.getTrainId(), inventory.getSeatType(), e.getMessage());
            }
        }

        log.info("========== 补票任务完成: 成功={}, 失败={} ==========", successCount, failCount);
    }

    /**
     * 根据票务配置生成分段余票
     */
    private List<TrainSegmentStock> generateSegmentStocks(TicketInventory inventory) {
        List<TrainSegmentStock> stocks = new ArrayList<>();

        Train train = trainService.getTrainById(inventory.getTrainId());
        if (train == null) {
            log.warn("列车不存在: trainId={}", inventory.getTrainId());
            return stocks;
        }

        // 获取列车的所有站点
        List<TrainStation> trainStations = stationService.getTrainStationTrainId(train.getId());
        if (trainStations == null || trainStations.isEmpty()) {
            log.warn("列车站点信息为空: trainId={}", inventory.getTrainId());
            return stocks;
        }

        // 预售期天数配置
        int presaleDays = 7; // 默认7天
        List<LocalDate> dates = dateList(LocalDate.now(), presaleDays);

        // 遍历每个出发日期
        for (LocalDate date : dates) {
            // 遍历每个站点区间（从起始站到终点站）
            for (int i = 0; i < trainStations.size() - 1; i++) {
                TrainStation startStation = trainStations.get(i);
                if (startStation.getDepartureTime() == null) {
                    continue;
                }

                TrainStation endStation = trainStations.get(i + 1);

                TrainSegmentStock stock = new TrainSegmentStock();
                stock.setTrainId(train.getId());
                stock.setStartStationId(startStation.getStationId());
                stock.setSeatType(inventory.getSeatType());
                stock.setTravelDate(date);
                stock.setStock(inventory.getTotalCount());
                stock.setPrice(calculateSegmentPrice(startStation, endStation));

                stocks.add(stock);
            }
        }

        return stocks;
    }

    /**
     * 计算分段价格
     */
    private BigDecimal calculateSegmentPrice(TrainStation startStation, TrainStation endStation) {
        if (startStation.getDistance() == null || endStation.getDistance() == null) {
            return BigDecimal.ONE; // 默认最低价
        }

        int distance = endStation.getDistance() - startStation.getDistance();
        if (distance <= 0) {
            return BigDecimal.ONE;
        }

        // 票价 = 里程 * 0.46 元/公里
        BigDecimal price = BigDecimal.valueOf(distance)
                .multiply(BigDecimal.valueOf(0.46));

        // 最低价保护
        if (price.compareTo(BigDecimal.ONE) < 0) {
            price = BigDecimal.ONE;
        }

        return price.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 删除过期余票数据
     * 每天凌晨2点执行，删除昨天之前的数据
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void deleteOldTicket() {
        log.info("========== 开始清理过期余票数据 ==========");
        // TODO: 实现清理逻辑
        // 建议：DELETE FROM train_segment_stock WHERE travel_date < CURRENT_DATE - 7
        log.info("========== 清理过期余票完成 ==========");
    }

    /**
     * 生成日期列表
     */
    public List<LocalDate> dateList(LocalDate startDate, int days) {
        List<LocalDate> dates = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            dates.add(startDate.plusDays(i));
        }
        return dates;
    }
}
