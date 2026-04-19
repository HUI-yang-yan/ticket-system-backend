package com.ticket.system.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单条聊天消息
 */
@Data
public class ChatMessageDTO {
    /**
     * 角色: user 或 assistant
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;
}
