package com.ticket.system.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

@Data
public class TicketInfoDTO {

    private Long id;
    private Long trainId;
    private String trainNumber;
    private String trainType;
    private Long departureStationId;
    private String departureStationName;
    private Long arrivalStationId;
    private String arrivalStationName;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private LocalDate departureDate;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private String seatType;
    private String seatTypeName;
    private Integer availableCount;
    private BigDecimal price;
    private String priceText;
    private Integer status;

    // 获取座位类型中文名称
    public String getSeatTypeName() {
        switch (seatType) {
            case "BUSINESS": return "商务座";
            case "FIRST": return "一等座";
            case "SECOND": return "二等座";
            case "SOFT_SLEEPER": return "软卧";
            case "HARD_SLEEPER": return "硬卧";
            case "SOFT_SEAT": return "软座";
            case "HARD_SEAT": return "硬座";
            default: return "其他";
        }
    }

    // 格式化价格
    public String getPriceText() {
        if (price == null) {
            return "0.00";
        }
        return String.format("%.2f", price);
    }
}