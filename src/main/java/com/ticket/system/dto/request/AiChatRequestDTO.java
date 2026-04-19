package com.ticket.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 聊天请求 DTO
 */
@Data
public class AiChatRequestDTO {

    @NotBlank(message = "消息内容不能为空")
    private String message;

    /**
     * 会话ID，不传则自动生成
     */
    private String sessionId;

    /**
     * 是否自动查询车票，默认 true
     */
    private Boolean autoQuery = true;
}
