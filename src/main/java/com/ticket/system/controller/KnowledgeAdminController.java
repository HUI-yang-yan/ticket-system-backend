package com.ticket.system.controller;

import com.ticket.system.common.result.Result;
import com.ticket.system.entity.KnowledgeDocument;
import com.ticket.system.service.KnowledgeDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/knowledge")
@RequiredArgsConstructor
@Tag(name = "知识库管理", description = "管理员上传、管理知识文档")
public class KnowledgeAdminController {

    private final KnowledgeDocumentService knowledgeDocumentService;

    @PostMapping("/upload")
    @Operation(summary = "上传知识文档", description = "上传 .md 格式的知识文档到知识库")
    public Result<String> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "通用规则") String category) {
        log.info("上传知识文档: filename={}, category={}", file.getOriginalFilename(), category);
        knowledgeDocumentService.uploadDocument(file, category);
        return Result.success("上传成功");
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新知识文档", description = "重新上传文件更新知识文档")
    public Result<String> updateDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category) {
        log.info("更新知识文档: id={}", id);
        knowledgeDocumentService.updateDocument(id, file, category);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识文档", description = "删除指定的知识文档及其文件")
    public Result<String> deleteDocument(@PathVariable Long id) {
        log.info("删除知识文档: id={}", id);
        knowledgeDocumentService.deleteDocument(id);
        return Result.success("删除成功");
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取知识文档", description = "根据ID获取知识文档详情")
    public Result<KnowledgeDocument> getDocument(@PathVariable Long id) {
        return Result.success(knowledgeDocumentService.getDocumentById(id));
    }

    @GetMapping("/list")
    @Operation(summary = "获取知识文档列表", description = "获取所有知识文档")
    public Result<List<KnowledgeDocument>> listDocuments() {
        return Result.success(knowledgeDocumentService.getAllDocuments());
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "按分类获取知识文档", description = "根据分类获取知识文档列表")
    public Result<List<KnowledgeDocument>> listByCategory(@PathVariable String category) {
        return Result.success(knowledgeDocumentService.getDocumentsByCategory(category));
    }

    @PostMapping("/{id}/toggle-status")
    @Operation(summary = "切换知识文档状态", description = "启用/禁用知识文档")
    public Result<String> toggleStatus(@PathVariable Long id) {
        knowledgeDocumentService.toggleStatus(id);
        return Result.success("状态切换成功");
    }
}
