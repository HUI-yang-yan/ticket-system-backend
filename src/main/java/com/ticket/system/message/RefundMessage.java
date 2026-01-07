package com.ticket.system.message;

import com.ticket.system.common.exception.BusinessException;
import com.ticket.system.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单ID */
    private Long orderId;

    /** 退款单号（幂等） */
    private String refundNo;

    /** 当前重试次数 */
    private int retryCount;

    /** 最大重试次数 */
    private static final int MAX_RETRY = 5;

    public RefundMessage nextRetry() {
        if (retryCount >= MAX_RETRY) {
            throw new BusinessException(ErrorCode.REFUND_RETRY_EXCEEDED.getCode(),"退款重试次数已达上限");
        }
        this.retryCount++;
        return this;
    }
}
