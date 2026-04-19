# 12306火车票订票系统

基于 Spring Boot 3.2 + JDK 17 开发的智能化火车票订票系统，支持用户注册登录、车次查询、购票、退票、候补购票等核心功能，并集成 **Spring AI** 提供智能客服体验。

## 技术栈

| 技术 | 说明 |
|------|------|
| Spring Boot 3.2.4 | 核心框架（升级自 2.7.14） |
| Spring AI 1.0.0-M6 | AI 智能助手（集成通义千问/ChatGPT） |
| MyBatis Spring Boot 3.0.3 | 数据访问层 |
| MySQL 8.0 | 数据库 |
| Redis (Redisson 3.27.0) | 分布式锁、缓存、会话管理 |
| RabbitMQ | 消息队列 |
| JWT | 用户认证 |
| Quartz | 定时任务 |
| Lombok | 简化代码 |
| springdoc-openapi 2.5.0 | API 文档（替代 Swagger） |

## 项目结构

```
src/main/java/com/ticket/system/
├── controller/          # REST API 控制器
├── service/            # 服务接口
│   └── impl/          # 服务实现
├── mapper/            # MyBatis Mapper 接口
├── entity/            # 实体类
├── dto/               # 数据传输对象
│   ├── request/       # 请求 DTO
│   └── response/      # 响应 DTO
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
├── aspect/           # AOP 切面
├── interceptor/      # 拦截器
└── ai/               # AI 相关模块
    ├── tools/        # AI Tools（Function Calling）
    ├── prompt/       # AI 提示词模板
    └── SystemConstant.java  # AI 系统常量
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

### 10. AI 智能助手模块

系统集成了基于 Spring AI 的智能助手，支持自然语言交互、智能票务查询、多轮对话等功能。

| 接口 | 说明 |
|------|------|
| POST `/api/ai/chat` | **统一聊天接口**（带会话管理、自动查票） |
| POST `/api/ai/parse` | 解析用户输入为结构化查询参数 |
| GET `/api/ai/history/{sessionId}` | 获取聊天历史 |
| DELETE `/api/ai/history/{sessionId}` | 清除聊天历史 |
| POST `/api/ai/chat/legacy` | 通用对话接口（无会话管理） |
| POST `/api/ai/ticket/consult` | 票务咨询（针对特定车次和日期） |
| POST `/api/ai/order/consult` | 订单咨询（针对特定订单） |
| GET `/api/ai/health` | AI 服务健康检查 |

#### AI 聊天接口使用示例

**请求示例**：
```json
POST /api/ai/chat
{
  "sessionId": "user-123-session",
  "message": "我想查一下明天北京到上海的高铁",
  "autoQuery": true
}
```

**响应示例**：
```json
{
  "success": true,
  "message": "已为您查询到 5 个车次，请查看下方结果。",
  "sessionId": "user-123-session",
  "params": {
    "from": "北京",
    "to": "上海",
    "date": "2026-04-20",
    "timeRange": "any",
    "preference": "fastest"
  },
  "paramMissingCount": 0,
  "missingParams": "无",
  "tickets": [...]
}
```

#### AI 对话特性

1. **意图识别**：自动判断用户是想查询车票还是闲聊打招呼，避免不必要的 API 调用
2. **参数提取**：将自然语言转换为结构化查询参数（出发地、目的地、日期、时间偏好、排序偏好）
3. **多轮对话**：支持会话上下文管理，可连续追问
4. **闲聊问候**：用户打招呼时返回友好问候，不调用 tools

#### AI 配置说明

系统使用通义千问作为默认模型，可在 `application.yml` 中配置：

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}           # API Key（建议使用环境变量）
      base-url: https://dashscope.aliyuncs.com/compatible-mode  # 通义千问地址
      chat:
        options:
          model: qwen-max-latest            # 默认模型
```

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

详细建表 SQL 请参考 `WAITLIST_README.md`

## 配置文件

### application.yml

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:mysql://192.168.171.134:3306/ticket_system
    username: root
    password: 123456

  redis:
    host: 192.168.171.134
    port: 6379

  rabbitmq:
    host: 192.168.171.134
    port: 5672

  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://dashscope.aliyuncs.com/compatible-mode
      chat:
        options:
          model: qwen-max-latest
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
| `refund.delay.queue` | 退款延迟重试队列（30秒 TTL） |

## 开发环境运行

### 环境要求

- **JDK 17+**（重要：已升级至 Java 17）
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.9+
- Maven 3.6+

### 编译运行

```bash
# 克隆项目后进入目录
cd ticket-12306-system

# 编译项目
mvn clean compile

# 打包
mvn clean package

# 运行项目（开发环境）
mvn spring-boot:run

# 或指定 profile 运行
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或直接运行 jar
java -jar target/ticket-12306-system-1.0.0.jar
```

### 配置 AI 功能

1. 设置环境变量或修改 `application.yml` 中的 API Key
2. 默认使用通义千问模型（如需切换其他模型，修改 `spring.ai.openai.base-url` 和 `model`）

## API 文档

启动项目后访问 OpenAPI 文档：

- **Swagger UI**: `http://localhost:8080/api/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/api/v3/api-docs`

## 项目亮点

### 1. 分布式锁
使用 Redisson 实现分布式锁，防止并发问题。锁 key 格式：`{prefix}:{entityId}:{...}`

### 2. 消息队列
使用 RabbitMQ 实现异步处理和延迟重试，支持手动 ACK。

### 3. 定时任务
使用 Quartz 实现定时任务调度，包括票务库存生成、订单自动取消、候补队列匹配。

### 4. JWT 认证
无状态认证，支持 token 续期（剩余时间 < 30 分钟时自动续期）。

### 5. 候补购票
支持先到先得（FIFO）的候补购票机制，退票自动转入候补池。

### 6. AI 智能助手
- 基于 Spring AI + 通义千问/ChatGPT
- 支持 Function Calling 工具调用
- 意图识别 + 参数提取 + 多轮对话
- 闲聊问候不调用 tools，节省资源

### 7. 高并发设计
基于 Redis 的缓存和分布式锁实现高并发场景下的库存扣减。

## 常见问题

### Q: 升级 JDK 版本后报错？
确保使用 JDK 17+，项目已不再支持 JDK 8。

### Q: AI 功能不可用？
1. 检查 `OPENAI_API_KEY` 环境变量是否配置
2. 检查网络能否访问通义千问 API
3. 查看日志确认 AI 服务健康状态：`GET /api/ai/health`

### Q: 候补购票如何工作？
用户提交候补请求 → 有乘客退票时系统自动匹配 → 用户支付（24小时窗口）→ 生成正式订单。详见 `WAITLIST_README.md`

## 许可证

MIT License
