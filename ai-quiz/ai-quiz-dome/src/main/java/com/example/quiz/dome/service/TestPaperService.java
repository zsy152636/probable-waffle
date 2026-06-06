package com.example.quiz.dome.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.quiz.dome.entity.Document;
import com.example.quiz.dome.entity.Question;
import com.example.quiz.dome.entity.QuestionOption;
import com.example.quiz.dome.entity.TestPaper;
import com.example.quiz.dome.entity.TestPaperAnswer;
import com.example.quiz.dome.mapper.DocumentMapper;
import com.example.quiz.dome.mapper.QuestionMapper;
import com.example.quiz.dome.mapper.QuestionOptionMapper;
import com.example.quiz.dome.mapper.TestPaperAnswerMapper;
import com.example.quiz.dome.mapper.TestPaperMapper;
import com.example.quiz.sys.common.BusinessException;
import com.example.quiz.sys.util.SnowflakeIdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TestPaperService {

    private final TestPaperMapper testPaperMapper;
    private final DocumentMapper documentMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper optionMapper;
    private final TestPaperAnswerMapper answerMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TestPaperService(TestPaperMapper testPaperMapper, DocumentMapper documentMapper,
                            QuestionMapper questionMapper, QuestionOptionMapper optionMapper,
                            TestPaperAnswerMapper answerMapper) {
        this.testPaperMapper = testPaperMapper;
        this.documentMapper = documentMapper;
        this.questionMapper = questionMapper;
        this.optionMapper = optionMapper;
        this.answerMapper = answerMapper;
    }

    public Page<TestPaper> page(int page, int pageSize, String keyword) {
        LambdaQueryWrapper<TestPaper> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(TestPaper::getPaperName, keyword);
        }
        wrapper.orderByDesc(TestPaper::getCreateTime);
        return testPaperMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    public TestPaper getById(Long id) {
        return testPaperMapper.selectById(id);
    }

    /**
     * 批量确认题目后自动创建试题
     */
    @Transactional
    public TestPaper createPaperAfterConfirm(Long documentId, List<Long> confirmedQuestionIds) {
        Document doc = documentMapper.selectById(documentId);
        if (doc == null) throw new BusinessException("文档不存在");

        List<Question> questions = questionMapper.selectBatchIds(confirmedQuestionIds);

        TestPaper paper = new TestPaper();
        paper.setPaperNo(SnowflakeIdGenerator.nextNo("TP"));
        paper.setPaperName(doc.getDocName());
        paper.setDocumentId(documentId);
        paper.setDocNo(doc.getDocNo());
        paper.setTotalCount(questions.size());
        paper.setSingleCount(countByType(questions, "SINGLE"));
        paper.setMultiCount(countByType(questions, "MULTI"));
        paper.setJudgeCount(countByType(questions, "JUDGE"));
        paper.setFillCount(countByType(questions, "FILL"));
        paper.setTotalScore(questions.stream().mapToInt(Question::getScore).sum());
        paper.setStatus(1);
        paper.setCreateUserId(doc.getUploadUserId());
        testPaperMapper.insert(paper);

        // 将正确答案写入试卷答案表
        int sort = 0;
        for (Question q : questions) {
            List<QuestionOption> opts = optionMapper.selectList(
                new LambdaQueryWrapper<QuestionOption>()
                    .eq(QuestionOption::getQuestionId, q.getId()));
            List<String> correctLabels = opts.stream()
                    .filter(o -> o.getIsCorrect() == 1)
                    .map(QuestionOption::getOptionLabel)
                    .collect(Collectors.toList());

            TestPaperAnswer answer = new TestPaperAnswer();
            answer.setTestPaperId(paper.getId());
            answer.setQuestionId(q.getId());
            answer.setQuestionType(q.getQuestionType());
            answer.setScore(q.getScore());
            answer.setSort(sort++);
            try {
                answer.setCorrectAnswer(objectMapper.writeValueAsString(correctLabels));
            } catch (JsonProcessingException e) {
                answer.setCorrectAnswer("[]");
            }
            answerMapper.insert(answer);
        }

        return paper;
    }

    @Transactional
    public void delete(Long id) {
        TestPaper paper = testPaperMapper.selectById(id);
        if (paper != null) {
            // 删除试卷答案
            answerMapper.delete(new LambdaQueryWrapper<TestPaperAnswer>()
                .eq(TestPaperAnswer::getTestPaperId, id));
            // 删除关联的题目选项和题目
            List<Question> questions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                    .eq(Question::getDocumentId, paper.getDocumentId())
                    .eq(Question::getStatus, 1));
            for (Question q : questions) {
                optionMapper.delete(new LambdaQueryWrapper<QuestionOption>()
                    .eq(QuestionOption::getQuestionId, q.getId()));
            }
            questionMapper.delete(new LambdaQueryWrapper<Question>()
                .eq(Question::getDocumentId, paper.getDocumentId())
                .eq(Question::getStatus, 1));
            // 重置文档题目计数
            Document doc = documentMapper.selectById(paper.getDocumentId());
            if (doc != null) {
                doc.setQuestionCount(0);
                doc.setQuestionGenTime(null);
                documentMapper.updateById(doc);
            }
        }
        testPaperMapper.deleteById(id);
    }

    private int countByType(List<Question> questions, String type) {
        return (int) questions.stream().filter(q -> type.equals(q.getQuestionType())).count();
    }
}
