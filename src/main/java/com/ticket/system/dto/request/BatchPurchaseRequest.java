package com.ticket.system.dto.request;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class BatchPurchaseRequest {

    @NotNull(message = "列车信息不能为空")
    private TrainPurchaseInfo trainInfo;

    @Valid
    @NotNull(message = "乘客列表不能为空")
    @Size(min = 1, max = 5, message = "每次最多购买5张票")
    private List<PassengerInfo> passengers;

    @NotNull(message = "联系人信息不能为空")
    private ContactInfo contactInfo;

    @NotNull(message = "支付信息不能为空")
    private PaymentInfo paymentInfo;

    @Data
    public static class TrainPurchaseInfo {
        @NotNull(message = "列车ID不能为空")
        private Long trainId;

        @NotNull(message = "出发站ID不能为空")
        private Long departureStationId;

        @NotNull(message = "到达站ID不能为空")
        private Long arrivalStationId;

        @NotNull(message = "出发日期不能为空")
        private String departureDate;

        @NotBlank(message = "座位类型不能为空")
        private String seatType;
    }

    @Data
    public static class PassengerInfo {
        @NotBlank(message = "乘客姓名不能为空")
        private String realName;

        @NotBlank(message = "身份证号不能为空")
        private String idCard;

        private String phone;

        // 座位偏好
        private String seatPreference;

        // 是否购买保险
        private Boolean purchaseInsurance = false;
    }

    @Data
    public static class ContactInfo {
        @NotBlank(message = "联系人姓名不能为空")
        private String name;

        @NotBlank(message = "联系人手机号不能为空")
        private String phone;

        private String email;
    }

    @Data
    public static class PaymentInfo {
        @NotBlank(message = "支付方式不能为空")
        private String method;

        private String channel;

        private Boolean autoPay = true;
    }

    // 获取购票数量
    public int getTicketCount() {
        return passengers != null ? passengers.size() : 0;
    }
}