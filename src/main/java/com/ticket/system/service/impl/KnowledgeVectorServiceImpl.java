package com.ticket.system.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.system.entity.KnowledgeDocument;
import com.ticket.system.service.KnowledgeVectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 知识库向量服务实现
 * 使用 Redisson 存储向量，OpenAI API 生成 embedding，Java 计算余弦相似度
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeVectorServiceImpl implements KnowledgeVectorService {

    private final RedissonClient redissonClient;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String VECTOR_MAP_KEY = "knowledge:vectors";
    private static final String CHUNK_MAP_KEY = "knowledge:chunks";
    private static final String CHUNK_ID_SET_KEY = "knowledge:chunkids";

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${ticket.knowledge.chunk-size:500}")
    private int chunkSize;

    @Value("${ticket.knowledge.chunk-overlap:50}")
    private int chunkOverlap;

    @Override
    public void generateAndStoreVectors(KnowledgeDocument document) {
        try {
            String content = document.getContent();
            if (content == null || content.isBlank()) {
                log.warn("文档内容为空: {}", document.getTitle());
                return;
            }

            // 1. 分块
            List<String> chunks = splitIntoChunks(content);
            log.info("文档分块完成: {}, chunks: {}", document.getTitle(), chunks.size());

            RMap<String, String> vectorMap = redissonClient.getMap(VECTOR_MAP_KEY);
            RMap<String, String> chunkMap = redissonClient.getMap(CHUNK_MAP_KEY);

            // 2. 生成向量并存储
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                String chunkId = document.getId() + "_" + i;

                // 生成 embedding
                float[] embedding = generateEmbedding(chunk);
                if (embedding == null) {
                    log.warn("embedding 生成失败: chunk {}", chunkId);
                    continue;
                }

                // 存储向量和 chunk 内容
                String vectorStr = floatArrayToString(embedding);
                ChunkData chunkData = new ChunkData(chunkId, chunk, document.getTitle(), document.getCategory());

                vectorMap.put(chunkId, vectorStr);
                chunkMap.put(chunkId, objectMapper.writeValueAsString(chunkData));
            }

            log.info("文档向量生成并存储成功: {}, chunks: {}", document.getTitle(), chunks.size());

        } catch (Exception e) {
            log.error("生成文档向量失败: {}", document.getTitle(), e);
            throw new RuntimeException("向量生成失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteVectors(Long documentId) {
        try {
            RMap<String, String> vectorMap = redissonClient.getMap(VECTOR_MAP_KEY);
            RMap<String, String> chunkMap = redissonClient.getMap(CHUNK_MAP_KEY);

            String docPrefix = documentId + "_";
            vectorMap.keySet().removeIf(key -> key.startsWith(docPrefix));
            chunkMap.keySet().removeIf(key -> key.startsWith(docPrefix));

            log.info("删除文档向量: documentId={}", documentId);
        } catch (Exception e) {
            log.error("删除文档向量失败: documentId={}", documentId, e);
        }
    }

    @Override
    public String searchRelevantContent(String query, int topK) {
        try {
            // 1. 生成查询向量
            float[] queryEmbedding = generateEmbedding(query);
            if (queryEmbedding == null) {
                return "";
            }

            // 2. 获取所有 chunk
            RMap<String, String> vectorMap = redissonClient.getMap(VECTOR_MAP_KEY);
            RMap<String, String> chunkMap = redissonClient.getMap(CHUNK_MAP_KEY);

            if (vectorMap.isEmpty()) {
                return "";
            }

            // 3. 计算相似度并排序
            List<ScoredChunk> scoredChunks = new ArrayList<>();

            for (String chunkId : vectorMap.keySet()) {
                String vectorStr = vectorMap.get(chunkId);
                String chunkJson = chunkMap.get(chunkId);

                if (vectorStr != null && chunkJson != null) {
                    float[] chunkEmbedding = stringToFloatArray(vectorStr);
                    double similarity = cosineSimilarity(queryEmbedding, chunkEmbedding);
                    ChunkData chunkData = objectMapper.readValue(chunkJson, ChunkData.class);
                    scoredChunks.add(new ScoredChunk(similarity, chunkData));
                }
            }

            // 4. 取 topK 并拼接结果
            StringBuilder result = new StringBuilder();
            scoredChunks.stream()
                    .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                    .limit(topK)
                    .forEach(sc -> {
                        result.append("【").append(sc.chunkData().title()).append("】\n")
                                .append(sc.chunkData().content()).append("\n\n");
                    });

            return result.toString().trim();

        } catch (Exception e) {
            log.error("搜索相关知识失败: query={}", query, e);
            return "";
        }
    }

    @Override
    public void regenerateVectors(KnowledgeDocument document) {
        deleteVectors(document.getId());
        generateAndStoreVectors(document);
    }

    /**
     * 调用 OpenAI API 生成 embedding
     */
    private float[] generateEmbedding(String text) {
        try {
            String url = baseUrl + "/v1/embeddings";

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("input", text);
            body.put("model", "text-embedding-v4");

            org.springframework.http.HttpEntity<Map<String, Object>> request =
                    new org.springframework.http.HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            if (response == null || !response.containsKey("data")) {
                log.warn("embedding API 返回异常: {}", response);
                return null;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
            if (dataList == null || dataList.isEmpty()) {
                return null;
            }

            @SuppressWarnings("unchecked")
            List<Number> embeddingList = (List<Number>) dataList.get(0).get("embedding");
            float[] result = new float[embeddingList.size()];
            for (int i = 0; i < embeddingList.size(); i++) {
                result[i] = embeddingList.get(i).floatValue();
            }
            return result;

        } catch (Exception e) {
            log.error("生成 embedding 失败: {}", e.getMessage());
            return null;
        }
    }

    private List<String> splitIntoChunks(String content) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = content.split("\n\n");

        StringBuilder currentChunk = new StringBuilder();
        for (String para : paragraphs) {
            if (currentChunk.length() + para.length() + 2 <= chunkSize) {
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(para);
            } else {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                }
                while (para.length() > chunkSize) {
                    chunks.add(para.substring(0, chunkSize));
                    para = para.substring(chunkSize - chunkOverlap);
                }
                currentChunk = new StringBuilder(para);
            }
        }
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
        return chunks;
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0;
        double dotProduct = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private String floatArrayToString(float[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    private float[] stringToFloatArray(String str) {
        String[] parts = str.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }

    private record ChunkData(String id, String content, String title, String category) {}
    private record ScoredChunk(double score, ChunkData chunkData) {}
}