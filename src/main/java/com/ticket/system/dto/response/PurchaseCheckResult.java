package com.ticket.system.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class PurchaseCheckResult {

    private boolean available;           // 是否可购买
    private String message;              // 检查消息
    private String checkId;              // 检查ID（用于后续确认）

    // 车票信息
    private String trainNumber;
    private String departureStationName;
    private String arrivalStationName;
    private Date departureTime;
    private Date arrivalTime;
    private String duration;

    // 座位信息
    private String seatType;
    private String seatTypeName;
    private Integer availableSeats;      // 剩余座位数

    // 价格信息
    private BigDecimal ticketPrice;      // 单张票价
    private BigDecimal insurancePrice;   // 保险费
    private BigDecimal serviceFee;       // 服务费
    private BigDecimal totalAmount;      // 总金额

    // 时间限制
    private Date expireTime;             // 购票过期时间
    private Long remainingSeconds;       // 剩余秒数

    // 验证结果
    private List<ValidationResult> validations;

    // 座位列表（可选）
    private List<SeatOption> seatOptions;

    /**
     * 可购结果
     */
    public static PurchaseCheckResult available(String message, BigDecimal totalAmount,
                                                Date expireTime) {
        PurchaseCheckResult result = new PurchaseCheckResult();
        result.setAvailable(true);
        result.setMessage(message);
        result.setTotalAmount(totalAmount);
        result.setExpireTime(expireTime);

        if (expireTime != null) {
            long remaining = (expireTime.getTime() - System.currentTimeMillis()) / 1000;
            result.setRemainingSeconds(remaining > 0 ? remaining : 0L);
        }

        return result;
    }

    /**
     * 不可购结果
     */
    public static PurchaseCheckResult unavailable(String message) {
        PurchaseCheckResult result = new PurchaseCheckResult();
        result.setAvailable(false);
        result.setMessage(message);
        return result;
    }

    /**
     * 包含验证结果的不可购结果
     */
    public static PurchaseCheckResult unavailableWithValidations(String message,
                                                                 List<ValidationResult> validations) {
        PurchaseCheckResult result = unavailable(message);
        result.setValidations(validations);
        return result;
    }

    @Data
    public static class ValidationResult {
        private String field;           // 验证字段
        private boolean valid;          // 是否有效
        private String message;         // 验证消息
        private String suggestion;      // 建议

        public static ValidationResult valid(String field) {
            ValidationResult result = new ValidationResult();
            result.setField(field);
            result.setValid(true);
            result.setMessage("验证通过");
            return result;
        }

        public static ValidationResult invalid(String field, String message) {
            ValidationResult result = new ValidationResult();
            result.setField(field);
            result.setValid(false);
            result.setMessage(message);
            return result;
        }
    }

    @Data
    public static class SeatOption {
        private String seatId;          // 座位ID
        private String carriageNumber;  // 车厢号
        private String seatNumber;      // 座位号
        private String seatType;        // 座位类型
        private BigDecimal price;       // 价格
        private boolean available;      // 是否可用
        private String description;     // 描述
    }
}