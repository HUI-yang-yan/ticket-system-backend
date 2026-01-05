package com.ticket.system.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Carriage {
    private Long id;
    private Long trainId;
    private Integer carriageIndex;
    private String carriageType;
    private Integer seatCount;
    private Integer rowCount;
    private Integer columnCount;
    private Date createTime;
}