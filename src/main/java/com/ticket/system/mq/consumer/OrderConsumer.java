package com.ticket.system.mq.consumer;

import com.ticket.system.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderConsumer {

    @Autowired
    private OrderService orderService;

    /**
     * 订单队列消息处理
     * 消息格式: "type:orderId"
     * type=create: 订单创建后处理（如发送通知）
     * type=cancel: 订单取消处理（自动取消未支付订单）
     */
    @RabbitListener(queues = "order.queue")
    public void handleOrderMessage(String message) {
        log.info("收到订单消息: {}", message);

        if (message == null || message.isEmpty()) {
            return;
        }

        String[] parts = message.split(":");
        if (parts.length < 2) {
            log.warn("订单消息格式错误: {}", message);
            return;
        }

        String type = parts[0];
        Long orderId = Long.parseLong(parts[1]);

        try {
            switch (type) {
                case "create":
                    handleOrderCreate(orderId);
                    break;
                case "cancel":
                    handleOrderCancel(orderId);
                    break;
                default:
                    log.warn("未知订单消息类型: {}, orderId: {}", type, orderId);
            }
        } catch (Exception e) {
            log.error("处理订单消息异常: message={}", message, e);
        }
    }

    private void handleOrderCreate(Long orderId) {
        log.info("处理订单创建消息: orderId={}", orderId);
        // 处理订单创建后的业务逻辑
        // 如：发送邮件通知、更新统计等
    }

    private void handleOrderCancel(Long orderId) {
        log.info("处理订单取消消息: orderId={}", orderId);
        // 自动取消未支付订单
        try {
            orderService.cancelOrder(orderId);
        } catch (Exception e) {
            log.error("自动取消订单失败: orderId={}", orderId, e);
        }
    }
}
