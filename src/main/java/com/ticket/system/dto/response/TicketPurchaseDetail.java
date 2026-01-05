package com.ticket.system.dto.response;

import com.ticket.system.common.util.DateUtil;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Date;

@Data
public class TicketPurchaseDetail {

    private String trainNumber;           // 车次号
    private Long trainId;                 // 列车ID
    private String trainType;             // 列车类型
    private String trainTypeName;         // 列车类型名称

    private Long departureStationId;      // 出发站ID
    private String departureStationName;  // 出发站名称
    private String departureStationCode;  // 出发站代码

    private Long arrivalStationId;        // 到达站ID
    private String arrivalStationName;    // 到达站名称
    private String arrivalStationCode;    // 到达站代码

    private Date departureDate;           // 出发日期
    private LocalTime departureTime;           // 出发时间
    private LocalTime arrivalTime;             // 到达时间
    private String duration;              // 运行时长

    private String seatType;              // 座位类型
    private String seatTypeName;          // 座位类型名称
    private String carriageNumber;        // 车厢号
    private String seatNumber;            // 座位号

    private String passengerRealName;     // 乘客姓名
    private String passengerIdCard;       // 乘客身份证
    private String passengerPhone;        // 乘客手机号

    private BigDecimal ticketPrice;       // 票价
    private BigDecimal insurancePrice;    // 保险费（可选）
    private BigDecimal serviceFee;        // 服务费（可选）
    private BigDecimal totalAmount;       // 总金额

    private String ticketStatus;          // 车票状态
    private String ticketNumber;          // 车票号码

    // 格式化显示信息
    public String getRouteDisplay() {
        return departureStationName + " → " + arrivalStationName;
    }

    public String getTimeDisplay() {
        if (departureTime != null && arrivalTime != null) {
            return String.format("%s - %s",
                    (departureTime),
                    (arrivalTime));
        }
        return "";
    }

    public String getDateDisplay() {
        if (departureDate != null) {
            return DateUtil.formatDate(departureDate, "MM月dd日");
        }
        return "";
    }

    public String getSeatDisplay() {
        StringBuilder sb = new StringBuilder();
        if (seatTypeName != null) {
            sb.append(seatTypeName);
        }
        if (carriageNumber != null) {
            sb.append(" ").append(carriageNumber).append("车");
        }
        if (seatNumber != null) {
            sb.append(seatNumber).append("号");
        }
        return sb.toString();
    }

    public String getPriceDisplay() {
        if (totalAmount != null) {
            return "¥" + totalAmount.setScale(2, BigDecimal.ROUND_HALF_UP);
        } else if (ticketPrice != null) {
            return "¥" + ticketPrice.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        return "";
    }
}