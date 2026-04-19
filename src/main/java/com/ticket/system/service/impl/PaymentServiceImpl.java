package com.ticket.system.service.impl;

import com.ticket.system.common.constant.OrderConstant;
import com.ticket.system.common.exception.BusinessException;
import com.ticket.system.common.exception.ErrorCode;
import com.ticket.system.common.util.SnowflakeIdUtil;
import com.ticket.system.dto.request.PaymentDTO;
import com.ticket.system.dto.response.PaymentResultDTO;
import com.ticket.system.dto.response.RefundCheckResult;
import com.ticket.system.dto.response.RefundRuleInfo;
import com.ticket.system.entity.Order;
import com.ticket.system.entity.Payment;
import com.ticket.system.entity.RefundRecord;
import com.ticket.system.mapper.OrderMapper;
import com.ticket.system.mapper.PaymentMapper;
import com.ticket.system.mapper.RefundRecordMapper;
import com.ticket.system.message.RefundMessage;
import com.ticket.system.mq.producer.RefundProducer;
import com.ticket.system.service.PaymentService;
import com.ticket.system.service.RefundRuleService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private SnowflakeIdUtil snowflakeIdUtil;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RefundProducer refundProducer;

    @Autowired
    private RefundRuleService refundRuleService;

    @Autowired
    private RefundRecordMapper refundRecordMapper;

    @Override
    @Transactional
    public PaymentResultDTO createPayment(PaymentDTO paymentDTO) {
        // 查询订单
        Order order = orderMapper.selectById(paymentDTO.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST.getCode(), "订单不存在");
        }

        // 检查订单状态
        if (order.getOrderStatus() != OrderConstant.ORDER_STATUS_PENDING) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "订单状态不允许支付");
        }

        // 检查支付金额
        if (paymentDTO.getPaymentAmount().compareTo(order.getTicketPrice()) != 0) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_ERROR.getCode(), "支付金额错误");
        }

        // 创建支付记录
        Payment payment = new Payment();
        BeanUtils.copyProperties(paymentDTO, payment);

        payment.setPaymentNumber(snowflakeIdUtil.generatePaymentNumber());
        payment.setPaymentStatus(0); // 支付中
        payment.setCreateTime(new Date());
        payment.setUpdateTime(new Date());

        int result = paymentMapper.insert(payment);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED.getCode(), "创建支付记录失败");
        }

        // 返回支付结果
        PaymentResultDTO paymentResultDTO = new PaymentResultDTO();
        BeanUtils.copyProperties(payment, paymentResultDTO);
        paymentResultDTO.setOrderNumber(order.getOrderNumber());
        paymentResultDTO.setOrderAmount(order.getTicketPrice());

        return paymentResultDTO;
    }

    @Override
    public PaymentResultDTO getPaymentByOrderId(Long orderId) {
        Payment payment = paymentMapper.selectByOrderId(orderId);
        if (payment == null) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED.getCode(), "支付记录不存在");
        }

        PaymentResultDTO paymentResultDTO = new PaymentResultDTO();
        BeanUtils.copyProperties(payment, paymentResultDTO);

        // 获取订单信息
        Order order = orderMapper.selectById(orderId);
        if (order != null) {
            paymentResultDTO.setOrderNumber(order.getOrderNumber());
            paymentResultDTO.setOrderAmount(order.getTicketPrice());
        }

        return paymentResultDTO;
    }

    @Override
    public PaymentResultDTO getPaymentByNumber(String paymentNumber) {
        Payment payment = paymentMapper.selectByPaymentNumber(paymentNumber);
        if (payment == null) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED.getCode(), "支付记录不存在");
        }

        PaymentResultDTO paymentResultDTO = new PaymentResultDTO();
        BeanUtils.copyProperties(payment, paymentResultDTO);

        // 获取订单信息
        Order order = orderMapper.selectById(payment.getOrderId());
        if (order != null) {
            paymentResultDTO.setOrderNumber(order.getOrderNumber());
            paymentResultDTO.setOrderAmount(order.getTicketPrice());
        }

        return paymentResultDTO;
    }

    @Override
    @Transactional
    public boolean processPaymentCallback(String paymentNumber, String status) {
        Payment payment = paymentMapper.selectByPaymentNumber(paymentNumber);
        if (payment == null) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED.getCode(), "支付记录不存在");
        }

        // 更新支付状态
        int paymentStatus = "SUCCESS".equals(status) ? 1 : 2;
        payment.setPaymentStatus(paymentStatus);
        payment.setPaymentTime(new Date());
        payment.setUpdateTime(new Date());

        int result = paymentMapper.update(payment);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED.getCode(), "更新支付状态失败");
        }

        // 更新订单状态
        if (paymentStatus == 1) {
            Order order = orderMapper.selectById(payment.getOrderId());
            if (order != null) {
                order.setOrderStatus(OrderConstant.ORDER_STATUS_PAID);
                order.setPayStatus(OrderConstant.PAY_STATUS_PAID);
                order.setPayTime(new Date());
                order.setUpdateTime(new Date());
                orderMapper.update(order);
            }
        }

        return true;
    }

    @Override
    @Transactional
    public boolean refundPayment(Long orderId) {

        String lockKey = "refund:order:" + orderId;
        RLock lock = redissonClient.getLock(lockKey);

        boolean locked = false;
        try {
            locked = lock.tryLock(0, 30, TimeUnit.SECONDS);
            if (!locked) {
                // 获取锁失败，说明有其他请求正在处理，退款消息会被延迟重试
                refundProducer.send(new RefundMessage(orderId, "orderId" + orderId,0));
                throw new BusinessException(ErrorCode.PAYMENT_FAILED.getCode(), "退款申请正在处理中，请稍后查询结果");
            }

            // 1. 查询订单
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                throw new BusinessException(ErrorCode.ORDER_NOT_EXIST.getCode(), "订单不存在");
            }

            // 1.5 检查订单是否已退款（幂等性检查）
            if (order.getOrderStatus() == 4) {
                log.warn("订单已退款，跳过处理: orderId={}", orderId);
                return true; // 幂等返回成功
            }

            // 2. 退款前检查（发车时间、订单状态等）
            RefundCheckResult checkResult = refundRuleService.checkRefundable(order);
            if (!checkResult.isRefundable()) {
                throw new BusinessException(ErrorCode.PAYMENT_FAILED.getCode(), checkResult.getMessage());
            }

            // 3. 计算退款金额
            RefundRuleInfo.RefundCalculation calculation = refundRuleService.calculateRefund(
                    order.getTicketPrice(), order.getDepartureTime());
            if (!calculation.isRefundable()) {
                throw new BusinessException(ErrorCode.PAYMENT_FAILED.getCode(), calculation.getMessage());
            }

            log.info("退票金额计算: orderId={}, 票价={}, 退款金额={}, 手续费={}, 实际退款={}, 规则={}",
                    orderId, calculation.getTicketPrice(), calculation.getRefundAmount(),
                    calculation.getServiceFee(), calculation.getActualRefundAmount(),
                    calculation.getMessage());

            // 4. 查询支付记录（可能不存在，如余额支付）
            Payment payment = paymentMapper.selectByOrderId(orderId);

            // 4.5 如果存在支付记录，进行支付状态和退款状态检查
            String paymentMethod = "BALANCE"; // 默认支付方式
            boolean hasPaymentRecord = (payment != null);
            if (hasPaymentRecord) {
                if (payment.getPaymentStatus() != 1) {
                    throw new BusinessException(ErrorCode.PAYMENT_FAILED.getCode(), "支付状态异常，无法退款");
                }
                // 检查是否已退款（幂等性检查）
                if (payment.getRefundStatus() != 0) {
                    log.warn("支付记录已处于退款状态，跳过处理: orderId={}, refundStatus={}", orderId, payment.getRefundStatus());
                    return true; // 幂等返回成功
                }
                paymentMethod = payment.getPaymentMethod();

                // 5. 原子标记退款中（防重复提交的关键）
                int updated = paymentMapper.lockRefund(orderId);
                log.info("lockRefund结果: orderId={}, updated={}", orderId, updated);
                if (updated == 0) {
                    // 说明有其他请求已经获取了退款锁，幂等返回
                    log.warn("其他请求正在处理退款，幂等跳过: orderId={}", orderId);
                    return true;
                }

                // 6. 更新为已退款（带条件检查）
                int successUpdated = paymentMapper.updateRefundSuccessWithLock(orderId);
                log.info("updateRefundSuccessWithLock结果: orderId={}, successUpdated={}", orderId, successUpdated);
            } else {
                // 如果没有 payment 记录（余额支付），检查订单支付状态
                if (order.getPayStatus() == null || order.getPayStatus() != 1) {
                    throw new BusinessException(ErrorCode.PAYMENT_FAILED.getCode(), "订单未支付，无法退款");
                }
                log.info("余额支付订单，无需 lockRefund: orderId={}", orderId);
            }

            // 无论是否有 payment 记录，都需要更新订单状态
            log.info("准备更新订单状态: orderId={}, 目标状态=4", orderId);
            orderMapper.updateOrderStatus(orderId, 4); // 4=已退款
            log.info("订单状态已更新: orderId={}", orderId);

            // 7. 创建退款记录
            RefundRecord refundRecord = new RefundRecord();
            refundRecord.setRefundNumber(snowflakeIdUtil.generateOrderNumber().replace("ORDER", "REFUND"));
            refundRecord.setOrderNumber(order.getOrderNumber());
            refundRecord.setUserId(order.getUserId());
            refundRecord.setRefundAmount(calculation.getRefundAmount());
            refundRecord.setActualRefundAmount(calculation.getActualRefundAmount());
            refundRecord.setServiceFee(calculation.getServiceFee());
            refundRecord.setStatus("SUCCESS");
            refundRecord.setPaymentMethod(paymentMethod);
            refundRecord.setRefundMethod("ORIGINAL");
            refundRecord.setApplyTime(new Date());
            refundRecord.setProcessTime(new Date());
            refundRecord.setCompleteTime(new Date());
            refundRecordMapper.insert(refundRecord);

            log.info("退款记录已创建: refundNumber={}, orderNumber={}, actualRefund={}",
                    refundRecord.getRefundNumber(), refundRecord.getOrderNumber(),
                    refundRecord.getActualRefundAmount());

            return true;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED.getCode(), e.getMessage());
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    @Override
    public RefundCheckResult refundCheck(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST.getCode(), "订单不存在");
        }
        return refundRuleService.checkRefundable(order);
    }


    @Override
    public void updatePaymentStatus(Long paymentId, Integer status) {
        Payment payment = paymentMapper.selectById(paymentId);
        if (payment == null) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED.getCode(), "支付记录不存在");
        }

        payment.setPaymentStatus(status);
        payment.setUpdateTime(new Date());

        paymentMapper.update(payment);
    }
}