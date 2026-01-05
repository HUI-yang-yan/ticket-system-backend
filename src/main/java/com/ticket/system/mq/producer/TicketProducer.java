package com.ticket.system.mq.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TicketProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendTicketLockMessage(Long ticketId, Long userId) {
        log.info("发送车票锁定消息: ticketId={}, userId={}", ticketId, userId);
        rabbitTemplate.convertAndSend("ticket.exchange", "ticket.lock", ticketId + ":" + userId);
    }

    public void sendTicketPurchaseMessage(Long ticketId, Long userId) {
        log.info("发送车票购买消息: ticketId={}, userId={}", ticketId, userId);
        rabbitTemplate.convertAndSend("ticket.exchange", "ticket.purchase", ticketId + ":" + userId);
    }

    public void sendTicketReleaseMessage(Long ticketId) {
        log.info("发送车票释放消息: ticketId={}", ticketId);
        rabbitTemplate.convertAndSend("ticket.exchange", "ticket.release", ticketId);
    }
}