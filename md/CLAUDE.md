# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在本仓库工作时提供指导。

## 日期处理规则

处理车票查询时，必须将相对日期转换为具体日期：
- "今天" → 2026-04-19（当前日期）
- "明天" → 2026-04-20
- "后天" → 2026-04-21
- "大后天" → 2026-04-22

**重要**：如果用户未指定具体日期（如只说"明天"），必须先确认具体日期再查询。

## 编译与运行命令

```bash
# 编译项目（需要 JDK 1.8）
mvn clean compile

# 打包
mvn clean package

# 运行
mvn spring-boot:run

# 指定 profile 运行
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**环境要求**：JDK 1.8、MySQL 8.0+、Redis 6.0+、RabbitMQ 3.9+、Maven 3.6+

## 架构概述

### 核心业务流程

**购票流程**：`TicketController` → `TicketServiceImpl.purchaseTicket()`
1. 获取分布式锁（`ticket:lock:{trainId}:{date}:{seatType}`）
2. 使用 `SELECT FOR UPDATE` 查询库存（悲观锁）
3. 通过乐观锁扣减库存（`version` 字段）
4. 更新 Redis 缓存

**退票流程**：`PaymentController.refundPayment()` → `RefundConsumer`
1. 更新支付状态
2. 取消原订单
3. 恢复库存
4. 将车票转入候补池

**候补流程**（详见 `WAITLIST_README.md`）：
- 退票 → 候补池 → 自动匹配待处理订单 → 用户支付 → 生成正式订单
- 匹配规则：按创建时间 FIFO，支付窗口 24 小时
- `WaitlistMatchTask` 每分钟执行匹配

### 关键设计模式

**分布式锁**：通过 `RedisUtil.lock/unlock` 使用 Redisson 实现
- 锁 key 格式：`{prefix}:{entityId}:{...}`
- 必须在 `finally` 块中释放

**乐观锁**：库存使用 `version` 字段
- `reduceInventory(id, newCount, version)` 若版本不匹配返回 0

**消息队列**：RabbitMQ + 手动 ACK
- `order.queue` - 订单创建消息
- `ticket.queue` - 票务消息
- `refund.queue` - 退款处理
- `refund.delay.queue` - 30秒 TTL 重试队列

**Token 认证**：JWT + 自动续期
- 有效期 24 小时
- Redis 缓存用户信息（key 为 token）
- `JwtInterceptor` 在 token 剩余时间 < 30 分钟时自动续期
- 响应头 `Token-Renew: true` 表示返回了新 token

### 核心文件

| 文件 | 用途 |
|------|------|
| `service/impl/TicketServiceImpl.java` | 库存扣减逻辑 |
| `service/impl/WaitlistServiceImpl.java` | 候补匹配算法 |
| `mq/consumer/RefundConsumer.java` | 退款与候补池集成 |
| `common/util/RedisUtil.java` | 分布式锁工具 |
| `common/util/JwtUtil.java` | Token 生成与解析 |
| `config/RabbitMQConfig.java` | 队列定义与绑定 |

### 数据库

- MySQL + HikariCP 连接池
- MyBatis mapper 位于 `src/main/resources/mapper/*.xml`
- 核心表：`order`、`payment`、`ticket_inventory`、`waitlist_order`、`waitlist_ticket`

### 关键配置

`application.yml` 中的重要配置：
- `jwt.expire`：86400000（24 小时）
- `order.expire-time`：1800000（30 分钟）
- `ticket.lock-time`：300000（5 分钟）

### 定时任务

| 任务 | 执行频率 | 用途 |
|------|----------|------|
| `WaitlistMatchTask` | 每分钟 | 匹配候补订单与可用票 |
| `OrderTask.autoCancelExpiredOrders` | 每5分钟 | 取消超时未支付订单 |
| `TicketTask.addTicket` | 每天12:00 | 生成未来7天库存 |

### API 基础路径

所有 API 位于 `/api` 下（context-path 在 `application.yml` 配置）

Swagger UI：`http://localhost:8080/api/swagger-ui.html`

