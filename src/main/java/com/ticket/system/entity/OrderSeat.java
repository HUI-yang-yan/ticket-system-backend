package com.ticket.system.entity;

import lombok.Data;
import java.util.Date;
import java.math.BigDecimal;

@Data
public class OrderSeat {
    private Long id;
    private Long orderId;
    private Long seatId;
    private Integer carriageIndex;
    private String seatNumber;
    private BigDecimal ticketPrice;
    private Date createTime;
}