package com.ticket.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Schema(description = "票务配置创建/更新请求")
public class TicketInventoryCreateDTO {

    @NotNull(message = "列车ID不能为空")
    @Schema(description = "列车ID", example = "1")
    private Long trainId;

    @NotBlank(message = "座位类型不能为空")
    @Schema(description = "座位类型", example = "SECOND", allowableValues = {"BUSINESS", "FIRST", "SECOND", "SOFT_SLEEPER", "HARD_SLEEPER"})
    private String seatType;

    @NotNull(message = "总票数不能为空")
    @Min(value = 0, message = "总票数不能为负数")
    @Schema(description = "总票数", example = "100")
    private Integer totalCount;

    @NotNull(message = "可用票数不能为空")
    @Min(value = 0, message = "可用票数不能为负数")
    @Schema(description = "可用票数", example = "80")
    private Integer availableCount;

    @Schema(description = "票价，单位元", example = "553.50")
    private BigDecimal price;

    @Schema(description = "状态 0-正常 1-禁用", example = "0")
    private Integer status;
}
