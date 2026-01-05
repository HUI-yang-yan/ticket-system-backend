package com.ticket.system.common.constant;

public class RedisConstant {

    // 用户相关
    public static final String USER_TOKEN_PREFIX = "user:token:";
    public static final String USER_INFO_PREFIX = "user:info:";

    // 车票相关
    public static final String TICKET_INVENTORY_PREFIX = "ticket:inventory:";
    public static final String TICKET_LOCK_PREFIX = "ticket:lock:";
    public static final String TICKET_SEAT_PREFIX = "ticket:seat:";

    // 订单相关
    public static final String ORDER_LOCK_PREFIX = "order:lock:";
    public static final String ORDER_TEMP_PREFIX = "order:temp:";

    // 缓存时间（秒）
    public static final long USER_TOKEN_EXPIRE = 24 * 60 * 60L;      // 24小时
    public static final long TICKET_INVENTORY_EXPIRE = 60 * 60L;     // 1小时
    public static final long LOCK_EXPIRE = 10L;                       // 10秒
}