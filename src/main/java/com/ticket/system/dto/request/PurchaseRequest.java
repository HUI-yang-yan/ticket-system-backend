package com.ticket.system.dto.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class PurchaseRequest {

    @NotNull(message = "列车ID不能为空")
    private Long trainId;

    @NotNull(message = "出发站ID不能为空")
    private Long departureStationId;

    @NotNull(message = "到达站ID不能为空")
    private Long arrivalStationId;

    @NotNull(message = "出发日期不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date departureDate;

    @NotBlank(message = "座位类型不能为空")
    private String seatType;

    @Min(value = 1, message = "购票数量至少1张")
    @Max(value = 5, message = "最多购买5张票")
    private Integer ticketCount = 1;

    // 乘客信息（单张票）
    @NotBlank(message = "乘客姓名不能为空")
    @Size(min = 2, max = 10, message = "姓名长度2-10个字符")
    private String passengerRealName;

    @NotBlank(message = "乘客身份证不能为空")
    @Pattern(regexp = "\\d{17}[\\d|x|X]|\\d{15}", message = "身份证格式不正确")
    private String passengerIdCard;

    // 联系人信息
    private String contactName;
    private String contactPhone;
    private String contactEmail;

    // 支付方式
    @NotBlank(message = "支付方式不能为空")
    private String paymentMethod; // ALIPAY, WECHAT, BANK, UNIONPAY

    // 是否购买保险
    private Boolean purchaseInsurance = false;

    // 座位偏好（可选）
    private String seatPreference; // WINDOW, AISLE, MIDDLE

    // 是否接受无座
    private Boolean acceptNoSeat = false;

    // 备注
    private String remark;

    // 验证方法
    public boolean isValid() {
        if (ticketCount == null || ticketCount < 1 || ticketCount > 5) {
            return false;
        }

        if (departureDate == null || departureDate.before(new Date())) {
            return false;
        }

        return true;
    }

    // 获取票价估算（前端可先计算）
    public BigDecimal getEstimatedAmount() {
        // 这里应该根据座位类型、距离等计算
        // 简化处理，返回默认值
        return new BigDecimal("100.00").multiply(new BigDecimal(ticketCount));
    }
}