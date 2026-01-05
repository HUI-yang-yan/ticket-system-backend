package com.ticket.system.entity;

import lombok.Data;

import java.time.LocalTime;
import java.util.Date;

@Data
public class Train {
    private Long id;
    private String trainNumber;
    private String trainType;
    private Long startStationId;
    private Long endStationId;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer duration;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}