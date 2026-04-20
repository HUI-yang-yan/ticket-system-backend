package com.ticket.system.service;

import com.ticket.system.dto.request.AiChatRequestDTO;
import com.ticket.system.dto.request.TicketQueryParamDTO;
import com.ticket.system.dto.response.AiChatResponseDTO;
import com.ticket.system.dto.response.ChatMessageDTO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI 聊天服务接口
 */
public interface AiChatService {

    /**
     * 统一聊天入口 - 处理会话管理、AI解析、车票查询
     */
    AiChatResponseDTO chat(AiChatRequestDTO request);

    /**
     * 流式聊天 - 返回 AI 响应的流
     */
    Flux<String> chatStream(AiChatRequestDTO request);

    /**
     * 解析用户消息为结构化查询参数
     */
    TicketQueryParamDTO parseQueryParams(String userMessage);

    /**
     * 获取会话聊天历史
     */
    List<ChatMessageDTO> getChatHistory(String sessionId);

    /**
     * 清除会话聊天历史
     */
    void clearChatHistory(String sessionId);
}
