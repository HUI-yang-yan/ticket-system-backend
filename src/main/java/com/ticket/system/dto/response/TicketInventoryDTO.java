package com.ticket.system.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "票务配置响应")
public class TicketInventoryDTO {

    @Schema(description = "配置ID")
    private Long id;

    @Schema(description = "列车ID")
    private Long trainId;

    @Schema(description = "车次号")
    private String trainNumber;

    @Schema(description = "座位类型", example = "SECOND")
    private String seatType;

    @Schema(description = "座位类型中文名", example = "二等座")
    private String seatTypeName;

    @Schema(description = "总票数", example = "100")
    private Integer totalCount;

    @Schema(description = "可用票数", example = "80")
    private Integer availableCount;

    @Schema(description = "已售票数", example = "20")
    private Integer soldCount;

    @Schema(description = "票价，单位元", example = "553.50")
    private BigDecimal price;

    @Schema(description = "版本号，用于乐观锁")
    private Integer version;

    @Schema(description = "状态 0-正常 1-禁用", example = "0")
    private Integer status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updateTime;

    // 座位类型中文名称
    public String getSeatTypeName() {
        switch (seatType) {
            case "BUSINESS": return "商务座";
            case "FIRST": return "一等座";
            case "SECOND": return "二等座";
            case "SOFT_SLEEPER": return "软卧";
            case "HARD_SLEEPER": return "硬卧";
            case "SOFT_SEAT": return "软座";
            case "HARD_SEAT": return "硬座";
            default: return "其他";
        }
    }

    // 计算售出数量
    public Integer getSoldCount() {
        if (totalCount != null && availableCount != null) {
            return totalCount - availableCount;
        }
        return 0;
    }

    // 获取库存百分比
    public Double getInventoryPercent() {
        if (totalCount != null && totalCount > 0) {
            return (availableCount != null ? availableCount.doubleValue() : 0) / totalCount * 100;
        }
        return 0.0;
    }
}
