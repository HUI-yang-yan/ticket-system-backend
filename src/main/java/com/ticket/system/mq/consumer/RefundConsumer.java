package com.ticket.system.mq.consumer;

import com.ticket.system.common.exception.BusinessException;
import com.ticket.system.common.exception.ErrorCode;
import com.ticket.system.entity.Order;
import com.ticket.system.entity.Payment;
import com.ticket.system.entity.TicketInventory;
import com.ticket.system.mapper.OrderMapper;
import com.ticket.system.mapper.PaymentMapper;
import com.ticket.system.mapper.TicketInventoryMapper;
import com.ticket.system.message.RefundMessage;
import com.ticket.system.mq.producer.RefundProducer;
import com.ticket.system.service.WaitlistService;
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
    @Autowired
    private WaitlistService waitlistService;
    @Autowired
    private TicketInventoryMapper ticketInventoryMapper;

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

            // 退款成功后，恢复票务库存
            try {
                Order order = orderMapper.selectById(orderId);
                if (order != null) {
                    // 查询库存记录
                    TicketInventory inventory = ticketInventoryMapper.selectForUpdate(
                            order.getTrainId(),
                            order.getDepartureDate().toLocalDate().toString(),
                            order.getSeatType());
                    if (inventory != null) {
                        // 恢复库存（+1）
                        int restoreResult = ticketInventoryMapper.increaseInventory(
                                inventory.getId(),
                                inventory.getAvailableCount() + 1,
                                inventory.getVersion());
                        if (restoreResult > 0) {
                            log.info("退票库存已恢复: orderId={}, trainId={}, 恢复后库存={}",
                                    orderId, order.getTrainId(), inventory.getAvailableCount() + 1);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("恢复库存异常 orderId={}", orderId, e);
                // 库存恢复失败不影响退款流程继续执行
            }

            // 退款成功后，将票转入候补池
            try {
                waitlistService.transferRefundTicketToWaitlist(orderId);
            } catch (Exception e) {
                log.error("将退款票转入候补池异常 orderId={}", orderId, e);
            }

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

