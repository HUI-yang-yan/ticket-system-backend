package com.ticket.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = "com.ticket.system")
@EnableCaching
public class TicketApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketApplication.class, args);
    }
}

//1. 实现库存扣减/恢复逻辑 - 在购票和退票时正确操作 train_segment_stock.stock
//2. 为候补匹配加分布式锁 - 防止同一候补票被重复匹配
//3. 实现候补支付后的出票流程 - 生成正式订单、分配座位