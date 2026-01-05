package com.ticket.system.mq.consumer;

import com.ticket.system.service.TicketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TicketConsumer {

    @Autowired
    private TicketService ticketService;

    @RabbitListener(queues = "ticket.queue")
    public void handleTicketLockMessage(String message) {
        log.info("收到车票锁定消息: {}", message);
        String[] parts = message.split(":");
        if (parts.length == 2) {
            Long ticketId = Long.parseLong(parts[0]);
            Long userId = Long.parseLong(parts[1]);
            // 处理车票锁定逻辑
        }
    }

    @RabbitListener(queues = "ticket.queue")
    public void handleTicketPurchaseMessage(String message) {
        log.info("收到车票购买消息: {}", message);
        String[] parts = message.split(":");
        if (parts.length == 2) {
            Long ticketId = Long.parseLong(parts[0]);
            Long userId = Long.parseLong(parts[1]);
            // 处理车票购买逻辑
        }
    }
}