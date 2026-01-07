package com.ticket.system.entity;

import lombok.Data;
import java.util.Date;
import java.math.BigDecimal;

@Data
public class Payment {
    private Long id;
    private Long orderId;
    private String paymentNumber;
    private BigDecimal paymentAmount;
    private String paymentMethod;
    private Integer paymentStatus;
    private Date paymentTime;
    private Integer refundStatus;
    private Date createTime;
    private Date updateTime;
}