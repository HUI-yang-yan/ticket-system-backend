package com.ticket.system.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Data
public class OrderInfoDTO {

    private Long id;
    private String orderNumber;
    private Long userId;
    private String username;
    private Long trainId;
    private String trainNumber;
    private Long departureStationId;
    private String departureStationName;
    private Long arrivalStationId;
    private String arrivalStationName;
    private LocalDateTime departureDate;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private String seatType;
    private String seatTypeName;
    private String passengerIdCard;
    private String passengerRealName;
    private BigDecimal ticketPrice;
    private String ticketPriceText;
    private Integer orderStatus;
    private String orderStatusText;
    private Integer payStatus;
    private String payStatusText;
    private Date payTime;
    private Date expireTime;
    private Date createTime;
    private Date updateTime;

    // 订单状态文本
    public String getOrderStatusText() {
        switch (orderStatus) {
            case 0: return "待支付";
            case 1: return "已支付";
            case 2: return "已取消";
            case 3: return "已完成";
            default: return "未知";
        }
    }

    // 支付状态文本
    public String getPayStatusText() {
        switch (payStatus) {
            case 0: return "未支付";
            case 1: return "已支付";
            default: return "未知";
        }
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
    public String getTicketPriceText() {
        if (ticketPrice == null) {
            return "0.00";
        }
        return "¥" + ticketPrice.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
    }

    // 检查订单是否过期
    public boolean isExpired() {
        return new Date().after(expireTime);
    }

    // 获取剩余支付时间（秒）
    public Long getRemainingPayTime() {
        if (orderStatus == 0) {
            long remaining = expireTime.getTime() - System.currentTimeMillis();
            return remaining > 0 ? remaining / 1000 : 0;
        }
        return 0L;
    }
}