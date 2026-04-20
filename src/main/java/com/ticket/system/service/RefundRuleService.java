package com.ticket.system.service;

import com.ticket.system.dto.response.RefundCheckResult;
import com.ticket.system.dto.response.RefundRuleInfo;
import com.ticket.system.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退票规则服务
 * 根据发车时间计算退款金额和手续费
 */
public interface RefundRuleService {

    /**
     * 根据发车时间获取适用的退票规则
     * @param departureTime 发车时间
     * @return 适用的退票规则
     */
    RefundRuleInfo getApplicableRule(LocalDateTime departureTime);

    /**
     * 计算退款金额
     * @param ticketPrice 票价
     * @param departureTime 发车时间
     * @return 退款计算结果
     */
    RefundRuleInfo.RefundCalculation calculateRefund(BigDecimal ticketPrice, LocalDateTime departureTime);

    /**
     * 检查订单是否可退
     * @param order 订单
     * @return 检查结果
     */
    RefundCheckResult checkRefundable(Order order);

    /**
     * 检查订单是否可退（指定时间）
     * @param order 订单
     * @param checkTime 检查时间
     * @return 检查结果
     */
    RefundCheckResult checkRefundable(Order order, LocalDateTime checkTime);
}
