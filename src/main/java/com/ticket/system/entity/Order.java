package com.ticket.system.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.math.BigDecimal;

@Data
public class Order {
    private Long id;
    private String orderNumber;
    private Long userId;
    private Long trainId;
    private Long departureStationId;
    private Long arrivalStationId;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private LocalDateTime departureDate;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private String seatType;
    private String passengerIdCard;
    private String passengerRealName;
    private BigDecimal ticketPrice;
    private Integer orderStatus;
    private Integer payStatus;
    private Date payTime;
    private Date expireTime;
    private Date createTime;
    private Date updateTime;
}