package com.ticket.system.dto.request;

import lombok.Data;

/**
 * AI 解析结果 - 结构化购票参数
 */
@Data
public class TicketQueryParamDTO {
    /**
     * 出发地城市
     */
    private String from;

    /**
     * 目的地城市
     */
    private String to;

    /**
     * 日期 YYYY-MM-DD
     */
    private String date;

    /**
     * 时间范围: morning | afternoon | evening | any
     */
    private String timeRange;

    /**
     * 偏好: fastest | cheapest | direct | any
     */
    private String preference;
}
