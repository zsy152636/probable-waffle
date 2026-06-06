package com.example.quiz.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.quiz.ai.entity.AIConfig;
import com.example.quiz.ai.mapper.AIConfigMapper;
import com.example.quiz.sys.common.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AIConfigService {

    private final AIConfigMapper aiConfigMapper;

    public AIConfigService(AIConfigMapper aiConfigMapper) {
        this.aiConfigMapper = aiConfigMapper;
    }

    public Page<AIConfig> page(int page, int pageSize) {
        return aiConfigMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<AIConfig>().orderByDesc(AIConfig::getCreateTime));
    }

    public AIConfig getById(Long id) {
        return aiConfigMapper.selectById(id);
    }

    public AIConfig getActive() {
        AIConfig config = aiConfigMapper.selectOne(
                new LambdaQueryWrapper<AIConfig>().eq(AIConfig::getIsActive, 1)
        );
        if (config == null) {
            throw new BusinessException("未配置激活的 AI 服务，请先在 AI 配置中设置");
        }
        return config;
    }

    @Transactional
    public void add(AIConfig config) {
        if (Integer.valueOf(1).equals(config.getIsActive())) {
            deactivateAll();
        }
        aiConfigMapper.insert(config);
    }

    @Transactional
    public void update(AIConfig config) {
        if (Integer.valueOf(1).equals(config.getIsActive())) {
            deactivateAll();
        }
        aiConfigMapper.updateById(config);
    }

    @Transactional
    public void delete(Long id) {
        aiConfigMapper.deleteById(id);
    }

    @Transactional
    public void activate(Long id) {
        AIConfig target = aiConfigMapper.selectById(id);
        if (target == null) {
            throw new BusinessException("配置不存在，请刷新页面后重试");
        }
        deactivateAll();
        aiConfigMapper.update(null,
                new LambdaUpdateWrapper<AIConfig>()
                        .eq(AIConfig::getId, id)
                        .set(AIConfig::getIsActive, 1));
    }

    @Transactional
    public void deactivate(Long id) {
        aiConfigMapper.update(null,
                new LambdaUpdateWrapper<AIConfig>()
                        .eq(AIConfig::getId, id)
                        .set(AIConfig::getIsActive, 0)
                        .isNotNull(AIConfig::getId));
    }

    private void deactivateAll() {
        aiConfigMapper.update(null,
                new LambdaUpdateWrapper<AIConfig>()
                        .set(AIConfig::getIsActive, 0)
                        .isNotNull(AIConfig::getId));
    }
}
