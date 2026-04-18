package com.ticket.system.task;

import com.ticket.system.service.WaitlistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 候补队列定时任务
 * 负责自动匹配候补订单和定时清理过期候补订单
 */
@Slf4j
@Component
public class WaitlistMatchTask {

    @Autowired
    private WaitlistService waitlistService;

    /**
     * 每分钟执行一次，扫描候补队列进行自动匹配
     * 同时处理已匹配但超时的候补订单
     */
    @Scheduled(fixedRate = 60000) // 每60秒执行一次
    public void processWaitlistQueue() {
        log.info("开始执行候补队列定时任务...");

        try {
            waitlistService.autoProcessWaitlist();
            log.info("候补队列定时任务执行完成");
        } catch (Exception e) {
            log.error("候补队列定时任务执行异常", e);
        }
    }
}
