package com.ticket.system.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TrainQueryDTO {

    private String trainNumber;     // 车次号
    private Long startStationId;    // 始发站ID
    private Long endStationId;      // 终点站ID
    private String trainType;       // 列车类型
    private LocalDate departureDate; // 出发日期
    private Integer pageNum = 1;    // 页码
    private Integer pageSize = 20;  // 每页条数
}