package com.example.quiz.dome.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.quiz.dome.entity.Document;
import com.example.quiz.dome.service.DocumentService;
import com.example.quiz.sys.common.PageResult;
import com.example.quiz.sys.common.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('doc:list')")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String docType) {
        Page<Document> p = documentService.page(page, pageSize, keyword, docType);
        var enriched = p.getRecords().stream().map(doc -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", doc.getId().toString());
            m.put("docNo", doc.getDocNo());
            m.put("docName", doc.getDocName());
            m.put("docType", doc.getDocType());
            m.put("fileSize", doc.getFileSize());
            m.put("questionCount", doc.getQuestionCount());
            long confirmed = documentService.getConfirmedCount(doc.getId());
            m.put("confirmedCount", (int) confirmed);
            m.put("hasPaper", documentService.hasPaper(doc.getId()));
            m.put("paperId", documentService.getPaperId(doc.getId()));
            m.put("questionGenTime", doc.getQuestionGenTime());
            m.put("uploadUserName", doc.getUploadUserName());
            m.put("createTime", doc.getCreateTime());
            m.put("updateTime", doc.getUpdateTime());
            return m;
        }).toList();
        return Result.ok(PageResult.of(enriched, p.getTotal(), p.getCurrent(), p.getSize()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('doc:list')")
    public Result<Document> get(@PathVariable Long id) {
        return Result.ok(documentService.getById(id));
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('doc:upload')")
    public Result<List<Document>> upload(@RequestParam("files") List<MultipartFile> files, Authentication auth) {
        Long userId = (Long) auth.getDetails();
        String username = auth.getName();
        List<Document> docs = new ArrayList<>();
        for (MultipartFile file : files) {
            docs.add(documentService.upload(file, userId, username));
        }
        return Result.ok(docs);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('doc:update')")
    public Result<?> update(@PathVariable Long id, @RequestBody Document doc) {
        doc.setId(id);
        documentService.update(doc);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('doc:delete')")
    public Result<?> delete(@PathVariable Long id) {
        documentService.delete(id);
        return Result.ok();
    }
}
