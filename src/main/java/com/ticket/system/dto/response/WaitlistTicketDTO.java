package com.ticket.system.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Data
public class WaitlistTicketDTO {

    private Long id;

    /** 车票号码 */
    private String ticketNumber;

    /** 原订单号 */
    private String originalOrderNumber;

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

    /** 乘车日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private LocalDate departureDate;

    /** 座位类型 */
    private String seatType;

    /** 座位类型中文名称 */
    private String seatTypeName;

    /** 车厢号 */
    private String carriageNumber;

    /** 座位号 */
    private String seatNumber;

    /** 票价 */
    private BigDecimal price;

    /** 票价文本 */
    private String priceText;

    /** 候补票状态 */
    private String status;

    /** 状态中文描述 */
    private String statusText;

    /** 来源 */
    private String source;

    /** 创建时间 */
    private Date createTime;

    /** 候补票状态文本 */
    public String getStatusText() {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case "AVAILABLE": return "可候补";
            case "CLAIMED": return "已被候补购得";
            case "EXPIRED": return "已过期";
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

    /** 格式化价格 */
    public String getPriceText() {
        if (price == null) {
            return "0.00";
        }
        return "¥" + price.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
    }
}
