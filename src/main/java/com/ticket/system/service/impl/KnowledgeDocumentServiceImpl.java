package com.ticket.system.service.impl;

import com.ticket.system.entity.KnowledgeDocument;
import com.ticket.system.mapper.KnowledgeDocumentMapper;
import com.ticket.system.service.KnowledgeDocumentService;
import com.ticket.system.service.KnowledgeVectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeVectorService knowledgeVectorService;

    @Value("${ticket.knowledge.base-path:data/knowledge}")
    private String knowledgeBasePath;

    @Override
    @Transactional
    public void uploadDocument(MultipartFile file, String category) {
        try {
            // 创建目录
            Path directory = Paths.get(knowledgeBasePath);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            // 获取文件名
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.endsWith(".md")) {
                throw new IllegalArgumentException("只支持上传 .md 格式的文件");
            }

            // 保存文件
            Path filePath = directory.resolve(filename);
            Files.write(filePath, file.getBytes());

            // 读取文件内容
            String content = Files.readString(filePath);

            // 提取标题（文件名去掉.md后缀）
            String title = filename.replace(".md", "");

            // 保存到数据库
            KnowledgeDocument doc = new KnowledgeDocument();
            doc.setTitle(title);
            doc.setFilename(filename);
            doc.setCategory(category);
            doc.setContent(content);
            doc.setFilePath(filePath.toString());
            doc.setStatus(1);
            knowledgeDocumentMapper.insert(doc);

            // 生成并存储向量
            knowledgeVectorService.generateAndStoreVectors(doc);

            log.info("知识文档上传成功: {}, category: {}", filename, category);
        } catch (IOException e) {
            log.error("知识文档上传失败", e);
            throw new RuntimeException("知识文档上传失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void updateDocument(Long id, MultipartFile file, String category) {
        KnowledgeDocument existing = knowledgeDocumentMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("文档不存在: " + id);
        }

        try {
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.endsWith(".md")) {
                throw new IllegalArgumentException("只支持上传 .md 格式的文件");
            }

            // 更新文件
            Path filePath = Paths.get(existing.getFilePath());
            Files.write(filePath, file.getBytes());

            // 读取新内容
            String content = Files.readString(filePath);

            // 更新数据库
            existing.setFilename(filename);
            existing.setContent(content);
            existing.setFilePath(filePath.toString());
            if (category != null) {
                existing.setCategory(category);
            }
            knowledgeDocumentMapper.update(existing);

            // 重新生成向量
            knowledgeVectorService.regenerateVectors(existing);

            log.info("知识文档更新成功: {}", filename);
        } catch (IOException e) {
            log.error("知识文档更新失败", e);
            throw new RuntimeException("知识文档更新失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteDocument(Long id) {
        KnowledgeDocument doc = knowledgeDocumentMapper.selectById(id);
        if (doc == null) {
            throw new IllegalArgumentException("文档不存在: " + id);
        }

        // 删除向量
        knowledgeVectorService.deleteVectors(id);

        // 删除文件
        try {
            Path filePath = Paths.get(doc.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            log.warn("删除知识文档文件失败: {}", doc.getFilePath(), e);
        }

        // 删除数据库记录
        knowledgeDocumentMapper.deleteById(id);
        log.info("知识文档删除成功: {}", doc.getTitle());
    }

    @Override
    public KnowledgeDocument getDocumentById(Long id) {
        return knowledgeDocumentMapper.selectById(id);
    }

    @Override
    public List<KnowledgeDocument> getAllDocuments() {
        return knowledgeDocumentMapper.selectAll();
    }

    @Override
    public List<KnowledgeDocument> getDocumentsByStatus(Integer status) {
        return knowledgeDocumentMapper.selectByStatus(status);
    }

    @Override
    public List<KnowledgeDocument> getDocumentsByCategory(String category) {
        return knowledgeDocumentMapper.selectByCategory(category);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        KnowledgeDocument doc = knowledgeDocumentMapper.selectById(id);
        if (doc == null) {
            throw new IllegalArgumentException("文档不存在: " + id);
        }
        doc.setStatus(doc.getStatus() == 1 ? 0 : 1);
        knowledgeDocumentMapper.update(doc);
        log.info("知识文档状态切换: {} -> {}", doc.getTitle(), doc.getStatus());
    }
}