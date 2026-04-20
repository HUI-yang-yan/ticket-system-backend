package com.ticket.system.service;

import com.ticket.system.entity.KnowledgeDocument;

/**
 * 知识库向量服务
 * 负责文档分块、向量生成和存储
 */
public interface KnowledgeVectorService {

    /**
     * 为文档生成向量并存储
     * @param document 知识文档
     */
    void generateAndStoreVectors(KnowledgeDocument document);

    /**
     * 删除文档的所有向量
     * @param documentId 文档ID
     */
    void deleteVectors(Long documentId);

    /**
     * 搜索最相关的知识内容
     * @param query 用户查询
     * @param topK 返回最相关的topK条
     * @return 相关内容片段
     */
    String searchRelevantContent(String query, int topK);

    /**
     * 重新生成文档向量（用于更新场景）
     * @param document 知识文档
     */
    void regenerateVectors(KnowledgeDocument document);
}