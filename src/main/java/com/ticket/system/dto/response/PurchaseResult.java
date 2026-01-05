package com.ticket.system.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class PurchaseResult {

    private boolean success;            // 是否成功
    private String message;             // 结果消息
    private String orderNumber;         // 订单号
    private BigDecimal amount;          // 支付金额
    private Date expireTime;            // 支付过期时间
    private Long remainingSeconds;      // 剩余支付秒数
    private String qrCodeUrl;           // 支付二维码URL（可选）
    private String paymentNumber;       // 支付流水号（可选）
    private String redirectUrl;         // 支付跳转URL（可选）

    // 购票详情
    private TicketPurchaseDetail purchaseDetail;

    // 错误信息
    private String errorCode;
    private String errorMessage;

    /**
     * 成功结果
     */
    public static PurchaseResult success(String orderNumber, BigDecimal amount, Date expireTime,
                                         TicketPurchaseDetail detail) {
        PurchaseResult result = new PurchaseResult();
        result.setSuccess(true);
        result.setMessage("购票成功");
        result.setOrderNumber(orderNumber);
        result.setAmount(amount);
        result.setExpireTime(expireTime);
        result.setPurchaseDetail(detail);

        // 计算剩余支付时间
        if (expireTime != null) {
            long remaining = (expireTime.getTime() - System.currentTimeMillis()) / 1000;
            result.setRemainingSeconds(remaining > 0 ? remaining : 0L);
        }

        return result;
    }

    /**
     * 成功结果（带支付信息）
     */
    public static PurchaseResult successWithPayment(String orderNumber, BigDecimal amount,
                                                    Date expireTime, String paymentNumber,
                                                    String qrCodeUrl, TicketPurchaseDetail detail) {
        PurchaseResult result = success(orderNumber, amount, expireTime, detail);
        result.setPaymentNumber(paymentNumber);
        result.setQrCodeUrl(qrCodeUrl);
        return result;
    }

    /**
     * 失败结果
     */
    public static PurchaseResult fail(String message) {
        PurchaseResult result = new PurchaseResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

    /**
     * 失败结果（带错误码）
     */
    public static PurchaseResult fail(String errorCode, String message) {
        PurchaseResult result = fail(message);
        result.setErrorCode(errorCode);
        result.setErrorMessage(message);
        return result;
    }

    /**
     * 检查是否过期
     */
    public boolean isExpired() {
        if (expireTime == null) {
            return false;
        }
        return expireTime.before(new Date());
    }

    /**
     * 获取格式化后的剩余支付时间
     */
    public String getRemainingTimeFormatted() {
        if (remainingSeconds == null || remainingSeconds <= 0) {
            return "已过期";
        }

        long hours = remainingSeconds / 3600;
        long minutes = (remainingSeconds % 3600) / 60;
        long seconds = remainingSeconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
}