package com.ticket.system.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class RefundTicket {
    private Long id;
    private String refundNumber;        // 退款单号
    private String ticketNumber;        // 车票号码
    private String orderNumber;         // 订单号
    private BigDecimal ticketPrice;     // 原票价
    private BigDecimal refundAmount;    // 退款金额
    private BigDecimal serviceFee;      // 手续费
    private String status;              // 状态
    private Date createTime;
}
