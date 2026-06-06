package com.example.quiz.dome.controller;

import com.example.quiz.dome.service.StatsService;
import com.example.quiz.sys.common.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('quiz:take')")
    public Result<Map<String, Object>> getOverview(Authentication auth) {
        Long userId = (Long) auth.getDetails();
        return Result.ok(statsService.getOverview(userId));
    }
}
