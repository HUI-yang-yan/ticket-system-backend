package com.ticket.system.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 * 候补票池实体类
 * 从退票产生的可候补车票
 */
@Data
public class WaitlistTicket {

    /** 候补票状态 - 可候补 */
    public static final String STATUS_AVAILABLE = "AVAILABLE";

    /** 候补票状态 - 已被候补购得 */
    public static final String STATUS_CLAIMED = "CLAIMED";

    /** 候补票状态 - 已过期 */
    public static final String STATUS_EXPIRED = "EXPIRED";

    /** 来源 - 退票 */
    public static final String SOURCE_REFUND = "REFUND";

    private Long id;

    /** 车票号码 */
    private String ticketNumber;

    /** 原订单号 */
    private String originalOrderNumber;

    /** 车次ID */
    private Long trainId;

    /** 出发站ID */
    private Long startStationId;

    /** 到达站ID */
    private Long endStationId;

    /** 乘车日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private LocalDate departureDate;

    /** 座位类型 */
    private String seatType;

    /** 车厢号 */
    private String carriageNumber;

    /** 座位号 */
    private String seatNumber;

    /** 票价 */
    private BigDecimal price;

    /** 候补票状态 */
    private String status;

    /** 来源 */
    private String source;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}
