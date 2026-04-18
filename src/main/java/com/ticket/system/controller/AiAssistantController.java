package com.ticket.system.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 助手控制器
 * 提供智能问答、订单查询、票务咨询等功能
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private final ChatClient chatClient;

    @Value("${spring.ai.openai.chat.model:gpt-3.5-turbo}")
    private String defaultModel;

    /**
     * 通用对话接口
     */
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String sessionId = request.getOrDefault("sessionId", "default");

        log.info("AI chat request - sessionId: {}, message: {}", sessionId, message);

        try {
            String response = chatClient.prompt()
                    .user(message)
                    .call()
                    .content();

            return Map.of(
                    "success", true,
                    "response", response,
                    "sessionId", sessionId
            );
        } catch (Exception e) {
            log.error("AI chat error", e);
            return Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "sessionId", sessionId
            );
        }
    }

    /**
     * 票务咨询接口
     */
    @PostMapping("/ticket/consult")
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

            return Map.of(
                    "success", true,
                    "response", response
            );
        } catch (Exception e) {
            log.error("AI ticket consult error", e);
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }

    /**
     * 订单咨询接口
     */
    @PostMapping("/order/consult")
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
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "AI Assistant",
                "model", defaultModel
        );
    }
}
