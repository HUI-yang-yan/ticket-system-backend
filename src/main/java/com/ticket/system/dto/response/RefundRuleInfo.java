package com.ticket.system.dto.response;

import com.ticket.system.common.util.DateUtil;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class RefundRuleInfo {

    private String ruleId;                  // 规则ID
    private String ruleName;                // 规则名称
    private String ruleDescription;         // 规则描述

    // 时间规则
    private Integer hoursBeforeDeparture;   // 发车前小时数
    private String timeRangeDescription;    // 时间范围描述

    // 退款比例
    private BigDecimal refundRate;          // 退款比例（0-1）
    private String refundRateDisplay;       // 退款比例显示（如"80%"）

    // 手续费规则
    private BigDecimal serviceFeeRate;      // 手续费率
    private BigDecimal minServiceFee;       // 最低手续费
    private BigDecimal maxServiceFee;       // 最高手续费
    private BigDecimal fixedServiceFee;     // 固定手续费

    // 适用条件
    private List<String> applicableTrainTypes;  // 适用列车类型
    private List<String> applicableSeatTypes;   // 适用座位类型
    private boolean applicableToStudent;        // 是否适用学生票
    private boolean applicableToGroup;          // 是否适用团体票

    // 特殊规则
    private boolean allowPartialRefund;         // 是否允许部分退票
    private boolean allowOnlineRefund;          // 是否允许在线退票
    private boolean requireVerification;        // 是否需要核验

    // 生效时间
    private Date effectiveDate;                 // 生效日期
    private Date expireDate;                    // 失效日期

    // 计算退款金额
    public RefundCalculation calculateRefund(BigDecimal ticketPrice, Date departureTime) {
        RefundCalculation calculation = new RefundCalculation();
        calculation.setTicketPrice(ticketPrice);

        // 计算时间差
        long hours = DateUtil.hoursBetween(new Date(), departureTime);

        // 根据规则计算
        if (hours >= hoursBeforeDeparture) {
            // 符合退款条件
            calculation.setRefundable(true);

            // 计算退款金额
            BigDecimal refundAmount = ticketPrice.multiply(refundRate);
            calculation.setRefundAmount(refundAmount);

            // 计算手续费
            BigDecimal fee = calculateServiceFee(ticketPrice);
            calculation.setServiceFee(fee);

            // 实际退款金额
            BigDecimal actualRefund = refundAmount.subtract(fee);
            calculation.setActualRefundAmount(actualRefund.compareTo(BigDecimal.ZERO) > 0 ?
                    actualRefund : BigDecimal.ZERO);

            calculation.setMessage(String.format("发车前%d小时以上退票，退款比例%s，手续费%s",
                    hoursBeforeDeparture, refundRateDisplay, fee));
        } else {
            calculation.setRefundable(false);
            calculation.setMessage("已超过可退票时间");
        }

        return calculation;
    }

    // 计算手续费
    private BigDecimal calculateServiceFee(BigDecimal ticketPrice) {
        BigDecimal fee = BigDecimal.ZERO;

        if (fixedServiceFee != null) {
            fee = fixedServiceFee;
        } else if (serviceFeeRate != null) {
            fee = ticketPrice.multiply(serviceFeeRate);
        }

        // 检查最低最高限制
        if (minServiceFee != null && fee.compareTo(minServiceFee) < 0) {
            fee = minServiceFee;
        }
        if (maxServiceFee != null && fee.compareTo(maxServiceFee) > 0) {
            fee = maxServiceFee;
        }

        return fee;
    }

    // 获取适用条件描述
    public String getApplicableCondition() {
        StringBuilder sb = new StringBuilder();

        if (applicableTrainTypes != null && !applicableTrainTypes.isEmpty()) {
            sb.append("适用车次：").append(String.join("、", applicableTrainTypes));
        }

        if (applicableSeatTypes != null && !applicableSeatTypes.isEmpty()) {
            if (sb.length() > 0) sb.append("；");
            sb.append("适用席位：").append(String.join("、", applicableSeatTypes));
        }

        if (hoursBeforeDeparture != null) {
            if (sb.length() > 0) sb.append("；");
            sb.append("发车前").append(hoursBeforeDeparture).append("小时以上");
        }

        return sb.toString();
    }

    @Data
    public static class RefundCalculation {
        private boolean refundable;
        private BigDecimal ticketPrice;
        private BigDecimal refundAmount;
        private BigDecimal serviceFee;
        private BigDecimal actualRefundAmount;
        private String message;
        private String ruleName;
    }
}