package com.ticket.system.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class PaymentResultDTO {

    private Long id;
    private Long orderId;
    private String orderNumber;
    private String paymentNumber;
    private BigDecimal paymentAmount;
    private String paymentAmountText;
    private BigDecimal orderAmount;
    private String orderAmountText;
    private String paymentMethod;
    private String paymentMethodText;
    private String paymentChannel;
    private Integer paymentStatus;
    private String paymentStatusText;
    private Date paymentTime;
    private Date createTime;
    private Date updateTime;

    // 支付方式文本
    public String getPaymentMethodText() {
        switch (paymentMethod) {
            case "ALIPAY": return "支付宝";
            case "WECHAT": return "微信支付";
            case "BANK": return "银行卡";
            default: return "其他";
        }
    }

    // 支付状态文本
    public String getPaymentStatusText() {
        switch (paymentStatus) {
            case 0: return "支付中";
            case 1: return "支付成功";
            case 2: return "支付失败";
            case 3: return "已退款";
            default: return "未知";
        }
    }

    // 格式化支付金额
    public String getPaymentAmountText() {
        if (paymentAmount == null) {
            return "0.00";
        }
        return "¥" + paymentAmount.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
    }

    // 格式化订单金额
    public String getOrderAmountText() {
        if (orderAmount == null) {
            return "0.00";
        }
        return "¥" + orderAmount.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
    }

    // 检查支付是否成功
    public boolean isSuccess() {
        return paymentStatus == 1;
    }
}