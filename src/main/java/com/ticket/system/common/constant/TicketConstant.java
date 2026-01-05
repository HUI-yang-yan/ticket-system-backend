package com.ticket.system.common.constant;

public class TicketConstant {

    // 座位类型
    public static final String SEAT_TYPE_BUSINESS = "BUSINESS";      // 商务座
    public static final String SEAT_TYPE_FIRST = "FIRST";            // 一等座
    public static final String SEAT_TYPE_SECOND = "SECOND";          // 二等座
    public static final String SEAT_TYPE_SOFT_SLEEPER = "SOFT_SLEEPER";   // 软卧
    public static final String SEAT_TYPE_HARD_SLEEPER = "HARD_SLEEPER";   // 硬卧
    public static final String SEAT_TYPE_SOFT_SEAT = "SOFT_SEAT";    // 软座
    public static final String SEAT_TYPE_HARD_SEAT = "HARD_SEAT";    // 硬座

    // 列车类型
    public static final String TRAIN_TYPE_G = "G";                   // 高铁
    public static final String TRAIN_TYPE_D = "D";                   // 动车
    public static final String TRAIN_TYPE_Z = "Z";                   // 直达特快
    public static final String TRAIN_TYPE_T = "T";                   // 特快
    public static final String TRAIN_TYPE_K = "K";                   // 快速
    public static final String TRAIN_TYPE_L = "L";                   // 临客

    // 车厢类型
    public static final String CARRIAGE_TYPE_BUSINESS = "BUSINESS";
    public static final String CARRIAGE_TYPE_FIRST = "FIRST";
    public static final String CARRIAGE_TYPE_SECOND = "SECOND";
    public static final String CARRIAGE_TYPE_SLEEPER = "SLEEPER";
    public static final String CARRIAGE_TYPE_DINING = "DINING";      // 餐车

    // 车票状态
    public static final int TICKET_STATUS_AVAILABLE = 1;             // 可用
    public static final int TICKET_STATUS_SOLD = 2;                  // 已售
    public static final int TICKET_STATUS_LOCKED = 3;                // 锁定
}