package com.ticket.system.dto.request;

import lombok.Data;

import java.time.LocalTime;

@Data
public class TrainStationDTO {

    private Long stationId;
    private String stationName;   // 可选，前端展示用

    private Integer stationOrder;

    private LocalTime arrivalTime;    // 始发站为 null
    private LocalTime departureTime;  // 终点站为 null

    private Integer stopMinutes;

    private Boolean isSale;           // 是否可上下车
}
