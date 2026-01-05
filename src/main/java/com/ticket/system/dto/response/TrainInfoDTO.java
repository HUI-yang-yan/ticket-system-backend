package com.ticket.system.dto.response;

import com.ticket.system.dto.request.TrainStationDTO;
import lombok.Data;

import java.time.LocalTime;
import java.util.Date;
import java.util.List;

@Data
public class TrainInfoDTO {

    private Long id;
    private String trainNumber;
    private String trainType;
    private Long startStationId;
    private Long endStationId;
    private String startStationName;  // 关联查询的始发站名称
    private String endStationName;    // 关联查询的终点站名称
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer duration;          // 运行时长（分钟）
    private String durationText;       // 格式化后的运行时长
    private Integer status;
    private List<TrainStationDTO> stationList;
    private Date createTime;
    private Date updateTime;
}