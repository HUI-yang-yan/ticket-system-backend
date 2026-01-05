package com.ticket.system.config;

import com.ticket.system.common.util.DateUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "ticket.presale")
@Data
public class TicketPreSaleConfig {

    // 预售期天数（默认30天）
    private int days = 30;

    // 放票时间（默认8:00）
    private String releaseTime = "08:00";

    // 不同车次的放票策略
    private Map<String, String> trainReleaseStrategy = new HashMap<>();

    // 退票返库时间（分钟）
    private int returnToInventoryMinutes = 15;

    /**
     * 获取指定车次的放票时间
     */
    public String getTrainReleaseTime(String trainNumber) {
        return trainReleaseStrategy.getOrDefault(trainNumber, releaseTime);
    }

    /**
     * 检查是否可以购票
     */
    public boolean canPurchase(LocalDate departureDate) {
        LocalDate today = LocalDate.now();
        LocalDate maxPurchaseDate = today.plusDays(days);
        return !departureDate.isAfter(maxPurchaseDate);
    }


}