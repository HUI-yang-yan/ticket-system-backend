package com.ticket.system.dto.response;

import com.ticket.system.common.util.DateUtil;
import lombok.Data;
import com.ticket.system.common.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class RefundResult {

    private boolean success;                // 是否成功
    private String message;                 // 结果消息
    private String refundId;                // 退款ID（内部）
    private String refundNumber;            // 退款流水号
    private String orderNumber;             // 原订单号
    private BigDecimal refundAmount;        // 退款金额
    private BigDecimal actualRefundAmount;  // 实际退款金额（扣除手续费）
    private BigDecimal serviceFee;          // 手续费
    private String refundStatus;            // 退款状态
    private String refundStatusText;        // 退款状态文本
    private Date applyTime;                 // 申请时间
    private Date processTime;               // 处理时间
    private Date completeTime;              // 完成时间
    private String refundMethod;            // 退款方式（原路退回、银行卡等）
    private String refundChannel;           // 退款渠道
    private String refundChannelNo;         // 渠道退款单号

    // 车票信息
    private List<RefundTicketInfo> ticketInfos;

    // 错误信息
    private String errorCode;
    private String errorMessage;
    private String errorDetail;

    // 退款规则信息
    private RefundRuleInfo refundRule;

    /**
     * 成功结果
     */
    public static RefundResult success(String refundNumber, String orderNumber,
                                       BigDecimal refundAmount, BigDecimal actualRefundAmount,
                                       BigDecimal serviceFee, List<RefundTicketInfo> ticketInfos) {
        RefundResult result = new RefundResult();
        result.setSuccess(true);
        result.setMessage("退款申请已提交");
        result.setRefundNumber(refundNumber);
        result.setOrderNumber(orderNumber);
        result.setRefundAmount(refundAmount);
        result.setActualRefundAmount(actualRefundAmount);
        result.setServiceFee(serviceFee);
        result.setRefundStatus("PROCESSING");
        result.setRefundStatusText("处理中");
        result.setApplyTime(new Date());
        result.setTicketInfos(ticketInfos);
        return result;
    }

    /**
     * 退款成功完成结果
     */
    public static RefundResult completed(String refundNumber, String orderNumber,
                                         BigDecimal refundAmount, BigDecimal actualRefundAmount,
                                         Date completeTime, String refundChannelNo) {
        RefundResult result = new RefundResult();
        result.setSuccess(true);
        result.setMessage("退款成功");
        result.setRefundNumber(refundNumber);
        result.setOrderNumber(orderNumber);
        result.setRefundAmount(refundAmount);
        result.setActualRefundAmount(actualRefundAmount);
        result.setRefundStatus("SUCCESS");
        result.setRefundStatusText("退款成功");
        result.setApplyTime(new Date());
        result.setCompleteTime(completeTime);
        result.setProcessTime(completeTime);
        result.setRefundChannelNo(refundChannelNo);
        return result;
    }

    /**
     * 失败结果
     */
    public static RefundResult fail(String message) {
        RefundResult result = new RefundResult();
        result.setSuccess(false);
        result.setMessage(message);
        result.setRefundStatus("FAILED");
        result.setRefundStatusText("退款失败");
        return result;
    }

    /**
     * 失败结果（带错误码）
     */
    public static RefundResult fail(String errorCode, String message, String detail) {
        RefundResult result = fail(message);
        result.setErrorCode(errorCode);
        result.setErrorDetail(detail);
        return result;
    }

    /**
     * 失败结果（从ErrorCode）
     */
    public static RefundResult fail(ErrorCode errorCode) {
        return fail(String.valueOf(errorCode.getCode()), errorCode.getMessage(), "");
    }

    /**
     * 退款已受理结果
     */
    public static RefundResult accepted(String refundNumber, String orderNumber,
                                        BigDecimal refundAmount, List<RefundTicketInfo> ticketInfos) {
        RefundResult result = new RefundResult();
        result.setSuccess(true);
        result.setMessage("退款申请已受理");
        result.setRefundNumber(refundNumber);
        result.setOrderNumber(orderNumber);
        result.setRefundAmount(refundAmount);
        result.setRefundStatus("ACCEPTED");
        result.setRefundStatusText("已受理");
        result.setApplyTime(new Date());
        result.setTicketInfos(ticketInfos);
        return result;
    }

    // ==================== 业务方法 ====================

    /**
     * 检查是否已退款完成
     */
    public boolean isCompleted() {
        return "SUCCESS".equals(refundStatus);
    }

    /**
     * 检查是否处理中
     */
    public boolean isProcessing() {
        return "PROCESSING".equals(refundStatus) || "ACCEPTED".equals(refundStatus);
    }

    /**
     * 检查是否失败
     */
    public boolean isFailed() {
        return "FAILED".equals(refundStatus);
    }

    /**
     * 获取退款进度
     */
    public String getRefundProgress() {
        switch (refundStatus) {
            case "ACCEPTED": return "已受理";
            case "PROCESSING": return "处理中";
            case "SUCCESS": return "退款成功";
            case "FAILED": return "退款失败";
            default: return "未知";
        }
    }

    /**
     * 获取退款金额显示
     */
    public String getRefundAmountDisplay() {
        if (actualRefundAmount != null) {
            return "¥" + actualRefundAmount.setScale(2, BigDecimal.ROUND_HALF_UP);
        } else if (refundAmount != null) {
            return "¥" + refundAmount.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        return "";
    }

    /**
     * 获取手续费显示
     */
    public String getServiceFeeDisplay() {
        if (serviceFee != null && serviceFee.compareTo(BigDecimal.ZERO) > 0) {
            return "¥" + serviceFee.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        return "¥0.00";
    }

    /**
     * 获取预计到账时间
     */
    public String getEstimatedArrivalTime() {
        if (completeTime != null) {
            return DateUtil.formatDateTime(completeTime);
        }

        // 根据退款方式估算
        if (applyTime != null) {
            Date estimated = DateUtil.addDays(applyTime,
                    "BANK".equals(refundMethod) ? 3 : 1);
            return DateUtil.formatDate(estimated, "yyyy-MM-dd");
        }

        return "1-3个工作日";
    }
}