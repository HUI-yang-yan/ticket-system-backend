package com.ticket.system.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class WaitlistCreateDTO {

    @NotNull(message = "列车ID不能为空")
    private Long trainId;

    @NotNull(message = "出发站ID不能为空")
    private Long startStationId;

    @NotNull(message = "到达站ID不能为空")
    private Long endStationId;

    @NotNull(message = "期望乘车日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private LocalDate departureDate;

    @NotBlank(message = "座位类型不能为空")
    private String seatType;
}
