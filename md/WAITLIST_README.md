# 候补车票业务说明

## 业务概述

候补车票业务允许乘客在车票售罄时提交候补请求，当有乘客退票时，系统自动将退票放入候补池，并按"先到先得"原则匹配给等待中的乘客。

## 业务流程

```
┌─────────────────────────────────────────────────────────────────┐
│                         候补购票流程                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. 乘客退票                                                     │
│     PaymentController.refundPayment()                            │
│                         │                                        │
│                         ▼                                        │
│  2. RefundConsumer 处理退款                                      │
│     - 更新支付状态为退款成功                                       │
│     - 取消原订单                                                  │
│     - 调用 waitlistService.transferRefundTicketToWaitlist()      │
│                         │                                        │
│                         ▼                                        │
│  3. 票转入候补池                                                  │
│     - 创建 WaitlistTicket 记录                                   │
│     - 状态: AVAILABLE                                            │
│                         │                                        │
│                         ▼                                        │
│  4. 触发候补匹配                                                  │
│     - 查询匹配的 PENDING 候补订单（按创建时间排序）                │
│     - 匹配成功则更新双方状态                                      │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                       候补订单状态流转                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  用户提交 ──► PENDING(待匹配)                                     │
│                            │                                    │
│          ┌─────────────────┼─────────────────┐                  │
│          │                 │                 │                  │
│          ▼                 ▼                 ▼                  │
│    MATCHED(已匹配)   CANCELLED(取消)    EXPIRED(过期)            │
│          │                                                    │
│          ▼                                                    │
│    SUCCESS(购票成功) 或 24h后 ──► EXPIRED                        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                       候补票池状态流转                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  退款转入 ──► AVAILABLE(可候补)                                   │
│                            │                                    │
│          ┌─────────────────┴─────────────────┐                  │
│          │                                 │                  │
│          ▼                                 ▼                  │
│   CLAIMED(已被候补购得)              EXPIRED(过期)              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## 数据库表结构

### waitlist_order 候补订单表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| waitlist_number | VARCHAR(50) | 候补单号 |
| user_id | BIGINT | 用户ID |
| train_id | BIGINT | 车次ID |
| start_station_id | BIGINT | 出发站ID |
| end_station_id | BIGINT | 到达站ID |
| seat_type | VARCHAR(20) | 座位类型 |
| departure_date | DATE | 期望乘车日期 |
| status | VARCHAR(20) | 状态 |
| order_number | VARCHAR(50) | 关联的候补票号 |
| create_time | TIMESTAMP | 创建时间 |
| expire_time | TIMESTAMP | 过期时间 |
| update_time | TIMESTAMP | 更新时间 |

### waitlist_ticket 候补票池表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| ticket_number | VARCHAR(50) | 车票号码 |
| original_order_number | VARCHAR(50) | 原订单号 |
| train_id | BIGINT | 车次ID |
| start_station_id | BIGINT | 出发站ID |
| end_station_id | BIGINT | 到达站ID |
| departure_date | DATE | 乘车日期 |
| seat_type | VARCHAR(20) | 座位类型 |
| carriage_number | VARCHAR(10) | 车厢号 |
| seat_number | VARCHAR(10) | 座位号 |
| price | DECIMAL(10,2) | 票价 |
| status | VARCHAR(20) | 状态 |
| source | VARCHAR(20) | 来源 |
| create_time | TIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | 更新时间 |

## API 接口

### 1. 创建候补购票请求

```
POST /api/waitlist/create
Content-Type: application/json

