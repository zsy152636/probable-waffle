package com.example.quiz.dome.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.quiz.dome.entity.TestPaper;
import com.example.quiz.dome.service.TestPaperService;
import com.example.quiz.sys.common.PageResult;
import com.example.quiz.sys.common.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test-papers")
public class TestPaperController {

    private final TestPaperService testPaperService;

    public TestPaperController(TestPaperService testPaperService) {
        this.testPaperService = testPaperService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('paper:list')")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        Page<TestPaper> p = testPaperService.page(page, pageSize, keyword);
        var enriched = p.getRecords().stream().map(paper -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", paper.getId().toString());
            m.put("paperNo", paper.getPaperNo());
            m.put("paperName", paper.getPaperName());
            m.put("documentId", paper.getDocumentId().toString());
            m.put("docNo", paper.getDocNo());
            m.put("totalCount", paper.getTotalCount());
            m.put("singleCount", paper.getSingleCount());
            m.put("multiCount", paper.getMultiCount());
            m.put("judgeCount", paper.getJudgeCount());
            m.put("fillCount", paper.getFillCount());
            m.put("totalScore", paper.getTotalScore());
            m.put("createTime", paper.getCreateTime());
            return m;
        }).toList();
        return Result.ok(PageResult.of(enriched, p.getTotal(), p.getCurrent(), p.getSize()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('paper:list')")
    public Result<TestPaper> get(@PathVariable Long id) {
        return Result.ok(testPaperService.getById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('paper:delete')")
    public Result<?> delete(@PathVariable Long id) {
        testPaperService.delete(id);
        return Result.ok();
    }
}
