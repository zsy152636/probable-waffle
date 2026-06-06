package com.example.quiz.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.quiz.ai.config.AIClientFactory;
import com.example.quiz.ai.entity.AIConfig;
import com.example.quiz.ai.service.AIConfigService;
import com.example.quiz.sys.common.PageResult;
import com.example.quiz.sys.common.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai-configs")
public class AIConfigController {

    private final AIConfigService aiConfigService;
    private final AIClientFactory clientFactory;

    public AIConfigController(AIConfigService aiConfigService, AIClientFactory clientFactory) {
        this.aiConfigService = aiConfigService;
        this.clientFactory = clientFactory;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ai:list')")
    public Result<PageResult<AIConfig>> list(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int pageSize) {
        Page<AIConfig> p = aiConfigService.page(page, pageSize);
        return Result.ok(PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ai:list')")
    public Result<AIConfig> get(@PathVariable Long id) {
        return Result.ok(aiConfigService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ai:add')")
    public Result<?> add(@RequestBody AIConfig config) {
        aiConfigService.add(config);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ai:update')")
    public Result<?> update(@PathVariable Long id, @RequestBody AIConfig config) {
        config.setId(id);
        aiConfigService.update(config);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ai:delete')")
    public Result<?> delete(@PathVariable Long id) {
        aiConfigService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/test")
    @PreAuthorize("hasAuthority('ai:test')")
    public Result<?> testConnection(@PathVariable Long id) throws JsonProcessingException {
        AIConfig config = aiConfigService.getById(id);
        if (config == null) {
            return Result.fail("配置不存在，请刷新页面后重试");
        }
        String reply = clientFactory.testConnection(config);
        return Result.ok(reply);
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('ai:update')")
    public Result<?> activate(@PathVariable Long id) {
        aiConfigService.activate(id);
        return Result.ok();
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('ai:update')")
    public Result<?> deactivate(@PathVariable Long id) {
        aiConfigService.deactivate(id);
        return Result.ok();
    }
}
