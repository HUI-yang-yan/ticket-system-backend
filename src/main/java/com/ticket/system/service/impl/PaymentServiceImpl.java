package com.ticket.system.service.impl;

import com.ticket.system.common.constant.OrderConstant;
import com.ticket.system.common.exception.BusinessException;
import com.ticket.system.common.exception.ErrorCode;
import com.ticket.system.common.util.SnowflakeIdUtil;
import com.ticket.system.dto.request.PaymentDTO;
import com.ticket.system.dto.response.PaymentResultDTO;
import com.ticket.system.entity.Order;
import com.ticket.system.entity.Payment;
import com.ticket.system.mapper.OrderMapper;
import com.ticket.system.mapper.PaymentMapper;
import com.ticket.system.message.RefundMessage;
import com.ticket.system.mq.producer.RefundProducer;
import com.ticket.system.service.PaymentService;
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
                refundProducer.send(new RefundMessage(orderId, "orderId" + orderId,0));
                return true;
            }

            Payment payment = paymentMapper.selectByOrderId(orderId);
            if (payment == null || payment.getPaymentStatus() != 1) {
                return true;
            }

            int updated = paymentMapper.lockRefund(orderId);
            if (updated == 0) {
                return true;
            }

            paymentMapper.updateRefundSuccess(orderId);
            orderMapper.updateOrderStatus(orderId,4);

            return true;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED.getCode(), e.getMessage());
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
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