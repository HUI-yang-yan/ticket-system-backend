package com.ticket.system.dto.request;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Data
public class RefundRequest {

    @NotBlank(message = "订单号不能为空")
    private String orderNumber;              // 订单号

    @NotNull(message = "退票原因不能为空")
    private String refundReason;            // 退票原因

    private String refundReasonCode;        // 退票原因代码
    private String refundReasonDetail;      // 退票原因详情

    // 退票车票列表（部分退票）
    @NotEmpty(message = "请选择要退的车票")
    private List<String> ticketNumbers;     // 车票号码列表

    // 退款方式
    @NotBlank(message = "退款方式不能为空")
    private String refundMethod = "ORIGINAL"; // ORIGINAL:原路退回, BANK:银行卡

    // 银行卡退款信息（当refundMethod=BANK时必填）
    private BankRefundInfo bankRefundInfo;

    // 联系人信息
    private String contactName;
    private String contactPhone;
    private String contactEmail;

    // 紧急联系方式
    private String emergencyContact;
    private String emergencyPhone;

    // 备注
    private String remark;

    // 验证方法
    public boolean isValid() {
        if (ticketNumbers == null || ticketNumbers.isEmpty()) {
            return false;
        }

        if ("BANK".equals(refundMethod) && bankRefundInfo == null) {
            return false;
        }

        return true;
    }

    @Data
    public static class BankRefundInfo {
        @NotBlank(message = "开户行不能为空")
        private String bankName;

        @NotBlank(message = "银行卡号不能为空")
        @Pattern(regexp = "\\d{16,19}", message = "银行卡号格式不正确")
        private String bankCardNo;

        @NotBlank(message = "持卡人姓名不能为空")
        private String cardHolderName;

        @Pattern(regexp = "\\d{11}", message = "手机号格式不正确")
        private String cardHolderPhone;

        private String branchName;          // 支行名称
    }

    // 退票原因枚举
    public enum RefundReason {
        CHANGE_PLAN("行程变更", "RC001"),
        SICK("生病", "RC002"),
        EMERGENCY("急事", "RC003"),
        TRAIN_CHANGE("车次变更", "RC004"),
        DUPLICATE_ORDER("重复购票", "RC005"),
        TIME_CONFLICT("时间冲突", "RC006"),
        OTHER("其他原因", "RC099");

        private final String name;
        private final String code;

        RefundReason(String name, String code) {
            this.name = name;
            this.code = code;
        }

        public String getName() { return name; }
        public String getCode() { return code; }
    }
}