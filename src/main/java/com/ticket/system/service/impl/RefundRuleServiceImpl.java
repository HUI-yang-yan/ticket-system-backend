package com.ticket.system.service.impl;

import com.ticket.system.common.util.DateUtil;
import com.ticket.system.dto.response.RefundCheckResult;
import com.ticket.system.dto.response.RefundRuleInfo;
import com.ticket.system.entity.Order;
import com.ticket.system.entity.Train;
import com.ticket.system.mapper.TrainMapper;
import com.ticket.system.service.RefundRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 退票规则服务实现
 * 根据发车时间按照铁路退票规则计算退款金额和手续费
 */
@Slf4j
@Service
public class RefundRuleServiceImpl implements RefundRuleService {

    /**
     * 退票规则定义：
     * - 开车前48小时以上：全额退款（无手续费）
     * - 开车前24-48小时：退款80%
     * - 开车前6-24小时：退款50%
     * - 开车前不足6小时：退款20%
     */
    private static final int RULE_48_HOURS = 48;
    private static final int RULE_24_HOURS = 24;
    private static final int RULE_6_HOURS = 6;

    @Autowired
    private TrainMapper trainMapper;

    @Override
    public RefundRuleInfo getApplicableRule(LocalDateTime departureTime) {
        long hoursBeforeDeparture = Duration.between(LocalDateTime.now(), departureTime).toHours();

        if (hoursBeforeDeparture >= RULE_48_HOURS) {
            // 开车前48小时以上：全额退款
            return createRule("RULE_48+", "48小时以上全额退款", 48,
                    BigDecimal.ONE, "100%",
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        } else if (hoursBeforeDeparture >= RULE_24_HOURS) {
            // 开车前24-48小时：退款80%
            return createRule("RULE_24-48", "24-48小时退款80%", 24,
                    new BigDecimal("0.8"), "80%",
                    new BigDecimal("0.2"), new BigDecimal("0"), null);
        } else if (hoursBeforeDeparture >= RULE_6_HOURS) {
            // 开车前6-24小时：退款50%
            return createRule("RULE_6-24", "6-24小时退款50%", 6,
                    new BigDecimal("0.5"), "50%",
                    new BigDecimal("0.5"), new BigDecimal("0"), null);
        } else if (hoursBeforeDeparture > 0) {
            // 开车前不足6小时：退款20%
            return createRule("RULE_<6", "不足6小时退款20%", 0,
                    new BigDecimal("0.2"), "20%",
                    new BigDecimal("0.8"), new BigDecimal("0"), null);
        } else {
            // 已过发车时间：不可退款
            return createNonRefundableRule();
        }
    }

    @Override
    public RefundRuleInfo.RefundCalculation calculateRefund(BigDecimal ticketPrice, LocalDateTime departureTime) {
        RefundRuleInfo rule = getApplicableRule(departureTime);
        // 将 LocalDateTime 转换为 Date
        Date departureDate = DateUtil.convertToDate(departureTime);
        return rule.calculateRefund(ticketPrice, departureDate);
    }

    @Override
    public RefundCheckResult checkRefundable(Order order) {
        return checkRefundable(order, LocalDateTime.now());
    }

    @Override
    public RefundCheckResult checkRefundable(Order order, LocalDateTime checkTime) {
        if (order == null) {
            return RefundCheckResult.nonRefundable("订单不存在");
        }

        // 检查订单状态
        if (order.getOrderStatus() != 1) { // 1=已支付
            return RefundCheckResult.nonRefundable("订单状态不允许退票，当前状态：" + getOrderStatusText(order.getOrderStatus()));
        }

        // 检查发车时间
        LocalDateTime departureTime = order.getDepartureTime();
        if (departureTime == null) {
            return RefundCheckResult.nonRefundable("发车时间未知");
        }

        // 已过发车时间，不可退
        if (departureTime.isBefore(checkTime)) {
            return RefundCheckResult.nonRefundable("已过发车时间，不支持退票");
        }

        // 计算时间差
        long hoursRemaining = Duration.between(checkTime, departureTime).toHours();

        // 如果超过48小时但订单已支付超过48小时，提示可能已过退款时间
        if (hoursRemaining < 0) {
            return RefundCheckResult.nonRefundable("已超过可退票时间");
        }

        // 获取适用规则并计算
        RefundRuleInfo rule = getApplicableRule(departureTime);
        RefundRuleInfo.RefundCalculation calculation = rule.calculateRefund(
                order.getTicketPrice(), DateUtil.convertToDate(departureTime));

        if (!calculation.isRefundable()) {
            return RefundCheckResult.nonRefundable(calculation.getMessage());
        }

        // 查询车次信息
        String trainNumber = null;
        try {
            Train train = trainMapper.selectById(order.getTrainId());
            if (train != null) {
                trainNumber = train.getTrainNumber();
            }
        } catch (Exception e) {
            log.warn("查询车次信息失败: orderId={}, trainId={}", order.getId(), order.getTrainId(), e);
        }

        // 构建可退结果
        List<RefundCheckResult.RefundableTicket> tickets = new ArrayList<>();
        RefundCheckResult.RefundableTicket ticket = new RefundCheckResult.RefundableTicket();
        ticket.setTicketNumber(order.getOrderNumber());
        ticket.setPassengerName(order.getPassengerRealName());
        ticket.setTrainNumber(trainNumber);
        ticket.setDepartureTime(DateUtil.convertToDate(departureTime));
        ticket.setSeatInfo(order.getSeatType());
        ticket.setTicketPrice(order.getTicketPrice());
        ticket.setRefundAmount(calculation.getRefundAmount());
        ticket.setServiceFee(calculation.getServiceFee());
        ticket.setActualRefund(calculation.getActualRefundAmount());
        ticket.setRefundRate(rule.getRefundRateDisplay());
        ticket.setRefundRule(rule.getRuleName());
        tickets.add(ticket);

        // 计算剩余秒数
        long remainingSeconds = Duration.between(checkTime, departureTime).toSeconds();

        // 构建完整的退款检查结果
        RefundCheckResult result = RefundCheckResult.refundable(
                calculation.getMessage(),
                tickets,
                calculation.getActualRefundAmount(),
                rule
        );
        result.setOrderNumber(order.getOrderNumber());
        result.setOrderStatus(getOrderStatusText(order.getOrderStatus()));
        result.setOrderCreateTime(order.getCreateTime());
        result.setRemainingSeconds(remainingSeconds);
        result.setLatestRefundTime(DateUtil.convertToDate(departureTime));

        return result;
    }

    private RefundRuleInfo createRule(String ruleId, String ruleName, int hoursBeforeDeparture,
                                       BigDecimal refundRate, String refundRateDisplay,
                                       BigDecimal serviceFeeRate, BigDecimal minFee, BigDecimal maxFee) {
        RefundRuleInfo rule = new RefundRuleInfo();
        rule.setRuleId(ruleId);
        rule.setRuleName(ruleName);
        rule.setRuleDescription(ruleName);
        rule.setHoursBeforeDeparture(hoursBeforeDeparture);
        rule.setRefundRate(refundRate);
        rule.setRefundRateDisplay(refundRateDisplay);
        rule.setServiceFeeRate(serviceFeeRate);
        rule.setMinServiceFee(minFee);
        rule.setMaxServiceFee(maxFee);
        rule.setAllowPartialRefund(true);
        rule.setAllowOnlineRefund(true);
        return rule;
    }

    private RefundRuleInfo createNonRefundableRule() {
        RefundRuleInfo rule = new RefundRuleInfo();
        rule.setRuleId("NON_REFUNDABLE");
        rule.setRuleName("不可退票");
        rule.setRuleDescription("已过发车时间或不符合退票条件");
        return rule;
    }

    private String getOrderStatusText(Integer orderStatus) {
        if (orderStatus == null) return "未知";
        switch (orderStatus) {
            case 0: return "待支付";
            case 1: return "已支付";
            case 2: return "已取消";
            case 3: return "已完成";
            case 4: return "已退款";
            default: return "未知(" + orderStatus + ")";
        }
    }
}
