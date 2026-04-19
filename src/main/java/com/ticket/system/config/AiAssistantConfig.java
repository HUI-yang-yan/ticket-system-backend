package com.ticket.system.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 助手配置
 * 支持 OpenAI GPT 和 Azure OpenAI
 */
@Configuration
public class AiAssistantConfig {

    @Value("${spring.ai.openai.base-url")
    private String openaiBaseUrl;

    @Value("${spring.ai.openai.chat.model")
    private String chatModel;

    /**
     * 配置 OpenAI ChatClient
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .build();
    }
}
