package com.ticket.system.mq.consumer;

import com.ticket.system.common.exception.BusinessException;
import com.ticket.system.common.exception.ErrorCode;
import com.ticket.system.common.util.RedisUtil;
import com.ticket.system.entity.Order;
import com.ticket.system.entity.Payment;
import com.ticket.system.entity.TicketInventory;
import com.ticket.system.mapper.OrderMapper;
import com.ticket.system.mapper.PaymentMapper;
import com.ticket.system.mapper.TicketInventoryMapper;
import com.ticket.system.mapper.TrainSegmentStockMapper;
import com.ticket.system.mapper.TrainStationMapper;
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

import java.time.LocalDate;
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
    @Autowired
    private TrainStationMapper trainStationMapper;
    @Autowired
    private TrainSegmentStockMapper trainSegmentStockMapper;
    @Autowired
    private RedisUtil redisUtil;

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

            // 查询订单
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                log.warn("退款订单不存在: orderId={}", orderId);
                return;
            }

            // 幂等性检查：检查订单是否已退款
            if (order.getOrderStatus() == 4) {
                log.warn("订单已退款，跳过处理: orderId={}", orderId);
                return;
            }

            // 查询支付记录（可能不存在，如余额支付）
            Payment payment = paymentMapper.selectByOrderId(orderId);
            if (payment != null) {
                if (payment.getPaymentStatus() != 1) {
                    return;
                }
                // 检查是否已退款
                if (payment.getRefundStatus() != 0) {
                    log.warn("支付记录已处于退款状态，跳过处理: orderId={}, refundStatus={}", orderId, payment.getRefundStatus());
                    return;
                }

                // 原子标记退款中
                int updated = paymentMapper.lockRefund(orderId);
                if (updated == 0) {
                    return;
                }

                // 更新为已退款
                paymentMapper.updateRefundSuccessWithLock(orderId);
            }

            // 更新订单状态为已退款（4）
            orderMapper.updateOrderStatus(orderId, 4);

            // 退款成功后，恢复区段票务库存
            try {
                Integer departureIndex = trainStationMapper.selectStationIndexByTrainIdAndStationId(
                        order.getTrainId(), order.getDepartureStationId());
                Integer arrivalIndex = trainStationMapper.selectStationIndexByTrainIdAndStationId(
                        order.getTrainId(), order.getArrivalStationId());
                if (departureIndex != null && arrivalIndex != null) {
                    int restoreResult = trainSegmentStockMapper.restoreSegmentStock(
                            order.getTrainId(),
                            order.getDepartureDate().toLocalDate(),
                            order.getSeatType(),
                            departureIndex,
                            arrivalIndex);
                    if (restoreResult > 0) {
                        log.info("退票区段库存已恢复: orderId={}, trainId={}, seatType={}, departureIndex={}, arrivalIndex={}",
                                orderId, order.getTrainId(), order.getSeatType(), departureIndex, arrivalIndex);

                        // 清理相关 Redis 缓存，确保查询到最新库存
                        invalidateTicketCache(order.getTrainId(), order.getDepartureDate().toLocalDate(),
                                order.getSeatType(), departureIndex, arrivalIndex);
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

    /**
     * 清理票务查询缓存
     * 确保退款后余票查询能立即看到最新库存
     */
    private void invalidateTicketCache(Long trainId, LocalDate departureDate, String seatType,
                                       Integer departureIndex, Integer arrivalIndex) {
        try {
            // 缓存 key 格式: ticket:stock:{trainId}:{date}:{seatType}:{startIndex}-{endIndex}
            String cacheKey = String.format("ticket:stock:%d:%s:%s:%d-%d",
                    trainId, departureDate, seatType, departureIndex, arrivalIndex);
            redisUtil.delete(cacheKey);
            log.info("已清理票务缓存: key={}", cacheKey);
        } catch (Exception e) {
            log.warn("清理票务缓存失败", e);
        }
    }
}

