package com.ticket.system.dto.response;

import com.ticket.system.common.util.DateUtil;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class RefundCheckResult {

    private boolean refundable;             // 是否可退
    private String message;                 // 检查消息
    private String checkId;                 // 检查ID

    // 订单信息
    private String orderNumber;
    private String orderStatus;
    private Date orderCreateTime;

    // 车票列表
    private List<RefundableTicket> refundableTickets;
    private List<NonRefundableTicket> nonRefundableTickets;

    // 退款计算
    private BigDecimal totalTicketPrice;    // 总票价
    private BigDecimal totalRefundAmount;   // 总应退金额
    private BigDecimal totalServiceFee;     // 总手续费
    private BigDecimal totalActualRefund;   // 总实际退款
    private String refundRate;              // 整体退款比例

    // 退款规则
    private RefundRuleInfo applicableRule;

    // 时间信息
    private Date latestRefundTime;          // 最晚退款时间
    private Long remainingSeconds;          // 剩余退款时间（秒）

    // 限制信息
    private List<String> restrictions;      // 限制条件
    private List<String> suggestions;       // 建议

    /**
     * 可退结果
     */
    public static RefundCheckResult refundable(String message, List<RefundableTicket> tickets,
                                               BigDecimal totalActualRefund, RefundRuleInfo rule) {
        RefundCheckResult result = new RefundCheckResult();
        result.setRefundable(true);
        result.setMessage(message);
        result.setRefundableTickets(tickets);
        result.setTotalActualRefund(totalActualRefund);
        result.setApplicableRule(rule);

        // 计算总金额
        if (tickets != null && !tickets.isEmpty()) {
            BigDecimal totalPrice = BigDecimal.ZERO;
            BigDecimal totalRefund = BigDecimal.ZERO;
            BigDecimal totalFee = BigDecimal.ZERO;

            for (RefundableTicket ticket : tickets) {
                totalPrice = totalPrice.add(ticket.getTicketPrice());
                totalRefund = totalRefund.add(ticket.getRefundAmount());
                totalFee = totalFee.add(ticket.getServiceFee());
            }

            result.setTotalTicketPrice(totalPrice);
            result.setTotalRefundAmount(totalRefund);
            result.setTotalServiceFee(totalFee);

            if (totalPrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal rate = totalRefund.divide(totalPrice, 4, BigDecimal.ROUND_HALF_UP);
                result.setRefundRate(rate.multiply(new BigDecimal(100)).setScale(1) + "%");
            }
        }

        return result;
    }

    /**
     * 不可退结果
     */
    public static RefundCheckResult nonRefundable(String message) {
        RefundCheckResult result = new RefundCheckResult();
        result.setRefundable(false);
        result.setMessage(message);
        return result;
    }

    /**
     * 部分可退结果
     */
    public static RefundCheckResult partiallyRefundable(String message,
                                                        List<RefundableTicket> refundableTickets,
                                                        List<NonRefundableTicket> nonRefundableTickets,
                                                        BigDecimal totalActualRefund) {
        RefundCheckResult result = new RefundCheckResult();
        result.setRefundable(true);
        result.setMessage(message);
        result.setRefundableTickets(refundableTickets);
        result.setNonRefundableTickets(nonRefundableTickets);
        result.setTotalActualRefund(totalActualRefund);
        return result;
    }

    @Data
    public static class RefundableTicket {
        private String ticketNumber;
        private String passengerName;
        private String trainNumber;
        private Date departureTime;
        private String seatInfo;
        private BigDecimal ticketPrice;
        private BigDecimal refundAmount;
        private BigDecimal serviceFee;
        private BigDecimal actualRefund;
        private String refundRate;
        private String refundRule;

        public String getDisplayInfo() {
            return String.format("%s %s %s %s",
                    passengerName, trainNumber,
                    DateUtil.formatDate(departureTime, "MM-dd HH:mm"),
                    seatInfo);
        }
    }

    @Data
    public static class NonRefundableTicket {
        private String ticketNumber;
        private String passengerName;
        private String trainNumber;
        private Date departureTime;
        private String seatInfo;
        private String reason;
        private String suggestion;

        public String getDisplayInfo() {
            return String.format("%s %s %s",
                    passengerName, trainNumber,
                    DateUtil.formatDate(departureTime, "MM-dd HH:mm"));
        }
    }

    // ==================== 业务方法 ====================

    /**
     * 获取可退票数量
     */
    public int getRefundableCount() {
        return refundableTickets != null ? refundableTickets.size() : 0;
    }

    /**
     * 获取不可退票数量
     */
    public int getNonRefundableCount() {
        return nonRefundableTickets != null ? nonRefundableTickets.size() : 0;
    }

    /**
     * 获取总票数
     */
    public int getTotalTicketCount() {
        return getRefundableCount() + getNonRefundableCount();
    }

    /**
     * 检查是否部分退款
     */
    public boolean isPartialRefund() {
        return getRefundableCount() > 0 && getNonRefundableCount() > 0;
    }

    /**
     * 获取退款金额显示
     */
    public String getTotalRefundDisplay() {
        if (totalActualRefund != null) {
            return "¥" + totalActualRefund.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        return "";
    }

    /**
     * 获取手续费显示
     */
    public String getTotalFeeDisplay() {
        if (totalServiceFee != null && totalServiceFee.compareTo(BigDecimal.ZERO) > 0) {
            return "¥" + totalServiceFee.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        return "¥0.00";
    }
}