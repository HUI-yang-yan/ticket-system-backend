package com.ticket.system.entity;

import lombok.Data;

import java.time.LocalTime;
import java.util.Date;
import java.sql.Time;

@Data
public class TrainStation {
    private Long id;
    private Long trainId;
    private Long stationId;
    private String stationName;
    private Integer stationIndex;
    private LocalTime arrivalTime;
    private LocalTime departureTime;
    private Integer stopDuration;
    private Integer distance;
    private Date createTime;
}