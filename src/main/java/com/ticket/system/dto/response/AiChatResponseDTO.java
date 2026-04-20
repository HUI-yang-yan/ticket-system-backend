package com.ticket.system.dto.response;

import com.ticket.system.dto.request.TicketQueryParamDTO;
import lombok.Data;

import java.util.List;

/**
 * 聊天响应 DTO
 */
@Data
public class AiChatResponseDTO {
    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * AI 文本回复
     */
    private String message;

    /**
     * 解析出的查询参数
     */
    private TicketQueryParamDTO params;

    /**
     * 车票查询结果
     */
    private List<TicketInfoDTO> tickets;

    /**
     * 缺失参数数量
     */
    private Integer paramMissingCount;

    /**
     * 缺失参数描述
     */
    private String missingParams;

    /**
     * 本次对话的意图类型: QUERY_TICKET / KNOWLEDGE / CHAT / UNCLEAR
     */
    private String intentType;
}
