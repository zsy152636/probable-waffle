package com.example.quiz.dome.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.quiz.dome.entity.QuestionGenConfig;
import com.example.quiz.dome.entity.QuestionGenConfigType;
import com.example.quiz.dome.service.QuestionGenConfigService;
import com.example.quiz.sys.common.PageResult;
import com.example.quiz.sys.common.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class QuestionGenConfigController {

    private final QuestionGenConfigService configService;

    public QuestionGenConfigController(QuestionGenConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/question-gen-configs")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int pageSize) {
        Page<QuestionGenConfig> result = configService.list(page, pageSize);
        List<Map<String, Object>> records = new ArrayList<>();
        for (QuestionGenConfig c : result.getRecords()) {
            records.add(toMap(c));
        }
        return Result.ok(PageResult.of(records, result.getTotal(), page, pageSize));
    }

    @GetMapping("/question-gen-configs/active")
    public Result<List<Map<String, Object>>> listActive() {
        List<QuestionGenConfig> list = configService.listAllActive();
        List<Map<String, Object>> records = new ArrayList<>();
        for (QuestionGenConfig c : list) {
            records.add(toMap(c));
        }
        return Result.ok(records);
    }

    @GetMapping("/question-gen-configs/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        QuestionGenConfig c = configService.getById(id);
        if (c == null) {
            return Result.fail("配置不存在");
        }
        return Result.ok(toMap(c));
    }

    @PostMapping("/question-gen-configs")
    @PreAuthorize("hasAuthority('doc:genQuestion')")
    public Result<Map<String, Object>> create(@RequestBody QuestionGenConfig config) {
        configService.save(config);
        return Result.ok(toMap(configService.getById(config.getId())));
    }

    @PutMapping("/question-gen-configs/{id}")
    @PreAuthorize("hasAuthority('doc:genQuestion')")
    public Result<?> update(@PathVariable Long id, @RequestBody QuestionGenConfig config) {
        config.setId(id);
        configService.update(config);
        return Result.ok();
    }

    @DeleteMapping("/question-gen-configs/{id}")
    @PreAuthorize("hasAuthority('doc:genQuestion')")
    public Result<?> delete(@PathVariable Long id) {
        configService.delete(id);
        return Result.ok();
    }

    private Map<String, Object> toMap(QuestionGenConfig c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId().toString());
        map.put("name", c.getName());
        map.put("description", c.getDescription());
        map.put("totalCount", c.getTotalCount());
        map.put("needPassage", c.getNeedPassage());
        map.put("direction", c.getDirection());
        map.put("sort", c.getSort());
        map.put("isActive", c.getIsActive());
        map.put("createTime", c.getCreateTime());
        map.put("updateTime", c.getUpdateTime());
        // 子表题型明细
        if (c.getTypes() != null) {
            List<Map<String, Object>> types = new ArrayList<>();
            for (QuestionGenConfigType t : c.getTypes()) {
                Map<String, Object> tm = new LinkedHashMap<>();
                tm.put("id", t.getId().toString());
                tm.put("questionType", t.getQuestionType());
                tm.put("count", t.getCount());
                tm.put("sort", t.getSort());
                types.add(tm);
            }
            map.put("types", types);
        }
        return map;
    }
}
