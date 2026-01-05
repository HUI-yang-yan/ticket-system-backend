package com.ticket.system.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class PaymentResult {

    private boolean success;            // 是否成功
    private String message;             // 结果消息
    private String paymentNumber;       // 支付流水号
    private String orderNumber;         // 订单号
    private BigDecimal amount;          // 支付金额
    private String paymentMethod;       // 支付方式
    private String paymentStatus;       // 支付状态
    private Date paymentTime;           // 支付时间

    // 支付渠道返回信息
    private String channelTradeNo;      // 渠道交易号
    private String channelResponse;     // 渠道返回信息

    // 票据信息
    private String ticketNumbers;       // 车票号码（多个用逗号分隔）
    private String qrCodeUrl;           // 取票二维码URL
    private String downloadUrl;         // 车票下载URL

    // 错误信息
    private String errorCode;
    private String errorDetail;

    /**
     * 支付成功结果
     */
    public static PaymentResult success(String paymentNumber, String orderNumber,
                                        BigDecimal amount, String paymentMethod,
                                        String ticketNumbers) {
        PaymentResult result = new PaymentResult();
        result.setSuccess(true);
        result.setMessage("支付成功");
        result.setPaymentNumber(paymentNumber);
        result.setOrderNumber(orderNumber);
        result.setAmount(amount);
        result.setPaymentMethod(paymentMethod);
        result.setPaymentStatus("SUCCESS");
        result.setPaymentTime(new Date());
        result.setTicketNumbers(ticketNumbers);
        return result;
    }

    /**
     * 支付处理中结果
     */
    public static PaymentResult processing(String paymentNumber, String orderNumber,
                                           BigDecimal amount, String paymentMethod) {
        PaymentResult result = new PaymentResult();
        result.setSuccess(true);
        result.setMessage("支付处理中");
        result.setPaymentNumber(paymentNumber);
        result.setOrderNumber(orderNumber);
        result.setAmount(amount);
        result.setPaymentMethod(paymentMethod);
        result.setPaymentStatus("PROCESSING");
        return result;
    }

    /**
     * 支付失败结果
     */
    public static PaymentResult fail(String paymentNumber, String orderNumber,
                                     String errorCode, String errorMessage) {
        PaymentResult result = new PaymentResult();
        result.setSuccess(false);
        result.setMessage("支付失败");
        result.setPaymentNumber(paymentNumber);
        result.setOrderNumber(orderNumber);
        result.setPaymentStatus("FAILED");
        result.setErrorCode(errorCode);
        result.setErrorDetail(errorMessage);
        return result;
    }

    /**
     * 退款结果
     */
    public static PaymentResult refund(String paymentNumber, String orderNumber,
                                       BigDecimal amount, String refundNumber) {
        PaymentResult result = new PaymentResult();
        result.setSuccess(true);
        result.setMessage("退款成功");
        result.setPaymentNumber(paymentNumber);
        result.setOrderNumber(orderNumber);
        result.setAmount(amount.negate()); // 负金额表示退款
        result.setPaymentStatus("REFUNDED");
        result.setPaymentTime(new Date());
        return result;
    }
}