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
}