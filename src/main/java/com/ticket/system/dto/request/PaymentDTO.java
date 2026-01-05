package com.ticket.system.dto.request;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class PaymentDTO {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotNull(message = "支付金额不能为空")
    private BigDecimal paymentAmount;

    @NotNull(message = "支付方式不能为空")
    private String paymentMethod; // ALIPAY, WECHAT, BANK

    private String paymentChannel; // 支付渠道
    private String remark;         // 备注
}