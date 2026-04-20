package com.ticket.system.entity;

import lombok.Data;
import java.util.Date;

@Data
public class KnowledgeDocument {
    private Long id;
    private String title;           // 文档标题
    private String filename;       // 文件名
    private String category;        // 分类（退票规则/定价规则/候补规则等）
    private String content;         // 文档内容（Markdown）
    private String filePath;        // 服务器文件路径
    private Integer status;         // 状态（0禁用/1启用）
    private Date createdAt;
    private Date updatedAt;
}
