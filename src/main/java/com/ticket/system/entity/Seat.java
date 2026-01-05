package com.ticket.system.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Seat {
    private Long id;
    private Long carriageId;
    private String seatNumber;
    private String seatType;
    private Integer rowNum;
    private Integer colNum;
    private Integer status;
    private Date createTime;
}