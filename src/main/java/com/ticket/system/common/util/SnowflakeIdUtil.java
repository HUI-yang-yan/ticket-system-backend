package com.ticket.system.common.util;

import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdUtil {

    // 起始的时间戳
    private final long START_TIMESTAMP = 1640995200000L; // 2022-01-01 00:00:00

    // 每一部分占用的位数
    private final long SEQUENCE_BIT = 12;   // 序列号占用的位数
    private final long MACHINE_BIT = 5;     // 机器标识占用的位数
    private final long DATACENTER_BIT = 5;  // 数据中心占用的位数

    // 每一部分的最大值
    private final long MAX_DATACENTER_NUM = -1L ^ (-1L << DATACENTER_BIT);
    private final long MAX_MACHINE_NUM = -1L ^ (-1L << MACHINE_BIT);
    private final long MAX_SEQUENCE = -1L ^ (-1L << SEQUENCE_BIT);

    // 每一部分向左的位移
    private final long MACHINE_LEFT = SEQUENCE_BIT;
    private final long DATACENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;
    private final long TIMESTAMP_LEFT = DATACENTER_LEFT + DATACENTER_BIT;

    // 数据中心ID
    private long datacenterId = 1;
    // 机器ID
    private long machineId = 1;
    // 序列号
    private long sequence = 0L;
    // 上一次时间戳
    private long lastTimestamp = -1L;

    public SnowflakeIdUtil() {
        // 参数校验
        if (datacenterId > MAX_DATACENTER_NUM || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId can't be greater than " + MAX_DATACENTER_NUM + " or less than 0");
        }
        if (machineId > MAX_MACHINE_NUM || machineId < 0) {
            throw new IllegalArgumentException("machineId can't be greater than " + MAX_MACHINE_NUM + " or less than 0");
        }
    }

    /**
     * 生成下一个ID
     */
    public synchronized long nextId() {
        long currentTimestamp = getCurrentTimestamp();

        if (currentTimestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id");
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0L) {
                currentTimestamp = getNextTimestamp();
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        return ((currentTimestamp - START_TIMESTAMP) << TIMESTAMP_LEFT)
                | (datacenterId << DATACENTER_LEFT)
                | (machineId << MACHINE_LEFT)
                | sequence;
    }

    /**
     * 生成字符串ID
     */
    public String nextIdString() {
        return String.valueOf(nextId());
    }

    /**
     * 生成订单号
     */
    public String generateOrderNumber() {
        return "TICKET" + nextId();
    }

    /**
     * 生成支付流水号
     */
    public String generatePaymentNumber() {
        return "PAY" + System.currentTimeMillis() + nextId() % 10000;
    }

    private long getNextTimestamp() {
        long timestamp = getCurrentTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentTimestamp();
        }
        return timestamp;
    }

    private long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
}