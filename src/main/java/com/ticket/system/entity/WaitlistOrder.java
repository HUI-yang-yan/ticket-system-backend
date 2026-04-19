package com.ticket.system.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 候补订单实体类
 * 用户提交的候补购票请求
 */
@Data
public class WaitlistOrder {

    /** 候补订单状态 - 待匹配 */
    public static final String STATUS_PENDING = "PENDING";

    /** 候补订单状态 - 已匹配成功，等待支付 */
    public static final String STATUS_MATCHED = "MATCHED";

    /** 候补订单状态 - 购票成功 */
    public static final String STATUS_SUCCESS = "SUCCESS";

    /** 候补订单状态 - 已过期 */
    public static final String STATUS_EXPIRED = "EXPIRED";

    /** 候补订单状态 - 已取消 */
    public static final String STATUS_CANCELLED = "CANCELLED";

    private Long id;

    /** 候补单号 */
    private String waitlistNumber;

    /** 用户ID */
    private Long userId;

    /** 车次ID */
    private Long trainId;

    /** 出发站ID */
    private Long startStationId;

    /** 到达站ID */
    private Long endStationId;

    /** 座位类型 */
    private String seatType;

    /** 期望乘车日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private LocalDate departureDate;

    /** 候补订单状态 */
    private String status;

    /** 候补订单创建时间 */
    private Date createTime;

    /** 候补订单过期时间（匹配后24小时未支付） */
    private Date expireTime;

    /** 更新时间 */
    private Date updateTime;

    /** 关联的正式订单号（匹配成功后生成） */
    private String orderNumber;
}
