package com.ticket.system.dto.response;

import lombok.Data;
import java.util.Date;

@Data
public class LockSeatResult {

    private boolean success;            // 是否成功
    private String message;             // 结果消息
    private String lockId;              // 锁定ID（用于后续操作）
    private Date lockTime;              // 锁定时间
    private Date expireTime;            // 锁定过期时间
    private Long remainingSeconds;      // 剩余锁定时间（秒）

    // 座位信息
    private String seatId;
    private String carriageNumber;
    private String seatNumber;
    private String seatType;

    // 订单预览信息
    private String orderPreviewId;
    private String estimatedAmount;

    /**
     * 成功锁定结果
     */
    public static LockSeatResult success(String lockId, String seatId,
                                         Date expireTime, String orderPreviewId) {
        LockSeatResult result = new LockSeatResult();
        result.setSuccess(true);
        result.setMessage("座位锁定成功");
        result.setLockId(lockId);
        result.setSeatId(seatId);
        result.setLockTime(new Date());
        result.setExpireTime(expireTime);
        result.setOrderPreviewId(orderPreviewId);

        if (expireTime != null) {
            long remaining = (expireTime.getTime() - System.currentTimeMillis()) / 1000;
            result.setRemainingSeconds(remaining > 0 ? remaining : 0L);
        }

        return result;
    }

    /**
     * 锁定失败结果
     */
    public static LockSeatResult fail(String message) {
        LockSeatResult result = new LockSeatResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

    /**
     * 检查是否已过期
     */
    public boolean isExpired() {
        if (expireTime == null) {
            return false;
        }
        return expireTime.before(new Date());
    }

    /**
     * 获取格式化剩余时间
     */
    public String getRemainingTimeFormatted() {
        if (remainingSeconds == null || remainingSeconds <= 0) {
            return "已过期";
        }

        long minutes = remainingSeconds / 60;
        long seconds = remainingSeconds % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }
}