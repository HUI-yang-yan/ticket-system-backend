package com.ticket.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "票务配置查询条件")
public class TicketInventoryQueryDTO {

    @Schema(description = "列车ID", example = "1")
    private Long trainId;

    @Schema(description = "座位类型", example = "SECOND")
    private String seatType;

    @Schema(description = "状态 0-正常 1-禁用", example = "0")
    private Integer status;
}
