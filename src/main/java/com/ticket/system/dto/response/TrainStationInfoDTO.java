package com.ticket.system.dto.response;

import lombok.Data;
import java.sql.Time;
import java.util.Date;

@Data
public class TrainStationInfoDTO {

    private Long id;
    private Long trainId;
    private String trainNumber;
    private Long stationId;
    private String stationName;
    private String stationCode;
    private Integer stationIndex;
    private Time arrivalTime;
    private Time departureTime;
    private Integer stopDuration;
    private Integer distance;
    private Date createTime;

    // 获取停留时间文本
    public String getStopDurationText() {
        if (stopDuration == null || stopDuration == 0) {
            return "----";
        } else if (stopDuration < 60) {
            return stopDuration + "分钟";
        } else {
            int hours = stopDuration / 60;
            int minutes = stopDuration % 60;
            return hours + "小时" + (minutes > 0 ? minutes + "分钟" : "");
        }
    }

    // 获取距离文本
    public String getDistanceText() {
        if (distance == null) {
            return "0km";
        }
        return distance + "km";
    }

    // 检查是否是始发站
    public boolean isStartStation() {
        return arrivalTime == null && departureTime != null;
    }

    // 检查是否是终点站
    public boolean isEndStation() {
        return arrivalTime != null && departureTime == null;
    }

    // 检查是否是途经站
    public boolean isMiddleStation() {
        return arrivalTime != null && departureTime != null;
    }
}