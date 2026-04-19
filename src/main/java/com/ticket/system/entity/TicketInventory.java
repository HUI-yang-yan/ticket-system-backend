package com.ticket.system.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TicketInventory {
    private Long id;
    private Long trainId;
    private String seatType;
    private Integer totalCount;
    private Integer availableCount;
    private BigDecimal price;
    private Integer version;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}