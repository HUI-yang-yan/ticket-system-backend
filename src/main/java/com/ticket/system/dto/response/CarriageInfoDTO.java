package com.ticket.system.dto.response;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class CarriageInfoDTO {

    private Long id;
    private Long trainId;
    private String trainNumber;
    private Integer carriageIndex;
    private String carriageType;
    private String carriageTypeName;
    private Integer seatCount;
    private Integer rowCount;
    private Integer columnCount;
    private List<SeatInfoDTO> seats;
    private Date createTime;

    // 获取车厢类型中文名称
    public String getCarriageTypeName() {
        switch (carriageType) {
            case "BUSINESS": return "商务车厢";
            case "FIRST": return "一等座车厢";
            case "SECOND": return "二等座车厢";
            case "SLEEPER": return "卧铺车厢";
            case "DINING": return "餐车";
            case "EXECUTIVE": return "高级软卧车厢";
            default: return "其他车厢";
        }
    }
}