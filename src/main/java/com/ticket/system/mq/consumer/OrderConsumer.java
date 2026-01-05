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

    @RabbitListener(queues = "order.queue")
    public void handleOrderCreateMessage(Long orderId) {
        log.info("收到订单创建消息: orderId={}", orderId);
        // 处理订单创建后的业务逻辑
        // 比如发送邮件通知、更新统计等
    }

    @RabbitListener(queues = "order.queue")
    public void handleOrderCancelMessage(Long orderId) {
        log.info("收到订单取消消息: orderId={}", orderId);
        // 自动取消未支付订单
        orderService.cancelOrder(orderId);
    }
}