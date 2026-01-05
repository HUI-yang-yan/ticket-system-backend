package com.ticket.system.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.Date;

@Data
public class OrderCreateDTO {

    @NotNull(message = "列车ID不能为空")
    private Long trainId;

    @NotNull(message = "出发站ID不能为空")
    private Long departureStationId;

    @NotNull(message = "到达站ID不能为空")
    private Long arrivalStationId;

    @NotNull(message = "出发日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private LocalDate departureDate;

    @NotNull(message = "座位类型不能为空")
    private String seatType;

    @NotBlank(message = "乘客身份证不能为空")
    @Pattern(regexp = "\\d{17}[\\d|x|X]|\\d{15}", message = "身份证格式不正确")
    private String passengerIdCard;

    @NotBlank(message = "乘客姓名不能为空")
    private String passengerRealName;

    private Integer ticketCount = 1; // 购票数量，默认1张
}