{
    "trainId": 1,
    "startStationId": 10,
    "endStationId": 20,
    "seatType": "SECOND",
    "departureDate": "2026-04-20"
}
```

响应：
```json
{
    "code": 200,
    "msg": "候补购票请求创建成功",
    "data": {
        "id": 1,
        "waitlistNumber": "W1234567890",
        "trainId": 1,
        "trainNumber": "G1234",
        "startStationName": "北京南",
        "endStationName": "上海虹桥",
        "seatType": "SECOND",
        "seatTypeName": "二等座",
        "departureDate": "2026-04-20",
        "status": "PENDING",
        "statusText": "待匹配",
        "createTime": "2026-04-15 10:00:00"
    }
}
```

### 2. 取消候补订单

```
POST /api/waitlist/cancel/{id}
```

响应：
```json
{
    "code": 200,
    "msg": "候补订单取消成功",
    "data": true
}
```

### 3. 查询用户候补订单列表

```
GET /api/waitlist/user/list
```

响应：
```json
{
    "code": 200,
    "msg": "success",
    "data": [
        {
            "id": 1,
            "waitlistNumber": "W1234567890",
            "trainNumber": "G1234",
            "startStationName": "北京南",
            "endStationName": "上海虹桥",
            "seatType": "SECOND",
            "departureDate": "2026-04-20",
            "status": "MATCHED",
            "statusText": "已匹配，待支付",
            "expireTime": "2026-04-16 10:00:00"
        }
    ]
}
```

### 4. 查询候补池可用票

```
GET /api/waitlist/query?trainId=1&startStationId=10&endStationId=20&seatType=SECOND&departureDate=2026-04-20
```

响应：
```json
{
    "code": 200,
    "msg": "success",
    "data": [
        {
            "id": 1,
            "ticketNumber": "TKT1234567890",
            "trainNumber": "G1234",
            "startStationName": "北京南",
            "endStationName": "上海虹桥",
            "departureDate": "2026-04-20",
            "seatType": "SECOND",
            "price": 553.00,
            "status": "AVAILABLE",
            "statusText": "可候补"
        }
    ]
}
```

### 5. 查询所有候补池可用票

```
GET /api/waitlist/ticket/available
```

### 6. 候补订单支付成功回调

```
POST /api/waitlist/paid/{waitlistNumber}
```

### 7. 手动触发候补队列处理（管理接口）

```
POST /api/waitlist/process
```

## 候补订单状态说明

| 状态 | 说明 |
|------|------|
| PENDING | 待匹配 - 等待系统匹配候补票 |
| MATCHED | 已匹配 - 已匹配到候补票，等待用户支付（24小时有效期）|
| SUCCESS | 购票成功 - 用户已支付，购票完成 |
| EXPIRED | 已过期 - 匹配后24小时未支付或候补票过期 |
| CANCELLED | 已取消 - 用户主动取消候补订单 |

## 候补票池状态说明

| 状态 | 说明 |
|------|------|
| AVAILABLE | 可候补 - 可以被候补购得 |
| CLAIMED | 已被候补购得 - 已被匹配给候补订单 |
| EXPIRED | 已过期 - 候补票已过期 |

## 设计规则

1. **先到先得**：候补订单按创建时间升序匹配，早申请的优先
2. **无需预付**：匹配成功后，用户有24小时时间完成支付
3. **自动匹配**：定时任务每分钟扫描候补队列，自动匹配可用票
4. **超时释放**：匹配后24小时未支付，候补订单和候补票释放回池

## 定时任务

- **WaitlistMatchTask**：每分钟执行一次
  - 扫描所有 PENDING 状态的候补订单
  - 尝试匹配可用候补票
  - 处理已匹配但超时的候补订单（超过24小时）

## 匹配算法

```
1. 根据 trainId + startStationId + endStationId + seatType + departureDate 筛选
2. 查询 AVAILABLE 状态的候补票（按 create_time ASC）
3. 查询 PENDING 状态的候补订单（按 create_time ASC）
4. 取第一个候补票匹配第一个候补订单
5. 更新候补票状态为 CLAIMED
6. 更新候补订单状态为 MATCHED，设置24小时过期时间
```

## 核心类说明

| 类 | 说明 |
|---|---|
| WaitlistController | 候补业务API控制器 |
| WaitlistService | 候补业务服务接口 |
| WaitlistServiceImpl | 候补业务服务实现 |
| WaitlistOrderMapper | 候补订单数据库操作 |
| WaitlistTicketMapper | 候补票池数据库操作 |
| WaitlistMatchTask | 候补队列定时任务 |
| RefundConsumer | 退款消费者（集成候补池）|
