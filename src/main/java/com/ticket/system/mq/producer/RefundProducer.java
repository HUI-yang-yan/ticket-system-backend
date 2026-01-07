package com.ticket.system.mq.producer;

import com.ticket.system.message.RefundMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RefundProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void send(RefundMessage message) {
        rabbitTemplate.convertAndSend(
                "order.exchange",
                "order.refund",
                message
        );

    }

    // 延迟发送（重试用）
    public void sendDelay(RefundMessage msg) {
        rabbitTemplate.convertAndSend(
                "refund.delay.exchange",
                "refund.delay.key",
                msg
        );
    }
}
