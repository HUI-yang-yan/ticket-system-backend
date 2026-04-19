package com.ticket.system.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.ticket.system.ai.AiPrompt.INTENT_PROMPT;

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

    /**
     * 会话过期时间（秒）- 30分钟
     */
    private static final long SESSION_EXPIRE_SECONDS = 30 * 60L;

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

            // 4. 先识别用户意图（轻量级判断，不调用 tools，节省资源）
            IntentResult intentResult = recognizeIntent(request.getMessage());
            boolean isChatIntent = intentResult.intent == UserIntent.CHAT;

            // 5. 发送消息给 AI 并获取结构化参数（使用 Tool Calling 模式）
            // 只有在非闲聊意图时才调用 tools
            TicketQueryParamDTO params = sendToAIAndExtractParams(history, isChatIntent);

            // 6. 计算缺失参数
            int missingCount = countMissingParams(params);
            response.setParamMissingCount(missingCount);
            response.setMissingParams(getMissingParamsDesc(params));

            // 7. 如果启用自动查询且参数完整，查询车票
            List<TicketInfoDTO> tickets = Collections.emptyList();
            if (!isChatIntent && Boolean.TRUE.equals(request.getAutoQuery()) && missingCount == 0) {
                tickets = queryTicketsWithParams(params);
            }
            response.setTickets(tickets);

            // 8. 获取 AI 的文本回复（根据查询结果生成）
            String aiTextResponse = generateTextResponse(params, tickets, isChatIntent);

            // 9. 添加 AI 回复到历史
            ChatMessageDTO assistantMsg = new ChatMessageDTO();
            assistantMsg.setRole("assistant");
            assistantMsg.setContent(aiTextResponse);
            assistantMsg.setTimestamp(LocalDateTime.now());
            history.add(assistantMsg);

            // 10. 保存更新后的历史到 Redis
            saveChatHistory(sessionId, history);

            // 11. 设置解析出的查询参数
            response.setParams(params);

            // 12. 设置成功响应
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
     * 根据查询结果生成简洁的 AI 文本回复
     * 只返回结果提示，不显示任何查询参数信息
     */
    private String generateTextResponse(TicketQueryParamDTO params, List<TicketInfoDTO> tickets, boolean isChatIntent) {
        // 如果是闲聊意图，返回友好问候
        if (isChatIntent) {
            return "您好！我是12306票务助手，很高兴为您服务。请问有什么可以帮您的？您可以告诉我出发地、目的地和日期，我来帮您查询车票。";
        }

        // 如果查询结果为空，返回友好提示
        if (tickets == null || tickets.isEmpty()) {
            if (countMissingParams(params) > 0) {
                return "好的，请补充您的出行信息：" + getMissingParamsDesc(params);
            }
            return "暂未查询到符合条件的车票，您可以尝试更换出发日期或目的地。";
        }

        // 有结果时，只返回简要提示，不暴露查询详情
        return "已为您查询到 " + tickets.size() + " 个车次，请查看下方结果。";
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
     * 第一步：识别用户意图（不调用 tools，节省资源）
     */
    private IntentResult recognizeIntent(String userMessage) {


        try {
            String result = chatClient.prompt()
                    .system(INTENT_PROMPT)
                    .user(userMessage)
                    .call()
                    .content();

            if (result != null && result.contains("QUERY_TICKET")) {
                return new IntentResult(UserIntent.QUERY_TICKET, "用户明确表示要查询车票");
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
    private TicketQueryParamDTO sendToAIAndExtractParams(List<ChatMessageDTO> history, boolean skipTools) {
        // 只使用最后一条用户消息作为输入，避免历史消息干扰
        String lastUserMessage = "";
        for (int i = history.size() - 1; i >= 0; i--) {
            if ("user".equals(history.get(i).getRole())) {
                lastUserMessage = history.get(i).getContent();
                break;
            }
        }

        // 如果明确是闲聊，不需要调用 tools
        if (skipTools) {
            return new TicketQueryParamDTO();
        }

        // 构建简洁的系统提示，只关注参数提取，不生成示例对话
        String systemPrompt = """
            你是一个12306票务助手的参数提取专家。

            你的任务是从用户消息中提取票务查询参数，只需返回JSON格式的参数，不要生成任何解释性文本。

            【提取规则】
            - from: 出发城市（中文，如"北京"、"上海"）
            - to: 到达城市
            - date: 出发日期（YYYY-MM-DD格式，如"2026-04-19"）
            - timeRange: 时间偏好（morning/afternoon/evening/any），用户没明确说就设为null
            - preference: 排序偏好（fastest/cheapest/direct/any），用户没明确说就设为null

            【日期转换规则】
            - "今天" → 当前日期
            - "明天" → 当前日期+1天
            - "后天" → 当前日期+2天
            - "4月19日" 或 "2026-04-19" → 对应日期
            - 日期格式统一使用 YYYY-MM-DD

            【重要】
            - 只返回JSON，不要任何其他文字
            - 如果参数不确定，设为 null（不要用 "any" 或其他占位符）
            - 参数必须是完整的查询条件

            返回格式：
            {"from":"城市","to":"城市","date":"YYYY-MM-DD","timeRange":"any","preference":"any"}
            """;

        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(lastUserMessage)
                    .tools(stationTools, trainTools, ticketTools)
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
