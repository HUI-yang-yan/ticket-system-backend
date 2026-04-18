# 12306火车票订票系统

基于 Spring Boot 2.7.14 开发的火车票订票系统，支持用户注册登录、车次查询、购票、退票、候补购票等核心功能。

## 技术栈

| 技术 | 说明 |
|------|------|
| Spring Boot 2.7.14 | 核心框架 |
| MyBatis | 数据访问层 |
| MySQL | 数据库 |
| Redis (Redisson) | 分布式锁、缓存 |
| RabbitMQ | 消息队列 |
| JWT | 用户认证 |
| Quartz | 定时任务 |
| Lombok | 简化代码 |

## 项目结构

```
src/main/java/com/ticket/system/
├── controller/          # REST API控制器
├── service/            # 服务接口
│   └── impl/          # 服务实现
├── mapper/            # MyBatis Mapper接口
├── entity/            # 实体类
├── dto/               # 数据传输对象
│   ├── request/       # 请求DTO
│   └── response/      # 响应DTO
├── config/            # 配置类
├── common/            # 公共组件
│   ├── constant/      # 常量定义
│   ├── exception/     # 异常处理
│   ├── result/        # 统一响应结果
│   └── util/         # 工具类
├── mq/                # 消息队列
│   ├── producer/     # 消息生产者
│   └── consumer/     # 消息消费者
├── task/              # 定时任务
├── aspect/           # AOP切面
└── interceptor/      # 拦截器
```

## 核心功能

### 1. 用户模块
- 用户注册 `/api/user/register`
- 用户登录 `/api/user/login`
- 获取用户信息 `/api/user/info`

### 2. 车次模块
- 查询车次列表 `/api/train/list`
- 查询车次详情 `/api/train/{trainId}`
- 根据站点查询车次 `/api/train/stations`

### 3. 车站模块
- 查询车站列表 `/api/station/list`
- 查询车次经停车站 `/api/trainStation/{trainId}`

### 4. 票务模块
- 查询余票 `/api/ticket/query`
- 查询余票详情 `/api/ticket/{ticketId}`
- 购票 `/api/ticket/purchase/{trainId}`

### 5. 订单模块
- 创建订单 `/api/order/create`
- 查询订单 `/api/order/{orderId}`
- 按订单号查询 `/api/order/number/{orderNumber}`
- 查询用户订单 `/api/order/user/list`
- 取消订单 `/api/order/cancel/{orderId}`
- 支付订单 `/api/order/pay/{orderId}`

### 6. 支付模块
- 支付订单 `/api/payment/pay/{orderId}`
- 退款 `/api/payment/refund/{orderId}`
- 查询支付状态 `/api/payment/{orderId}`

### 7. 退票模块
- 申请退票 `/api/payment/refund/{orderId}`（调用退款流程）

### 8. 候补购票模块
候补购票功能允许乘客在车票售罄时提交候补请求，当有乘客退票时，系统自动匹配。

| 接口 | 说明 |
|------|------|
| POST `/api/waitlist/create` | 创建候补购票请求 |
| POST `/api/waitlist/cancel/{id}` | 取消候补订单 |
| GET `/api/waitlist/user/list` | 查询用户候补订单 |
| GET `/api/waitlist/query` | 查询候补池可用票 |
| GET `/api/waitlist/ticket/available` | 查询所有可用候补票 |
| POST `/api/waitlist/process` | 手动触发候补队列处理 |

详细说明请参考 [候补购票业务说明](WAITLIST_README.md)

### 9. 联系人模块
- 添加联系人 `/api/contact/add`
- 查询联系人 `/api/contact/{contactId}`
- 查询用户联系人 `/api/contact/user/list`
- 修改联系人 `/api/contact/{contactId}`
- 删除联系人 `/api/contact/{contactId}`

## 数据库表结构

### 核心表

| 表名 | 说明 |
|------|------|
| `user` | 用户表 |
| `train` | 车次表 |
| `station` | 车站表 |
| `train_station` | 车次经停站表 |
| `carriage` | 车厢表 |
| `seat` | 座位表 |
| `order` | 订单表 |
| `order_seat` | 订单座位关联表 |
| `payment` | 支付记录表 |
| `refund_record` | 退款记录表 |
| `refund_ticket` | 退票表 |
| `ticket_inventory` | 票务库存表 |
| `train_segment_stock` | 车次区间库存表 |
| `contact` | 联系人表 |

### 候补相关表

| 表名 | 说明 |
|------|------|
| `waitlist_order` | 候补订单表 |
| `waitlist_ticket` | 候补票池表 |

详细建表SQL请参考 `WAITLIST_README.md`

## 配置文件

### application.yml

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:mysql://192.168.171.134:3306/ticket_system?useUnicode=true&characterEncoding=utf8
    username: root
    password: root

  redis:
    host: 192.168.171.134
    port: 6379

  rabbitmq:
    host: 192.168.171.134
    port: 5672
```

## 定时任务

| 任务 | 说明 | 执行频率 |
|------|------|----------|
| `TicketTask.addTicket` | 生成未来7天票务库存 | 每天12:00 |
| `TicketTask.deleteOldTicket` | 删除过期票务数据 | 每天12:00 |
| `OrderTask.autoCancelExpiredOrders` | 自动取消过期订单 | 每5分钟 |
| `WaitlistMatchTask.processWaitlistQueue` | 自动匹配候补队列 | 每分钟 |

## 消息队列

| 队列 | 说明 |
|------|------|
| `order.queue` | 订单创建消息 |
| `ticket.queue` | 票务消息 |
| `refund.queue` | 退款消息 |
| `refund.delay.queue` | 退款延迟重试队列 |

## 开发环境运行

```bash
# 编译项目
mvn clean package

# 运行项目
mvn spring-boot:run

# 或直接运行jar
java -jar target/ticket-12306-system-1.0.0.jar
```

## 环境依赖

- JDK 1.8+
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.9+
- Maven 3.6+

## API文档

启动项目后访问 Swagger UI：
```
http://localhost:8080/api/swagger-ui.html
```

## 项目亮点

1. **分布式锁**：使用 Redisson 实现分布式锁，防止并发问题
2. **消息队列**：使用 RabbitMQ 实现异步处理和延迟重试
3. **定时任务**：使用 Quartz 实现定时任务调度
4. **JWT认证**：无状态认证，支持 token 续期
5. **候补购票**：支持先到先得的候补购票机制
6. **高并发设计**：基于 Redis 的缓存和锁实现高并发场景

## 许可证

MIT License
