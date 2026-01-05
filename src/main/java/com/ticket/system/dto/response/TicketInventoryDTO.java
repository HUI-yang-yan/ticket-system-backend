package com.ticket.system.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;

@Data
public class TicketInventoryDTO {

    private Long id;
    private Long trainId;
    private String trainNumber;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date departureDate;
    private String departureDateText;
    private String seatType;
    private String seatTypeName;
    private Integer totalCount;
    private Integer availableCount;
    private Integer soldCount;
    private Integer lockedCount;
    private Double price;
    private String priceText;
    private Integer version;
    private Date createTime;
    private Date updateTime;

    // 格式化出发日期
    public String getDepartureDateText() {
        if (departureDate == null) {
            return "";
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(departureDate);
    }

    // 座位类型中文名称
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

    // 计算售出数量
    public Integer getSoldCount() {
        if (totalCount != null && availableCount != null) {
            return totalCount - availableCount;
        }
        return 0;
    }

    // 获取库存百分比
    public Double getInventoryPercent() {
        if (totalCount != null && totalCount > 0) {
            return (availableCount != null ? availableCount.doubleValue() : 0) / totalCount * 100;
        }
        return 0.0;
    }
}