package com.ticket.system.mq.consumer;

import com.ticket.system.common.util.RedisUtil;
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

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 车票队列消息处理
     * 消息格式: "type:data"
     * type=lock: 车票锁定消息 "lock:ticketId:userId"
     * type=purchase: 车票购买消息 "purchase:ticketId:userId:departureDate:seatType"
     */
    @RabbitListener(queues = "ticket.queue")
    public void handleTicketMessage(String message) {
        log.info("收到车票消息: {}", message);

        if (message == null || message.isEmpty()) {
            return;
        }

        String[] parts = message.split(":");
        if (parts.length < 2) {
            log.warn("车票消息格式错误: {}", message);
            return;
        }

        String type = parts[0];

        try {
            switch (type) {
                case "lock":
                    handleTicketLock(parts);
                    break;
                case "purchase":
                    handleTicketPurchase(parts);
                    break;
                default:
                    log.warn("未知车票消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("处理车票消息异常: message={}", message, e);
        }
    }

    private void handleTicketLock(String[] parts) {
        if (parts.length < 3) {
            log.warn("车票锁定消息格式错误: {}", String.join(":", parts));
            return;
        }
        Long ticketId = Long.parseLong(parts[1]);
        Long userId = Long.parseLong(parts[2]);
        log.info("处理车票锁定消息: ticketId={}, userId={}", ticketId, userId);
        // 车票锁定逻辑已在前端或Controller中处理
        // 这里可以记录锁定日志、更新统计等
    }

    private void handleTicketPurchase(String[] parts) {
        if (parts.length < 5) {
            log.warn("车票购买消息格式错误: {}", String.join(":", parts));
            return;
        }
        Long trainId = Long.parseLong(parts[1]);
        Long userId = Long.parseLong(parts[2]);
        String departureDate = parts[3];
        String seatType = parts[4];

        log.info("处理车票购买消息: trainId={}, userId={}, departureDate={}, seatType={}",
                trainId, userId, departureDate, seatType);

        try {
            // 调用购票服务
            // 注意：startStationId和endStationId需要从ticketId查询获取，这里简化处理传null
            boolean success = ticketService.purchaseTicket(trainId, userId, departureDate, seatType, null, null);
            log.info("车票购买结果: trainId={}, success={}", trainId, success);
        } catch (Exception e) {
            log.error("车票购买失败: trainId={}, userId={}", trainId, userId, e);
        }
    }
}
