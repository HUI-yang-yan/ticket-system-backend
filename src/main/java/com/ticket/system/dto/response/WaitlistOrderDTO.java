package com.ticket.system.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Data
public class WaitlistOrderDTO {

    private Long id;

    /** 候补单号 */
    private String waitlistNumber;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 车次ID */
    private Long trainId;

    /** 车次号 */
    private String trainNumber;

    /** 出发站ID */
    private Long startStationId;

    /** 出发站名称 */
    private String startStationName;

    /** 到达站ID */
    private Long endStationId;

    /** 到达站名称 */
    private String endStationName;

    /** 座位类型 */
    private String seatType;

    /** 座位类型中文名称 */
    private String seatTypeName;

    /** 期望乘车日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private LocalDate departureDate;

    /** 候补订单状态 */
    private String status;

    /** 状态中文描述 */
    private String statusText;

    /** 候补订单创建时间 */
    private Date createTime;

    /** 候补订单过期时间 */
    private Date expireTime;

    /** 关联的正式订单号 */
    private String orderNumber;

    /** 候补订单状态文本 */
    public String getStatusText() {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case "PENDING": return "待匹配";
            case "MATCHED": return "已匹配，待支付";
            case "SUCCESS": return "购票成功";
            case "EXPIRED": return "已过期";
            case "CANCELLED": return "已取消";
            default: return "未知";
        }
    }

    /** 座位类型中文名称 */
    public String getSeatTypeName() {
        if (seatType == null) {
            return "其他";
        }
        switch (seatType) {
            case "BUSINESS": return "商务座";
            case "FIRST": return "一等座";
            case "SECOND": return "二等座";
            case "SOFT_SLEEPER": return "软卧";
            case "HARD_SLEEPER": return "硬卧";
            case "SOFT_SEAT": return "软座";
            case "HARD_SEAT": return "硬座";
            default: return "其他";
        }
    }
}
