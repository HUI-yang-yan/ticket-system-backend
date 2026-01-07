package com.ticket.system.mq.consumer;

import com.ticket.system.common.exception.BusinessException;
import com.ticket.system.common.exception.ErrorCode;
import com.ticket.system.entity.Payment;
import com.ticket.system.mapper.OrderMapper;
import com.ticket.system.mapper.PaymentMapper;
import com.ticket.system.message.RefundMessage;
import com.ticket.system.mq.producer.RefundProducer;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RefundConsumer {

    @Autowired
    private PaymentMapper paymentMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RefundProducer refundProducer;

    @RabbitListener(queues = "refund.queue")
    public void handleRefund(RefundMessage msg) {

        Long orderId = msg.getOrderId();
        String lockKey = "refund:order:" + orderId;
        RLock lock = redissonClient.getLock(lockKey);

        boolean locked = false;
        try {
            locked = lock.tryLock(0, 30, TimeUnit.SECONDS);
            if (!locked) {
                extracted(msg);
                return;
            }

            Payment payment = paymentMapper.selectByOrderId(orderId);
            if (payment == null || payment.getPaymentStatus() != 1) {
                return;
            }

            int updated = paymentMapper.lockRefund(orderId);
            if (updated == 0) {
                return;
            }

            paymentMapper.updateRefundSuccess(orderId);
            orderMapper.cancelOrder(orderId);

        } catch (Exception e) {
            log.error("退款消费异常 orderId={}", orderId, e);
            extracted(msg);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    private void extracted(RefundMessage msg) {
        try {
            refundProducer.sendDelay(msg.nextRetry());
        } catch (BusinessException businessException) {
            log.info("需要人工处理: {}",businessException.getMessage());
        }
    }
}

