package com.ticket.system.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.system.ai.tools.DateParserTool;
import com.ticket.system.ai.tools.KnowledgeRetrievalTool;
import com.ticket.system.ai.tools.StationTools;
import com.ticket.system.ai.tools.TicketTools;
import com.ticket.system.ai.tools.TrainTools;
import com.ticket.system.common.constant.RedisConstant;
import com.ticket.system.ai.SystemConstant;
import com.ticket.system.dto.request.AiChatRequestDTO;
import com.ticket.system.dto.request.TicketQueryDTO;
import com.ticket.system.dto.request.TicketQueryParamDTO;
import com.ticket.system.dto.response.AiChatResponseDTO;
import com.ticket.system.dto.response.ChatMessageDTO;
import com.ticket.system.dto.response.StationInfoDTO;
import com.ticket.system.dto.response.TicketInfoDTO;
import com.ticket.system.service.AiChatService;
import com.ticket.system.service.StationService;
import com.ticket.system.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.ticket.system.ai.AiPrompt.INTENT_PROMPT;
import static com.ticket.system.ai.AiPrompt.CHAT_PROMPT;
import static com.ticket.system.ai.AiPrompt.KNOWLEDGE_PROMPT;

/**
 * AI 聊天服务实现
 * 支持 Spring AI Function Calling / Tool 机制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final ChatClient chatClient;
    private final RedissonClient redissonClient;
    private final StationService stationService;
    private final TicketService ticketService;
    private final ObjectMapper objectMapper;

    /**
     * AI 工具（用于 Function Calling）
     */
    private final StationTools stationTools;
    private final TrainTools trainTools;
    private final TicketTools ticketTools;
    private final DateParserTool dateParserTool;
    private final KnowledgeRetrievalTool knowledgeRetrievalTool;

    /**
     * 会话过期时间（秒）- 30分钟
     */
    private static final long SESSION_EXPIRE_SECONDS = 30 * 60L;

    @Override
    public Flux<String> chatStream(AiChatRequestDTO request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        try {
            // 获取历史记录
            List<ChatMessageDTO> history = getChatHistory(sessionId);

            // 添加用户消息到历史
            ChatMessageDTO userMsg = new ChatMessageDTO();
            userMsg.setRole("user");
            userMsg.setContent(request.getMessage());
            userMsg.setTimestamp(LocalDateTime.now());
            history.add(userMsg);

            // 先识别用户意图（这个可以快速阻塞获取）
            IntentResult intentResult = recognizeIntent(history);
            UserIntent intent = intentResult.intent;

            // 根据意图构建不同的流
            Flux<String> responseFlux;

            switch (intent) {
                case QUERY_TICKET -> {
                    TicketQueryParamDTO params = sendToAIAndExtractParams(history);
                    int missingCount = countMissingParams(params);
                    if (Boolean.TRUE.equals(request.getAutoQuery()) && missingCount == 0) {
                        List<TicketInfoDTO> tickets = queryTicketsWithParams(params);
                        responseFlux = generateQueryResponseStream(params, tickets, missingCount);
                    } else {
                        responseFlux = generateQueryResponseStream(params, null, missingCount);
                    }
                }
                case KNOWLEDGE -> responseFlux = generateKnowledgeResponseStream(request.getMessage(), history);
                case CHAT -> responseFlux = generateChatResponseStream(request.getMessage(), history);
                default -> responseFlux = generateUnclearResponseStream(request.getMessage());
            }

            // 保存用户消息到历史（异步，不阻塞流）
            saveChatHistory(sessionId, history);

            return responseFlux;

        } catch (Exception e) {
            log.error("AI chat stream error", e);
            return Flux.just("处理失败: " + e.getMessage());
        }
    }

    /**
     * 生成查票响应（流式）
     */
    private Flux<String> generateQueryResponseStream(TicketQueryParamDTO params, List<TicketInfoDTO> tickets, int missingCount) {
        if (missingCount > 0) {
            String prompt = String.format(
                    "你是小铁，12306票务助手。用户想查票但没有提供完整信息：缺少 %s。用友好自然的语气问一下。",
                    getMissingParamsDesc(params));
            try {
                return chatClient.prompt()
                        .system(prompt)
                        .user(params.getFrom() != null ? "用户说想去" + params.getFrom() : "用户还没说清楚去哪里")
                        .stream()
                        .content();
            } catch (Exception e) {
                log.error("generateQueryResponseStream prompt error", e);
                return Flux.just("帮您查询前，还需要了解一下：您的" + getMissingParamsDesc(params) + "是？");
            }
        }

        if (tickets == null || tickets.isEmpty()) {
            return Flux.just("暂未查询到符合条件的车票，您可以尝试更换出发日期或目的地，或者提交候补申请～");
        }

        return Flux.just("已为您查询到 " + tickets.size() + " 个车次，请查看下方列表。如需进一步筛选（最快/最便宜），告诉我就好！");
    }

    /**
     * 生成知识问答响应（流式）
     */
    private Flux<String> generateKnowledgeResponseStream(String userMessage, List<ChatMessageDTO> history) {
        String currentDateInfo = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
        String systemPrompt = KNOWLEDGE_PROMPT.replace("{currentDate}", currentDateInfo);

        List<Message> recentMessages = convertToSpringAiMessages(
                history.subList(Math.max(0, history.size() - 8), history.size() - 1)
        );

        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .messages(recentMessages)
                    .user(userMessage)
                    .tools(knowledgeRetrievalTool)
                    .stream()
                    .content();
        } catch (Exception e) {
            log.error("generateKnowledgeResponseStream error", e);
            return Flux.just("抱歉，我现在有点忙，稍后再问我吧～如果有紧急问题，可以拨打12306客服热线。");
        }
    }

    /**
     * 生成闲聊响应（流式）
     */
    private Flux<String> generateChatResponseStream(String userMessage, List<ChatMessageDTO> history) {
        String currentDateInfo = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
        String systemPrompt = CHAT_PROMPT.replace("{currentDate}", currentDateInfo);

        List<Message> recentMessages = convertToSpringAiMessages(
                history.subList(Math.max(0, history.size() - 8), history.size() - 1)
        );

        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .messages(recentMessages)
                    .user(userMessage)
                    .stream()
                    .content();
        } catch (Exception e) {
            log.error("generateChatResponseStream error", e);
            return Flux.just("你好！我是小铁，你的12306票务助手，有什么可以帮你的？");
        }
    }

    /**
     * 生成意图不明确响应（流式）
     */
    private Flux<String> generateUnclearResponseStream(String userMessage) {
        String prompt = """
                你是小铁，12306票务助手。用户说了一句不太明确的话。
                请友好地引导用户说清楚想要什么，比如"您是想查票呢，还是有其他问题？"
                """;
        try {
            return chatClient.prompt()
                    .system(prompt)
                    .user(userMessage)
                    .stream()
                    .content();
        } catch (Exception e) {
            log.error("generateUnclearResponseStream error", e);
            return Flux.just("您好！我是小铁，12306票务助手。我可以帮您查询车票、了解退票改签规则、候补购票等。请问有什么可以帮您的？");
        }
    }

    @Override
    public AiChatResponseDTO chat(AiChatRequestDTO request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        AiChatResponseDTO response = new AiChatResponseDTO();
        response.setSessionId(sessionId);

        try {
            // 1. 获取或创建会话
            RMap<String, Object> sessionMap = getSessionMap(sessionId);

            // 2. 获取历史记录
            List<ChatMessageDTO> history = getChatHistory(sessionId);

            // 3. 构建用户消息
            ChatMessageDTO userMsg = new ChatMessageDTO();
            userMsg.setRole("user");
            userMsg.setContent(request.getMessage());
            userMsg.setTimestamp(LocalDateTime.now());
            history.add(userMsg);

            // 4. 先识别用户意图（传入历史以便上下文关联）
            IntentResult intentResult = recognizeIntent(history);
            UserIntent intent = intentResult.intent;

            // 5. 根据意图类型分别处理
            String aiTextResponse;
            TicketQueryParamDTO params = new TicketQueryParamDTO();
            List<TicketInfoDTO> tickets = Collections.emptyList();

            switch (intent) {
                case QUERY_TICKET -> {
                    response.setIntentType("QUERY_TICKET");
                    params = sendToAIAndExtractParams(history);
                    int missingCount = countMissingParams(params);
                    response.setParamMissingCount(missingCount);
                    response.setMissingParams(getMissingParamsDesc(params));

                    if (Boolean.TRUE.equals(request.getAutoQuery()) && missingCount == 0) {
                        tickets = queryTicketsWithParams(params);
                    }
                    response.setTickets(tickets);
                    aiTextResponse = generateQueryResponse(params, tickets, missingCount);
                }
                case KNOWLEDGE -> {
                    response.setIntentType("KNOWLEDGE");
                    aiTextResponse = generateKnowledgeResponse(request.getMessage(), history);
                }
                case CHAT -> {
                    response.setIntentType("CHAT");
                    aiTextResponse = generateChatResponse(request.getMessage(), history);
                }
                default -> {
                    response.setIntentType("UNCLEAR");
                    aiTextResponse = generateUnclearResponse(request.getMessage());
                }
            }

            // 6. 添加 AI 回复到历史
            ChatMessageDTO assistantMsg = new ChatMessageDTO();
            assistantMsg.setRole("assistant");
            assistantMsg.setContent(aiTextResponse);
            assistantMsg.setTimestamp(LocalDateTime.now());
            history.add(assistantMsg);

            // 7. 保存更新后的历史到 Redis
            saveChatHistory(sessionId, history);

            // 8. 设置解析出的查询参数
            response.setParams(params);

            // 9. 设置成功响应
            response.setSuccess(true);
            response.setMessage(aiTextResponse);

            return response;

        } catch (Exception e) {
            log.error("AI chat error, sessionId: {}", sessionId, e);
            response.setSuccess(false);
            response.setMessage("处理失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 生成查票响应
     */
    private String generateQueryResponse(TicketQueryParamDTO params, List<TicketInfoDTO> tickets, int missingCount) {
        if (missingCount > 0) {
            // 参数不完整，让 AI 友好地引导
            String prompt = String.format(
                    "你是小铁，12306票务助手。用户想查票但没有提供完整信息：缺少 %s。用友好自然的语气问一下。",
                    getMissingParamsDesc(params));
            try {
                return chatClient.prompt()
                        .system(prompt)
                        .user(params.getFrom() != null ? "用户说想去" + params.getFrom() : "用户还没说清楚去哪里")
                        .call()
                        .content();
            } catch (Exception e) {
                log.error("generateQueryResponse prompt error", e);
                return "帮您查询前，还需要了解一下：您的" + getMissingParamsDesc(params) + "是？";
            }
        }

        if (tickets == null || tickets.isEmpty()) {
            return "暂未查询到符合条件的车票，您可以尝试更换出发日期或目的地，或者提交候补申请～";
        }

        return "已为您查询到 " + tickets.size() + " 个车次，请查看下方列表。如需进一步筛选（最快/最便宜），告诉我就好！";
    }

    /**
     * 生成知识问答响应
     */
    private String generateKnowledgeResponse(String userMessage, List<ChatMessageDTO> history) {
        String currentDateInfo = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
        String systemPrompt = KNOWLEDGE_PROMPT.replace("{currentDate}", currentDateInfo);

        // 获取最近 8 条消息（4 轮对话）的上下文
        List<Message> recentMessages = convertToSpringAiMessages(
                history.subList(Math.max(0, history.size() - 8), history.size() - 1)
        );

        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .messages(recentMessages)
                    .user(userMessage)
                    .tools(knowledgeRetrievalTool)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("generateKnowledgeResponse error", e);
            return "抱歉，我现在有点忙，稍后再问我吧～如果有紧急问题，可以拨打12306客服热线。";
        }
    }

    /**
     * 生成闲聊响应
     */
    private String generateChatResponse(String userMessage, List<ChatMessageDTO> history) {
        String currentDateInfo = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
        String systemPrompt = CHAT_PROMPT.replace("{currentDate}", currentDateInfo);

        // 获取最近 8 条消息（4 轮对话）的上下文
        List<Message> recentMessages = convertToSpringAiMessages(
                history.subList(Math.max(0, history.size() - 8), history.size() - 1)
        );

        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .messages(recentMessages)
                    .user(userMessage)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("generateChatResponse error", e);
            return "你好！我是小铁，你的12306票务助手，有什么可以帮你的？";
        }
    }

    /**
     * 生成意图不明确响应
     */
    private String generateUnclearResponse(String userMessage) {
        String prompt = """
                你是小铁，12306票务助手。用户说了一句不太明确的话。
                请友好地引导用户说清楚想要什么，比如"您是想查票呢，还是有其他问题？"
                """;
        try {
            return chatClient.prompt()
                    .system(prompt)
                    .user(userMessage)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("generateUnclearResponse error", e);
            return "您好！我是小铁，12306票务助手。我可以帮您查询车票、了解退票改签规则、候补购票等。请问有什么可以帮您的？";
        }
    }

    @Override
    public TicketQueryParamDTO parseQueryParams(String userMessage) {
        try {
            return chatClient.prompt()
                    .system(SystemConstant.AI_SYSTEM_HELPER + "\n\n你是一个参数提取助手，直接从用户输入中提取票务查询参数并返回JSON，不要有其他内容。返回格式：{\"from\":\"出发城市\",\"to\":\"到达城市\",\"date\":\"YYYY-MM-DD\",\"timeRange\":\"any\",\"preference\":\"any\"}，如果参数不确定则设为null。")
                    .user(userMessage)
                    .call()
                    .entity(TicketQueryParamDTO.class);
        } catch (Exception e) {
            log.error("Parse query params error", e);
            return new TicketQueryParamDTO();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ChatMessageDTO> getChatHistory(String sessionId) {
        try {
            RMap<String, Object> sessionMap = getSessionMap(sessionId);
            Object messagesObj = sessionMap.get("messages");
            if (messagesObj == null) {
                return new ArrayList<>();
            }
            String jsonStr = messagesObj.toString();
            return objectMapper.readValue(jsonStr,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ChatMessageDTO.class));
        } catch (Exception e) {
            log.error("Get chat history error, sessionId: {}", sessionId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void clearChatHistory(String sessionId) {
        try {
            String key = RedisConstant.AI_CHAT_SESSION_PREFIX + sessionId;
            redissonClient.getMap(key).clear();
            log.info("Cleared chat history, sessionId: {}", sessionId);
        } catch (Exception e) {
            log.error("Clear chat history error, sessionId: {}", sessionId, e);
        }
    }

    /**
     * 获取会话 RMap
     */
    private RMap<String, Object> getSessionMap(String sessionId) {
        String key = RedisConstant.AI_CHAT_SESSION_PREFIX + sessionId;
        RMap<String, Object> map = redissonClient.getMap(key);
        map.expire(SESSION_EXPIRE_SECONDS, TimeUnit.SECONDS);
        return map;
    }

    /**
     * 保存聊天历史
     */
    private void saveChatHistory(String sessionId, List<ChatMessageDTO> history) {
        try {
            RMap<String, Object> sessionMap = getSessionMap(sessionId);
            sessionMap.put("messages", objectMapper.writeValueAsString(history));
            sessionMap.put("lastAccessAt", LocalDateTime.now().toString());
        } catch (JsonProcessingException e) {
            log.error("Save chat history error, sessionId: {}", sessionId, e);
        }
    }

    /**
     * 用户意图枚举
     */
    private enum UserIntent {
        /** 需要查询车票 */
        QUERY_TICKET,
        /** 票务知识问答 */
        KNOWLEDGE,
        /** 闲聊/问候/其他 */
        CHAT,
        /** 意图不明确 */
        UNCLEAR
    }

    /**
     * 意图识别结果
     */
    private static class IntentResult {
        UserIntent intent;
        String reason;

        IntentResult(UserIntent intent, String reason) {
            this.intent = intent;
            this.reason = reason;
        }
    }

    /**
     * 第一步：识别用户意图（基于历史上下文）
     */
    private IntentResult recognizeIntent(List<ChatMessageDTO> history) {
        // 构建历史上下文
        StringBuilder contextBuilder = new StringBuilder();
        // 只取最近 6 条历史（3 轮对话）
        int start = Math.max(0, history.size() - 6);
        for (int i = start; i < history.size(); i++) {
            ChatMessageDTO msg = history.get(i);
            contextBuilder.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }
        String historyContext = contextBuilder.toString();

        try {
            String result = chatClient.prompt()
                    .system(INTENT_PROMPT)
                    .user("【历史上下文】\n" + historyContext + "\n【当前消息】" + history.get(history.size() - 1).getContent())
                    .call()
                    .content();

            if (result != null && result.contains("QUERY_TICKET")) {
                return new IntentResult(UserIntent.QUERY_TICKET, "用户明确表示要查询车票");
            } else if (result != null && result.contains("KNOWLEDGE")) {
                return new IntentResult(UserIntent.KNOWLEDGE, "用户想了解票务知识");
            } else if (result != null && result.contains("CHAT")) {
                return new IntentResult(UserIntent.CHAT, "用户在进行闲聊或问候");
            } else {
                return new IntentResult(UserIntent.UNCLEAR, "用户意图不明确");
            }
        } catch (Exception e) {
            log.error("recognizeIntent error", e);
            return new IntentResult(UserIntent.UNCLEAR, "意图识别失败，默认走查询流程");
        }
    }

    /**
     * 发送消息给 AI（使用 Tool Calling 模式）
     * AI 会自动调用注入的工具来获取票务信息
     * 返回结构化 JSON 格式的响应
     * 注意：此方法只解析参数，不生成展示给用户的文本
     */
    private TicketQueryParamDTO sendToAIAndExtractParams(List<ChatMessageDTO> history) {
        // 只使用最后一条用户消息作为输入，避免历史消息干扰
        String lastUserMessage = "";
        for (int i = history.size() - 1; i >= 0; i--) {
            if ("user".equals(history.get(i).getRole())) {
                lastUserMessage = history.get(i).getContent();
                break;
            }
        }

        // 获取当前日期信息，用于 AI 自动推理相对日期
        LocalDate today = LocalDate.now();
        String currentDateInfo = String.format("当前日期：%s（%s）",
                today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                today.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.CHINA));

        // 构建最近 8 条消息的上下文
        StringBuilder contextBuilder = new StringBuilder();
        int start = Math.max(0, history.size() - 8);
        for (int i = start; i < history.size(); i++) {
            ChatMessageDTO msg = history.get(i);
            contextBuilder.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }
        String recentContext = contextBuilder.toString();

        // 构建简洁的系统提示，只关注参数提取，不生成示例对话
        // 注意：JSON示例中的大括号需要转义，避免被StringTemplate解析为模板变量
        String jsonExample = "\\{\"from\":\"城市\",\"to\":\"城市\",\"date\":\"YYYY-MM-DD\",\"timeRange\":\"any\",\"preference\":\"any\"\\}";
        String systemPrompt = """
            你是一个12306票务助手的参数提取专家。

            你的任务是从用户消息中提取票务查询参数，只需返回JSON格式的参数，不要生成任何解释性文本。

            【提取规则】
            - from: 出发城市（中文，如"北京"、"上海"）
            - to: 到达城市
            - date: 出发日期（YYYY-MM-DD格式，如"2026-04-19"）
            - timeRange: 时间偏好（morning/afternoon/evening/any），用户没明确说就设为null
            - preference: 排序偏好（fastest/cheapest/direct/any），用户没明确说就设为null

            【关键规则 - 日期必须通过工具获取】
            当用户提到"今天"、"明天"、"后天"、"大后天"、"下周三"、"4月20日"、"周一"等任何相对日期或模糊日期时，
            必须先调用 parse_date 工具将日期转换为 YYYY-MM-DD 格式，工具返回的才是正确的日期。
            不要自己计算日期，必须依赖 parse_date 工具的结果。

            【重要】
            - 只返回JSON，不要任何其他文字
            - 如果参数不确定，设为 null（不要用 "any" 或其他占位符）
            - 参数必须是完整的查询条件

            返回格式示例：
            """ + jsonExample;

        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(currentDateInfo + "\n\n【最近对话上下文】\n" + recentContext + "\n【当前消息】" + history.get(history.size() - 1).getContent())
                    .tools(stationTools, trainTools, ticketTools, dateParserTool)
                    .call()
                    .entity(TicketQueryParamDTO.class);
        } catch (Exception e) {
            log.error("sendToAIAndExtractParams error", e);
            return new TicketQueryParamDTO();
        }
    }

    /**
     * 将内部 ChatMessageDTO 列表转换为 Spring AI 的 Message 列表
     */
    private List<Message> convertToSpringAiMessages(List<ChatMessageDTO> history) {
        List<Message> messages = new ArrayList<>();
        for (ChatMessageDTO msg : history) {
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
            // 忽略其他角色类型
        }
        return messages;
    }

    
    /**
     * 使用参数查询车票
     */
    private List<TicketInfoDTO> queryTicketsWithParams(TicketQueryParamDTO params) {
        if (params.getFrom() == null || params.getTo() == null || params.getDate() == null) {
            return Collections.emptyList();
        }

        try {
            // 解析城市为车站
            StationInfoDTO fromStation = resolveMainStation(params.getFrom());
            StationInfoDTO toStation = resolveMainStation(params.getTo());

            if (fromStation == null || toStation == null) {
                log.warn("Station not found, from: {}, to: {}", params.getFrom(), params.getTo());
                return Collections.emptyList();
            }

            // 构建查询 DTO
            TicketQueryDTO queryDTO = new TicketQueryDTO();
            queryDTO.setDepartureStationId(fromStation.getId());
            queryDTO.setArrivalStationId(toStation.getId());
            queryDTO.setDepartureDate(LocalDate.parse(params.getDate()));

            // 执行查询
            List<TicketInfoDTO> tickets = ticketService.queryTickets(queryDTO);

            // 应用偏好排序
            return applyPreferenceSorting(tickets, params.getPreference());

        } catch (Exception e) {
            log.error("Query tickets error", e);
            return Collections.emptyList();
        }
    }

    /**
     * 解析城市名称为主车站
     * 用户输入"北京"，实际站点可能是"北京南"、"北京站"等
     * 策略：优先选择不带后缀的主站
     */
    private StationInfoDTO resolveMainStation(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return null;
        }

        try {
            List<StationInfoDTO> stations = stationService.getStationsByCity(cityName);
            if (stations == null || stations.isEmpty()) {
                // 尝试模糊搜索
                stations = stationService.searchStations(cityName);
            }

            if (stations == null || stations.isEmpty()) {
                return null;
            }

            // 优先选择城市名完全匹配的站点
            for (StationInfoDTO station : stations) {
                if (station.getCity() != null &&
                        station.getCity().replace("市", "").equals(cityName.replace("市", ""))) {
                    // 如果有精确匹配市名的，返回
                    if (station.getStationName().equals(cityName) ||
                            station.getStationName().equals(cityName + "站")) {
                        return station;
                    }
                }
            }

            // 否则选择最短名称的站点（主站通常名称最短）
            return stations.stream()
                    .min(Comparator.comparingInt(s -> s.getStationName().length()))
                    .orElse(stations.get(0));

        } catch (Exception e) {
            log.error("Resolve main station error, city: {}", cityName, e);
            return null;
        }
    }

    /**
     * 应用偏好排序
     */
    private List<TicketInfoDTO> applyPreferenceSorting(List<TicketInfoDTO> tickets, String preference) {
        if (tickets == null || tickets.isEmpty() || preference == null || "any".equals(preference)) {
            return tickets;
        }

        return switch (preference) {
            case "fastest" -> tickets.stream()
                    .sorted(Comparator.comparing(TicketInfoDTO::getDepartureTime,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());
            case "cheapest" -> tickets.stream()
                    .sorted(Comparator.comparing(TicketInfoDTO::getPrice,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());
            case "direct" -> tickets.stream()
                    .filter(t -> t.getTrainType() != null &&
                            (t.getTrainType().startsWith("G") ||  // 高铁
                                    t.getTrainType().startsWith("D") ||  // 动车
                                    t.getTrainType().startsWith("C")))  // 城际
                    .collect(Collectors.toList());
            default -> tickets;
        };
    }

    /**
     * 计算缺失参数数量
     */
    private int countMissingParams(TicketQueryParamDTO params) {
        int count = 0;
        if (params.getFrom() == null) count++;
        if (params.getTo() == null) count++;
        if (params.getDate() == null) count++;
        return count;
    }

    /**
     * 获取缺失参数描述
     */
    private String getMissingParamsDesc(TicketQueryParamDTO params) {
        List<String> missing = new ArrayList<>();
        if (params.getFrom() == null) missing.add("出发地");
        if (params.getTo() == null) missing.add("目的地");
        if (params.getDate() == null) missing.add("日期");
        return missing.isEmpty() ? "无" : String.join("、", missing);
    }
}
