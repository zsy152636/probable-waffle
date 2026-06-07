package com.example.quiz.dome.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.quiz.dome.entity.QuestionGenConfig;
import com.example.quiz.dome.entity.QuestionGenConfigType;
import com.example.quiz.dome.mapper.QuestionGenConfigMapper;
import com.example.quiz.dome.mapper.QuestionGenConfigTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class QuestionGenConfigService {

    private final QuestionGenConfigMapper mapper;
    private final QuestionGenConfigTypeMapper typeMapper;

    public QuestionGenConfigService(QuestionGenConfigMapper mapper,
                                     QuestionGenConfigTypeMapper typeMapper) {
        this.mapper = mapper;
        this.typeMapper = typeMapper;
    }

    public Page<QuestionGenConfig> list(int page, int pageSize) {
        Page<QuestionGenConfig> result = mapper.selectPage(
            new Page<>(page, pageSize),
            new LambdaQueryWrapper<QuestionGenConfig>()
                .orderByAsc(QuestionGenConfig::getSort)
                .orderByDesc(QuestionGenConfig::getCreateTime)
        );
        for (QuestionGenConfig config : result.getRecords()) {
            config.setTypes(loadTypes(config.getId()));
        }
        return result;
    }

    public List<QuestionGenConfig> listAllActive() {
        List<QuestionGenConfig> configs = mapper.selectList(
            new LambdaQueryWrapper<QuestionGenConfig>()
                .eq(QuestionGenConfig::getIsActive, 1)
                .orderByAsc(QuestionGenConfig::getSort)
        );
        for (QuestionGenConfig config : configs) {
            config.setTypes(loadTypes(config.getId()));
        }
        return configs;
    }

    public QuestionGenConfig getById(Long id) {
        QuestionGenConfig config = mapper.selectById(id);
        if (config != null) {
            config.setTypes(loadTypes(id));
        }
        return config;
    }

    @Transactional
    public void save(QuestionGenConfig config) {
        if (config.getTotalCount() == null) config.setTotalCount(0);
        if (config.getSort() == null) config.setSort(0);
        if (config.getIsActive() == null) config.setIsActive(1);
        mapper.insert(config);
        saveTypes(config.getId(), config.getTypes());
    }

    @Transactional
    public void update(QuestionGenConfig config) {
        mapper.updateById(config);
        // 删除旧明细，插入新明细
        typeMapper.delete(new LambdaQueryWrapper<QuestionGenConfigType>()
                .eq(QuestionGenConfigType::getConfigId, config.getId()));
        saveTypes(config.getId(), config.getTypes());
    }

    @Transactional
    public void delete(Long id) {
        typeMapper.delete(new LambdaQueryWrapper<QuestionGenConfigType>()
                .eq(QuestionGenConfigType::getConfigId, id));
        mapper.deleteById(id);
    }

    private List<QuestionGenConfigType> loadTypes(Long configId) {
        return typeMapper.selectList(
            new LambdaQueryWrapper<QuestionGenConfigType>()
                .eq(QuestionGenConfigType::getConfigId, configId)
                .orderByAsc(QuestionGenConfigType::getSort)
        );
    }

    private void saveTypes(Long configId, List<QuestionGenConfigType> types) {
        if (types != null) {
            int sort = 0;
            for (QuestionGenConfigType t : types) {
                t.setConfigId(configId);
                t.setSort(sort++);
                typeMapper.insert(t);
            }
        }
    }
}
