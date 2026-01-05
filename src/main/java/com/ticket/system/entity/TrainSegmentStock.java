package com.ticket.system.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 车次区间库存表
 */
@Data
public class TrainSegmentStock {

    /**
     * 主键
     */
    private Long id;

    /**
     * 车次ID
     */
    private Long trainId;

    /**
     * 区间起始站
     */
    private Long startStationId;

    /**
     * 乘车日期
     */
    private LocalDate travelDate;

    /**
     * 座席类型（二等座 / 一等座 / 无座）
     */
    private String seatType;

    /**
     * 剩余库存
     */
    private Integer stock;

    private BigDecimal price;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
