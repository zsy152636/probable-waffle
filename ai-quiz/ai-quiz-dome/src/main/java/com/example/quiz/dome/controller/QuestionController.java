package com.example.quiz.dome.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.quiz.dome.entity.Question;
import com.example.quiz.dome.entity.TestPaper;
import com.example.quiz.dome.service.QuestionService;
import com.example.quiz.dome.service.TestPaperService;
import com.example.quiz.sys.common.PageResult;
import com.example.quiz.sys.common.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.quiz.dome.entity.QuestionGenConfig;
import com.example.quiz.dome.entity.QuestionOption;
import com.example.quiz.dome.service.QuestionGenConfigService;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class QuestionController {

    private final QuestionService questionService;
    private final TestPaperService testPaperService;
    private final QuestionGenConfigService configService;

    public QuestionController(QuestionService questionService, TestPaperService testPaperService,
                              QuestionGenConfigService configService) {
        this.questionService = questionService;
        this.testPaperService = testPaperService;
        this.configService = configService;
    }

    @PostMapping("/documents/{docId}/generate-questions")
    @PreAuthorize("hasAuthority('doc:genQuestion')")
    public Result<List<Map<String, Object>>> generateQuestions(
            @PathVariable Long docId, @RequestBody Map<String, Object> body) {
        Object configIdObj = body.get("configId");
        List<Question> questions;

        Long configId = null;
        if (configIdObj instanceof Number n) {
            configId = n.longValue();
        } else if (configIdObj instanceof String s && !s.isBlank()) {
            configId = Long.valueOf(s);
        }

        if (configId != null) {
            // 新模式：使用题目生成配置
            QuestionGenConfig config = configService.getById(configId);
            if (config == null) {
                return Result.fail("题目生成配置不存在");
            }
            String reqDirection = body.get("direction") instanceof String s ? s : "";
            String direction = mergeDirection(config.getDirection(), reqDirection);
            questions = questionService.generateQuestionsFromConfig(docId, config, direction);
        } else {
            // 兼容旧模式
            int totalCount = body.get("totalCount") instanceof Integer i ? i : 20;
            boolean includeFill = body.get("includeFill") instanceof Boolean b && b;
            String direction = body.get("direction") instanceof String s ? s : "";
            @SuppressWarnings("unchecked")
            Map<String, Integer> typeRatios = (Map<String, Integer>) body.get("typeRatios");
            if (typeRatios == null) {
                typeRatios = new LinkedHashMap<>();
                typeRatios.put("single", 50);
                typeRatios.put("multi", 30);
                typeRatios.put("judge", 20);
            }
            questions = questionService.generateQuestions(docId, totalCount, includeFill, typeRatios, direction);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Question q : questions) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", q.getId().toString());
            item.put("questionType", q.getQuestionType());
            item.put("questionTitle", q.getQuestionTitle());
            item.put("passage", q.getPassage() != null ? q.getPassage() : "");
            List<QuestionOption> opts = questionService.getOptions(q.getId());
            List<Map<String, Object>> optList = new ArrayList<>();
            for (QuestionOption o : opts) {
                Map<String, Object> om = new LinkedHashMap<>();
                om.put("label", o.getOptionLabel());
                om.put("text", o.getOptionText());
                om.put("isCorrect", o.getIsCorrect());
                om.put("sort", o.getSort());
                optList.add(om);
            }
            item.put("options", optList);
            item.put("analysis", q.getAnalysis());
            item.put("difficulty", q.getDifficulty());
            item.put("score", q.getScore());
            item.put("sort", q.getSort());
            item.put("status", q.getStatus());
            result.add(item);
        }
        return Result.ok(result);
    }

    @GetMapping("/documents/{docId}/questions")
    @PreAuthorize("hasAuthority('doc:list')")
    public Result<PageResult<Map<String, Object>>> listQuestions(
            @PathVariable Long docId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String questionType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        Page<Question> questionPage = questionService.listByDocument(docId, status, questionType, page, pageSize);
        List<Question> questions = questionPage.getRecords();

        // 批量加载选项
        List<Long> qIds = questions.stream().map(Question::getId).toList();
        Map<Long, List<QuestionOption>> optionMap = new HashMap<>();
        if (!qIds.isEmpty()) {
            List<QuestionOption> allOptions = questionService.getOptionsBatch(qIds);
            for (QuestionOption o : allOptions) {
                optionMap.computeIfAbsent(o.getQuestionId(), k -> new ArrayList<>()).add(o);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Question q : questions) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", q.getId().toString());
            item.put("documentId", q.getDocumentId().toString());
            item.put("questionType", q.getQuestionType());
            item.put("questionTitle", q.getQuestionTitle());
            item.put("passage", q.getPassage() != null ? q.getPassage() : "");
            List<QuestionOption> opts = optionMap.getOrDefault(q.getId(), List.of());
            List<Map<String, Object>> optList = new ArrayList<>();
            for (QuestionOption o : opts) {
                Map<String, Object> om = new LinkedHashMap<>();
                om.put("label", o.getOptionLabel());
                om.put("text", o.getOptionText());
                om.put("isCorrect", o.getIsCorrect());
                om.put("sort", o.getSort());
                optList.add(om);
            }
            item.put("options", optList);
            item.put("analysis", q.getAnalysis());
            item.put("difficulty", q.getDifficulty());
            item.put("score", q.getScore());
            item.put("sort", q.getSort());
            item.put("status", q.getStatus());
            result.add(item);
        }
        return Result.ok(PageResult.of(result, questionPage.getTotal(), page, pageSize));
    }

    @PutMapping("/questions/{id}")
    @PreAuthorize("hasAuthority('question:confirm')")
    public Result<?> updateQuestion(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        questionService.updateWithOptions(id, body);
        return Result.ok();
    }

    @DeleteMapping("/questions/{id}")
    @PreAuthorize("hasAuthority('question:confirm')")
    public Result<?> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return Result.ok();
    }

    @PostMapping("/questions/batch-confirm")
    @PreAuthorize("hasAuthority('question:confirm')")
    public Result<Map<String, Object>> batchConfirm(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        questionService.batchConfirm(ids);

        Question first = questionService.getById(ids.get(0));
        TestPaper paper = testPaperService.createPaperAfterConfirm(first.getDocumentId(), ids);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("paperId", paper.getId().toString());
        resp.put("paperNo", paper.getPaperNo());
        resp.put("paperName", paper.getPaperName());
        resp.put("totalCount", paper.getTotalCount());
        resp.put("singleCount", paper.getSingleCount());
        resp.put("multiCount", paper.getMultiCount());
        resp.put("judgeCount", paper.getJudgeCount());
        resp.put("fillCount", paper.getFillCount());
        resp.put("shortCount", paper.getShortCount());
        resp.put("codeCount", paper.getCodeCount());
        resp.put("calcCount", paper.getCalcCount());
        resp.put("totalScore", paper.getTotalScore());
        return Result.ok(resp);
    }

    @PostMapping("/questions/batch-delete")
    @PreAuthorize("hasAuthority('question:confirm')")
    public Result<?> batchDelete(@RequestBody Map<String, List<Long>> body) {
        questionService.batchDelete(body.get("ids"));
        return Result.ok();
    }

    @DeleteMapping("/documents/{docId}/questions")
    @PreAuthorize("hasAuthority('doc:genQuestion')")
    public Result<?> cancelGenerate(@PathVariable Long docId) {
        questionService.cancelGenerate(docId);
        return Result.ok();
    }

    private String mergeDirection(String configDirection, String reqDirection) {
        if (configDirection == null || configDirection.isBlank()) {
            return reqDirection;
        }
        if (reqDirection == null || reqDirection.isBlank()) {
            return configDirection;
        }
        return configDirection + "\n" + reqDirection;
    }
}
