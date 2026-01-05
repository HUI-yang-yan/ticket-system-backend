package com.ticket.system.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class TicketInventory {
    private Long id;
    private Long trainId;
    private String seatType;
    private Integer totalCount;
    private Integer version;
    private Date createTime;
    private Date updateTime;
    private BigDecimal price;
    private Integer status;
}