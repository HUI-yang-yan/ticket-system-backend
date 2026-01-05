package com.ticket.system.dto.response;

import com.ticket.system.common.util.DateUtil;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class RefundTicketInfo {

    private String ticketNumber;           // 车票号码
    private String orderNumber;            // 订单号
    private String trainNumber;            // 车次号
    private String departureStationName;   // 出发站
    private String arrivalStationName;     // 到达站
    private Date departureTime;            // 出发时间
    private Date arrivalTime;              // 到达时间
    private String seatType;               // 座位类型
    private String seatNumber;             // 座位号
    private String carriageNumber;         // 车厢号

    // 乘客信息
    private String passengerName;
    private String passengerIdCard;

    // 票务信息
    private BigDecimal ticketPrice;        // 原票价
    private BigDecimal refundAmount;       // 应退款金额
    private BigDecimal actualRefundAmount; // 实际退款金额
    private BigDecimal serviceFee;         // 手续费
    private String refundRate;             // 退款比例
    private String refundRule;             // 适用规则

    // 状态信息
    private String ticketStatus;           // 车票状态
    private String refundStatus;           // 退款状态
    private Date refundApplyTime;          // 退款申请时间
    private Date refundCompleteTime;       // 退款完成时间

    // 格式化显示信息
    public String getRouteDisplay() {
        return departureStationName + " → " + arrivalStationName;
    }

    public String getTimeDisplay() {
        if (departureTime != null) {
            return DateUtil.formatDate(departureTime, "MM-dd HH:mm");
        }
        return "";
    }

    public String getSeatDisplay() {
        StringBuilder sb = new StringBuilder();
        if (carriageNumber != null) {
            sb.append(carriageNumber).append("车");
        }
        if (seatNumber != null) {
            sb.append(seatNumber).append("号");
        }
        return sb.toString().isEmpty() ? "" : sb.toString();
    }

    public String getPriceDisplay() {
        if (ticketPrice != null) {
            return "¥" + ticketPrice.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        return "";
    }

    public String getRefundAmountDisplay() {
        if (actualRefundAmount != null) {
            return "¥" + actualRefundAmount.setScale(2, BigDecimal.ROUND_HALF_UP);
        } else if (refundAmount != null) {
            return "¥" + refundAmount.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        return "";
    }

    public String getServiceFeeDisplay() {
        if (serviceFee != null && serviceFee.compareTo(BigDecimal.ZERO) > 0) {
            return "¥" + serviceFee.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        return "¥0.00";
    }

    // 检查是否可退
    public boolean isRefundable() {
        return "REFUNDABLE".equals(ticketStatus) ||
                "PAID".equals(ticketStatus) ||
                "CHANGED".equals(ticketStatus);
    }

    // 检查是否已退款
    public boolean isRefunded() {
        return "REFUNDED".equals(refundStatus);
    }
}