package com.ticket.system.ai.tools;

import com.ticket.system.service.KnowledgeVectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 知识库检索工具
 * 使用 Redis Vector Search 进行语义检索
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeRetrievalTool {

    private final KnowledgeVectorService knowledgeVectorService;

    /**
     * 检索与用户问题相关的知识内容
     * 使用向量相似度搜索，返回最相关的知识片段
     *
     * @param query 用户的问题，如"退票手续费怎么算"、"候补规则是什么"
     * @return 相关知识内容片段，如果无匹配则返回空字符串
     */
    @Tool(name = "retrieve_knowledge", description = "检索票务知识库中与用户问题相关的内容。当用户询问退票、改签、候补、行李规定等票务规则时，调用此工具获取相关知识作为参考回答。")
    public String retrieveKnowledge(
            @ToolParam(description = "用户的问题或关键词") String query) {
        log.info("[KnowledgeRetrievalTool] 语义检索知识: {}", query);

        try {
            // 使用向量搜索检索最相关的知识内容
            String result = knowledgeVectorService.searchRelevantContent(query, 3);
            log.info("[KnowledgeRetrievalTool] 检索结果长度: {}", result.length());
            return result;
        } catch (Exception e) {
            log.error("[KnowledgeRetrievalTool] 检索失败", e);
            return "";
        }
    }

    /**
     * 获取所有已启用的知识文档标题和分类（供调试用）
     */
    @Tool(name = "list_knowledge_docs", description = "列出所有已启用的知识文档标题和分类")
    public String listKnowledgeDocs() {
        // 此功能暂不实现，后续可通过其他 service 获取
        return "知识库检索工具已就绪";
    }
}