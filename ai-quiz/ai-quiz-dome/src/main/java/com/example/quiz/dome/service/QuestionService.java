package com.example.quiz.dome.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.quiz.ai.service.AIService;
import com.example.quiz.dome.entity.Document;
import com.example.quiz.dome.entity.Question;
import com.example.quiz.dome.entity.QuestionOption;
import com.example.quiz.dome.mapper.DocumentMapper;
import com.example.quiz.dome.mapper.QuestionMapper;
import com.example.quiz.dome.mapper.QuestionOptionMapper;
import com.example.quiz.sys.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class QuestionService {

    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper optionMapper;
    private final DocumentMapper documentMapper;
    private final AIService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuestionService(QuestionMapper questionMapper, QuestionOptionMapper optionMapper,
                           DocumentMapper documentMapper, AIService aiService) {
        this.questionMapper = questionMapper;
        this.optionMapper = optionMapper;
        this.documentMapper = documentMapper;
        this.aiService = aiService;
    }

    @Transactional
    public List<Question> generateQuestions(Long documentId, int totalCount,
                                            boolean includeFill, Map<String, Integer> typeRatios,
                                            String direction) {
        Document doc = documentMapper.selectById(documentId);
        if (doc == null) throw new BusinessException("文档不存在");
        if (doc.getContentText() == null || doc.getContentText().isBlank())
            throw new BusinessException("文档内容为空，无法生成题目");

        // 先清除旧题目数据，支持重新生成
        deleteAllByDocument(documentId);

        String aiJson = aiService.generateQuestions(doc.getContentText(), totalCount, includeFill, typeRatios, direction);
        List<Question> questions = parseAndSave(aiJson, documentId);

        doc.setQuestionCount(questions.size());
        doc.setQuestionGenTime(LocalDateTime.now());
        documentMapper.updateById(doc);

        return questions;
    }

    @Transactional
    public void cancelGenerate(Long documentId) {
        deleteAllByDocument(documentId);
        Document doc = documentMapper.selectById(documentId);
        if (doc != null) {
            doc.setQuestionCount(0);
            doc.setQuestionGenTime(null);
            documentMapper.updateById(doc);
        }
    }

    private void deleteAllByDocument(Long documentId) {
        List<Question> existing = questionMapper.selectList(
            new LambdaQueryWrapper<Question>().eq(Question::getDocumentId, documentId));
        for (Question q : existing) {
            optionMapper.delete(new LambdaQueryWrapper<QuestionOption>()
                .eq(QuestionOption::getQuestionId, q.getId()));
        }
        questionMapper.delete(new LambdaQueryWrapper<Question>()
            .eq(Question::getDocumentId, documentId));
    }

    private List<Question> parseAndSave(String json, Long documentId) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode qArr = root.path("questions");
            if (qArr.isEmpty()) throw new BusinessException("AI 未返回有效题目数据");

            List<Question> questions = new ArrayList<>();
            int sort = 0;
            for (JsonNode qNode : qArr) {
                Question q = new Question();
                q.setDocumentId(documentId);
                q.setQuestionType(qNode.path("questionType").asText());
                q.setQuestionTitle(qNode.path("questionTitle").asText());
                q.setPassage(qNode.path("passage").asText(""));
                q.setAnalysis(qNode.path("analysis").asText(""));
                q.setDifficulty(qNode.path("difficulty").asInt(1));
                q.setScore(qNode.path("score").asInt(5));
                q.setSort(sort++);
                q.setStatus(0);
                questionMapper.insert(q);

                // 保存选项
                JsonNode optArr = qNode.path("options");
                if (!optArr.isArray() || optArr.isEmpty()) {
                    throw new BusinessException(
                        String.format("第 %d 道题「%s」缺少 options 选项数据，AI 返回不完整，请重新生成",
                            sort, q.getQuestionTitle()));
                }
                int optSort = 0;
                for (JsonNode optNode : optArr) {
                    QuestionOption opt = new QuestionOption();
                    opt.setQuestionId(q.getId());
                    opt.setOptionLabel(optNode.path("label").asText());
                    opt.setOptionText(optNode.path("text").asText());
                    opt.setIsCorrect(optNode.path("isCorrect").asBoolean() ? 1 : 0);
                    opt.setSort(optSort++);
                    optionMapper.insert(opt);
                }
                questions.add(q);
            }
            return questions;
        } catch (Exception e) {
            if (e instanceof BusinessException) throw (BusinessException) e;
            throw new BusinessException("AI 返回数据解析失败: " + e.getMessage());
        }
    }

    public Page<Question> listByDocument(Long documentId, Integer status, String questionType, int page, int pageSize) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getDocumentId, documentId);
        if (status != null) {
            wrapper.eq(Question::getStatus, status);
        }
        if (questionType != null && !questionType.isEmpty()) {
            wrapper.eq(Question::getQuestionType, questionType);
        }
        wrapper.orderByAsc(Question::getSort);
        return questionMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    public List<Question> listAllByDocument(Long documentId, Integer status) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getDocumentId, documentId);
        if (status != null) {
            wrapper.eq(Question::getStatus, status);
        }
        wrapper.orderByAsc(Question::getSort);
        return questionMapper.selectList(wrapper);
    }

    public Question getById(Long id) {
        return questionMapper.selectById(id);
    }

    public List<QuestionOption> getOptions(Long questionId) {
        return optionMapper.selectList(
            new LambdaQueryWrapper<QuestionOption>()
                .eq(QuestionOption::getQuestionId, questionId)
                .orderByAsc(QuestionOption::getSort)
        );
    }

    public List<QuestionOption> getOptionsBatch(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) return List.of();
        return optionMapper.selectList(
            new LambdaQueryWrapper<QuestionOption>()
                .in(QuestionOption::getQuestionId, questionIds)
                .orderByAsc(QuestionOption::getSort));
    }

    public void update(Question question) {
        questionMapper.updateById(question);
    }

    public void deleteQuestion(Long id) {
        optionMapper.delete(new LambdaQueryWrapper<QuestionOption>().eq(QuestionOption::getQuestionId, id));
        questionMapper.deleteById(id);
    }

    @Transactional
    public void batchConfirm(List<Long> ids) {
        for (Long id : ids) {
            Question q = new Question();
            q.setId(id);
            q.setStatus(1);
            questionMapper.updateById(q);
        }
    }

    @Transactional
    public void batchDelete(List<Long> ids) {
        for (Long id : ids) {
            deleteQuestion(id);
        }
    }
}
