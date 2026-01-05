package com.ticket.system.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class RefundRecord {
    private Long id;
    private String refundNumber;        // 退款流水号
    private String orderNumber;         // 订单号
    private Long userId;                // 用户ID
    private BigDecimal refundAmount;    // 申请退款金额
    private BigDecimal actualRefundAmount; // 实际退款金额
    private BigDecimal serviceFee;      // 手续费
    private String refundReason;        // 退款原因
    private String refundReasonCode;    // 退款原因代码
    private String status;              // 状态：APPLIED, PROCESSING, SUCCESS, FAILED
    private String paymentMethod;       // 支付方式
    private String refundMethod;        // 退款方式
    private String bankName;            // 银行名称（银行卡退款）
    private String bankCardNo;          // 银行卡号
    private String cardHolderName;      // 持卡人姓名
    private String channelRefundNo;     // 渠道退款单号
    private String remark;              // 备注
    private Date applyTime;             // 申请时间
    private Date processTime;           // 处理时间
    private Date completeTime;          // 完成时间
    private Date createTime;
    private Date updateTime;
}

