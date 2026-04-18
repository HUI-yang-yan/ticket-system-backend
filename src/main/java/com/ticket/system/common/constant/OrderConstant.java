package com.ticket.system.common.constant;

public class OrderConstant {

    // 订单状态
    public static final int ORDER_STATUS_PENDING = 0;    // 待支付
    public static final int ORDER_STATUS_PAID = 1;       // 已支付
    public static final int ORDER_STATUS_CANCELLED = 2;  // 已取消
    public static final int ORDER_STATUS_COMPLETED = 3;  // 已完成

    // 支付状态
    public static final int PAY_STATUS_PENDING = 0;      // 未支付
    public static final int PAY_STATUS_PAID = 1;         // 已支付

    // 支付方式
    public static final String PAY_METHOD_ALIPAY = "ALIPAY";
    public static final String PAY_METHOD_WECHAT = "WECHAT";
    public static final String PAY_METHOD_BANK = "BANK";

    // 订单过期时间（30分钟）
    public static final long ORDER_EXPIRE_TIME = 30 * 60 * 1000L;

    // ==================== 候补订单相关常量 ====================

    // 候补订单状态
    public static final String WAITLIST_STATUS_PENDING = "PENDING";      // 待匹配
    public static final String WAITLIST_STATUS_MATCHED = "MATCHED";     // 已匹配
    public static final String WAITLIST_STATUS_SUCCESS = "SUCCESS";    // 购票成功
    public static final String WAITLIST_STATUS_EXPIRED = "EXPIRED";     // 已过期
    public static final String WAITLIST_STATUS_CANCELLED = "CANCELLED"; // 已取消

    // 候补票状态
    public static final String WAITLIST_TICKET_AVAILABLE = "AVAILABLE"; // 可候补
    public static final String WAITLIST_TICKET_CLAIMED = "CLAIMED";     // 已被候补购得
    public static final String WAITLIST_TICKET_EXPIRED = "EXPIRED";     // 已过期

    // 候补票来源
    public static final String WAITLIST_TICKET_SOURCE_REFUND = "REFUND"; // 退票来源

    // 候补订单匹配后支付有效期（24小时）
    public static final long WAITLIST_MATCH_EXPIRE_TIME = 24 * 60 * 60 * 1000L;
}