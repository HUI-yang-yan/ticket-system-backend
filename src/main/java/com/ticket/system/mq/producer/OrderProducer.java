package com.ticket.system.mq.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendOrderCreateMessage(Long orderId) {
        log.info("发送订单创建消息: orderId={}", orderId);
        rabbitTemplate.convertAndSend("order.exchange", "order.create", orderId);
    }

    public void sendOrderDelayMessage(Long orderId) {
        log.info("发送订单延迟消息: orderId={}", orderId);
        rabbitTemplate.convertAndSend("order.exchange", "order.delay", orderId);
    }

    public void sendOrderCancelMessage(Long orderId) {
        log.info("发送订单取消消息: orderId={}", orderId);
        rabbitTemplate.convertAndSend("order.exchange", "order.cancel", orderId);
    }
}