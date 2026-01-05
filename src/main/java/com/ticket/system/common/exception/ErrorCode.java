package com.ticket.system.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "成功"),
    FAILURE(1, "失败"),

    // 用户相关错误 1000-1999
    USER_NOT_EXIST(1001, "用户不存在"),
    USER_NOT_LOGIN(1005,"用户未登录"),
    USER_PASSWORD_ERROR(1002, "密码错误"),
    USER_EXIST(1003, "用户已存在"),
    USER_DISABLED(1004, "用户已被禁用"),

    // 车票相关错误 2000-2999
    TICKET_NOT_EXIST(2001, "车票不存在"),
    TICKET_SOLD_OUT(2002, "车票已售罄"),
    TICKET_QUERY_ERROR(2003, "车票查询失败"),

    // 订单相关错误 3000-3999
    ORDER_NOT_EXIST(3001, "订单不存在"),
    ORDER_EXPIRED(3002, "订单已过期"),
    ORDER_STATUS_ERROR(3003, "订单状态错误"),
    ORDER_CREATE_FAILED(3004, "订单创建失败"),

    // 支付相关错误 4000-4999
    PAYMENT_FAILED(4001, "支付失败"),
    PAYMENT_AMOUNT_ERROR(4002, "支付金额错误"),

    // 系统错误 5000-5999
    SYSTEM_ERROR(5001, "系统错误"),
    PARAM_ERROR(5002, "参数错误"),
    DATABASE_ERROR(5003, "数据库错误"),
    REDIS_ERROR(5004, "Redis错误"),
    RABBITMQ_ERROR(5005, "消息队列错误"),
    UNAUTHORIZED(5006,"未携带token"),

    // 权限相关错误 6000-6999
    TOKEN_INVALID(6001, "Token无效"),
    TOKEN_EXPIRED(6002, "Token已过期"),
    ACCESS_DENIED(6003, "访问被拒绝"),

    // 业务逻辑错误 7000-7999
    SEAT_OCCUPIED(7001, "座位已被占用"),
    SEAT_LOCK_FIRST(7004,"请先锁定座位"),
    TRAIN_NOT_EXIST(7002, "列车不存在"),
    STATION_NOT_EXIST(7003, "车站不存在");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}