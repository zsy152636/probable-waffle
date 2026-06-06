package com.example.quiz.dome.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.quiz.dome.entity.Document;
import com.example.quiz.dome.entity.Question;
import com.example.quiz.dome.entity.TestPaper;
import com.example.quiz.dome.mapper.DocumentMapper;
import com.example.quiz.dome.mapper.QuestionMapper;
import com.example.quiz.dome.mapper.TestPaperMapper;
import com.example.quiz.dome.util.DocumentParser;
import com.example.quiz.sys.common.BusinessException;
import com.example.quiz.sys.util.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DocumentService {

    @Value("${upload.path}")
    private String uploadPath;

    private final DocumentMapper documentMapper;
    private final QuestionMapper questionMapper;
    private final TestPaperMapper testPaperMapper;

    public DocumentService(DocumentMapper documentMapper, QuestionMapper questionMapper,
                           TestPaperMapper testPaperMapper) {
        this.documentMapper = documentMapper;
        this.questionMapper = questionMapper;
        this.testPaperMapper = testPaperMapper;
    }

    public Page<Document> page(int page, int pageSize, String keyword, String docType) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Document::getDocName, keyword);
        }
        if (StringUtils.hasText(docType)) {
            wrapper.eq(Document::getDocType, docType.toUpperCase());
        }
        wrapper.orderByDesc(Document::getCreateTime);
        return documentMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    public Document getById(Long id) {
        return documentMapper.selectById(id);
    }

    public Document upload(MultipartFile file, Long userId, String username) {
        String originalName = file.getOriginalFilename();
        String ext = DocumentParser.getExtension(originalName != null ? originalName : "unknown");
        if (!ext.matches("(?i)txt|docx|pdf|md")) {
            throw new BusinessException("仅支持 TXT / DOCX / PDF / MD 文件");
        }

        // 先解析文本（必须在 transferTo 之前，否则流会被消耗）
        String contentText = DocumentParser.parse(file);

        // 保存文件
        String storedName = UUID.randomUUID().toString() + "." + ext;
        Path uploadDir = Paths.get(uploadPath);
        try {
            Files.createDirectories(uploadDir);
            file.transferTo(uploadDir.resolve(storedName).toFile());
        } catch (IOException e) {
            throw new BusinessException("文件保存失败: " + e.getMessage());
        }

        Document doc = new Document();
        doc.setDocNo(SnowflakeIdGenerator.nextNo("WD"));
        doc.setDocName(originalName);
        doc.setDocType(ext.toUpperCase());
        doc.setFilePath(uploadPath + "/" + storedName);
        doc.setFileSize(file.getSize());
        doc.setContentText(contentText);
        doc.setQuestionCount(0);
        doc.setUploadUserId(userId);
        doc.setUploadUserName(username);
        documentMapper.insert(doc);
        return doc;
    }

    public void update(Document doc) {
        documentMapper.updateById(doc);
    }

    public void delete(Long id) {
        documentMapper.deleteById(id);
    }

    /**
     * 获取文档的确认题目数。若已存在试题则直接返回试题统计。
     */
    public long getConfirmedCount(Long docId) {
        return questionMapper.selectCount(
            new LambdaQueryWrapper<Question>()
                .eq(Question::getDocumentId, docId)
                .eq(Question::getStatus, 1)
        );
    }

    public boolean hasPaper(Long docId) {
        return testPaperMapper.exists(
            new LambdaQueryWrapper<TestPaper>().eq(TestPaper::getDocumentId, docId)
        );
    }

    public Long getPaperId(Long docId) {
        Page<TestPaper> page = testPaperMapper.selectPage(
            new Page<>(1, 1),
            new LambdaQueryWrapper<TestPaper>()
                .eq(TestPaper::getDocumentId, docId)
                .orderByDesc(TestPaper::getCreateTime)
        );
        return page.getRecords().isEmpty() ? null : page.getRecords().get(0).getId();
    }
}
