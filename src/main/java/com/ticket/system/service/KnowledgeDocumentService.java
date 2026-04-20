package com.ticket.system.service;

import com.ticket.system.entity.KnowledgeDocument;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface KnowledgeDocumentService {

    void uploadDocument(MultipartFile file, String category);

    void updateDocument(Long id, MultipartFile file, String category);

    void deleteDocument(Long id);

    KnowledgeDocument getDocumentById(Long id);

    List<KnowledgeDocument> getAllDocuments();

    List<KnowledgeDocument> getDocumentsByStatus(Integer status);

    List<KnowledgeDocument> getDocumentsByCategory(String category);

    void toggleStatus(Long id);
}
