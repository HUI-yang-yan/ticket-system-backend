package com.ticket.system.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class TicketQueryDTO {

    @NotNull(message = "出发站ID不能为空")
    private Long departureStationId;

    @NotNull(message = "到达站ID不能为空")
    private Long arrivalStationId;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private LocalDate departureDate;

    private Long trainId;

    private String trainType;      // 列车类型（可选）
    private String seatType;       // 座位类型（可选）
    private Integer pageNum = 1;   // 页码
    private Integer pageSize = 20; // 每页条数
}