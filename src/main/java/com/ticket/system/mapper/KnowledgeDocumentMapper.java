package com.ticket.system.mapper;

import com.ticket.system.entity.KnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface KnowledgeDocumentMapper {

    void insert(KnowledgeDocument doc);

    void update(KnowledgeDocument doc);

    void deleteById(@Param("id") Long id);

    KnowledgeDocument selectById(@Param("id") Long id);

    List<KnowledgeDocument> selectAll();

    List<KnowledgeDocument> selectByStatus(@Param("status") Integer status);

    List<KnowledgeDocument> selectByCategory(@Param("category") String category);
}
