package com.ticket.system.controller;

import com.ticket.system.ai.SystemConstant;
import com.ticket.system.common.result.Result;
import com.ticket.system.dto.request.AiChatRequestDTO;
import com.ticket.system.dto.request.TicketQueryParamDTO;
import com.ticket.system.dto.response.AiChatResponseDTO;
import com.ticket.system.dto.response.ChatMessageDTO;
import com.ticket.system.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * AI 助手控制器
 * 提供智能问答、订单查询、票务咨询等功能
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "AI助手", description = "智能问答、订单查询、票务咨询、聊天会话管理")
public class AiAssistantController {

    private final ChatClient chatClient;
    private final AiChatService aiChatService;

    @Value("${spring.ai.openai.chat.model:gpt-3.5-turbo}")
    private String defaultModel;

    /**
     * 统一聊天接口（带会话管理和自动查票）
     */
    @PostMapping("/chat")
    @Operation(summary = "统一聊天接口", description = "带会话管理的AI聊天，支持自动查票和多轮对话")
    public Result<AiChatResponseDTO> chat(@RequestBody AiChatRequestDTO request) {
        log.info("AI chat request - sessionId: {}, message: {}, autoQuery: {}",
                request.getSessionId(), request.getMessage(), request.getAutoQuery());

        AiChatResponseDTO response = aiChatService.chat(request);
        return Result.success(response);
    }

    /**
     * 流式聊天接口 - SSE 响应
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式聊天接口", description = "SSE流式响应，适合前端逐字展示AI回复")
    public Flux<String> chatStream(@RequestBody AiChatRequestDTO request) {
        log.info("AI chat stream request - sessionId: {}, message: {}, autoQuery: {}",
                request.getSessionId(), request.getMessage(), request.getAutoQuery());

        return aiChatService.chatStream(request);
    }

    /**
     * 解析用户输入为结构化查询参数
     */
    @PostMapping("/parse")
    @Operation(summary = "解析用户输入", description = "将自然语言解析为结构化查询参数（出发地、目的地、日期、时间段、偏好）")
    public Result<Map<String, String>> parseQuery(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        log.info("AI parse request - message: {}", message);

        TicketQueryParamDTO params = aiChatService.parseQueryParams(message);
        return Result.success(Map.of(
                "from", params.getFrom() != null ? params.getFrom() : "",
                "to", params.getTo() != null ? params.getTo() : "",
                "date", params.getDate() != null ? params.getDate() : "",
                "timeRange", params.getTimeRange() != null ? params.getTimeRange() : "any",
                "preference", params.getPreference() != null ? params.getPreference() : "any"
        ));
    }

    /**
     * 获取聊天历史
     */
    @GetMapping("/history/{sessionId}")
    @Operation(summary = "获取聊天历史", description = "获取指定会话ID的聊天记录")
    public Result<List<ChatMessageDTO>> getHistory(@PathVariable String sessionId) {
        log.info("Get chat history - sessionId: {}", sessionId);
        List<ChatMessageDTO> history = aiChatService.getChatHistory(sessionId);
        return Result.success(history);
    }

    /**
     * 清除聊天历史
     */
    @DeleteMapping("/history/{sessionId}")
    @Operation(summary = "清除聊天历史", description = "删除指定会话ID的聊天记录")
    public Result<Void> clearHistory(@PathVariable String sessionId) {
        log.info("Clear chat history - sessionId: {}", sessionId);
        aiChatService.clearChatHistory(sessionId);
        return Result.success();
    }

    /**
     * 通用对话接口（无会话管理）
     */
    @PostMapping("/chat/legacy")
    @Operation(summary = "通用对话接口", description = "简单的AI对话，无会话管理功能")
    public Map<String, Object> chatLegacy(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String sessionId = request.getOrDefault("sessionId", "default");

        log.info("AI chat legacy - sessionId: {}, message: {}", sessionId, message);

        try {
            String response = chatClient.prompt()
                    .system(SystemConstant.AI_SYSTEM_HELPER)
                    .user(message)
                    .call()
                    .content();

            if (response != null) {
                return Map.of(
                        "success", true,
                        "response", response,
                        "sessionId", sessionId
                );
            }
        } catch (Exception e) {
            log.error("AI chat error", e);
            return Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "sessionId", sessionId
            );
        }
        return Map.of();
    }

    /**
     * 票务咨询接口
     */
    @PostMapping("/ticket/consult")
    @Operation(summary = "票务咨询", description = "针对特定车次和日期的票务问题解答")
    public Map<String, Object> ticketConsult(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        String departure = request.get("departure");
        String destination = request.get("destination");
        String date = request.get("date");

        log.info("AI ticket consult - departure: {}, destination: {}, date: {}",
                departure, destination, date);

        String prompt = String.format(
                "你是一个12306火车票务助手。用户想要从%s出发到%s，出发日期为%s。用户的问题是：%s。请根据你的知识提供帮助。",
                departure, destination, date, question
        );

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (response != null) {
                return Map.of(
                        "success", true,
                        "response", response
                );
            }
        } catch (Exception e) {
            log.error("AI ticket consult error", e);
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
        return Map.of();
    }

    /**
     * 订单咨询接口
     */
    @PostMapping("/order/consult")
    @Operation(summary = "订单咨询", description = "针对特定订单的咨询问题解答")
    public Map<String, Object> orderConsult(@RequestBody Map<String, String> request) {
        String orderNumber = request.get("orderNumber");
        String question = request.get("question");

        log.info("AI order consult - orderNumber: {}", orderNumber);

        String prompt = String.format(
                "你是一个12306订单客服。用户查询订单号%s，问题是：%s。请根据你的知识提供帮助。",
                orderNumber, question
        );

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return Map.of(
                    "success", true,
                    "response", response
            );
        } catch (Exception e) {
            log.error("AI order consult error", e);
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "AI助手服务健康状态检查")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "AI Assistant",
                "model", defaultModel
        );
    }
}
