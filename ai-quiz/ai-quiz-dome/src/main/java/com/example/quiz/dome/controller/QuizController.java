package com.example.quiz.dome.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.quiz.dome.service.QuizService;
import com.example.quiz.sys.common.PageResult;
import com.example.quiz.sys.common.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/{testPaperId}/paper")
    @PreAuthorize("hasAuthority('quiz:take')")
    public Result<Map<String, Object>> getPaper(@PathVariable Long testPaperId) {
        return Result.ok(quizService.getPaper(testPaperId));
    }

    @PostMapping("/{testPaperId}/submit")
    @PreAuthorize("hasAuthority('quiz:take')")
    public Result<Map<String, Object>> submit(@PathVariable Long testPaperId,
                                              @RequestBody Map<String, Object> body,
                                              Authentication auth) {
        Long userId = (Long) auth.getDetails();
        String startTime = (String) body.get("startTime");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) body.get("answers");
        return Result.ok(quizService.submit(testPaperId, userId, startTime, answers));
    }

    @GetMapping("/results")
    @PreAuthorize("hasAuthority('quiz:take')")
    public Result<PageResult<Map<String, Object>>> getResults(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            Authentication auth) {
        Long userId = (Long) auth.getDetails();
        Page<Map<String, Object>> p = quizService.getResults(userId, page, pageSize, keyword);
        return Result.ok(PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize()));
    }

    @GetMapping("/results/{resultId}")
    @PreAuthorize("hasAuthority('quiz:take')")
    public Result<Map<String, Object>> getResult(@PathVariable Long resultId) {
        return Result.ok(quizService.getResultById(resultId));
    }

    @DeleteMapping("/results/{resultId}")
    @PreAuthorize("hasAuthority('quiz:take')")
    public Result<Void> deleteResult(@PathVariable Long resultId, Authentication auth) {
        Long userId = (Long) auth.getDetails();
        quizService.deleteResult(resultId, userId);
        return Result.ok();
    }
}
