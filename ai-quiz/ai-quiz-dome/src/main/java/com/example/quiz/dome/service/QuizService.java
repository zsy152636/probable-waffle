package com.example.quiz.dome.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.quiz.dome.entity.*;
import com.example.quiz.dome.mapper.*;
import com.example.quiz.sys.common.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class QuizService {

    private final TestPaperMapper testPaperMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper optionMapper;
    private final QuizResultMapper quizResultMapper;
    private final TestPaperAnswerMapper answerMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuizService(TestPaperMapper testPaperMapper, QuestionMapper questionMapper,
                       QuestionOptionMapper optionMapper, QuizResultMapper quizResultMapper,
                       TestPaperAnswerMapper answerMapper) {
        this.testPaperMapper = testPaperMapper;
        this.questionMapper = questionMapper;
        this.optionMapper = optionMapper;
        this.quizResultMapper = quizResultMapper;
        this.answerMapper = answerMapper;
    }

    public Map<String, Object> getPaper(Long testPaperId) {
        TestPaper paper = testPaperMapper.selectById(testPaperId);
        if (paper == null) throw new BusinessException("试题不存在");

        List<Question> questions = questionMapper.selectList(
            new LambdaQueryWrapper<Question>()
                .eq(Question::getDocumentId, paper.getDocumentId())
                .eq(Question::getStatus, 1)
                .orderByAsc(Question::getSort)
        );

        List<Map<String, Object>> questionList = new ArrayList<>();
        for (Question q : questions) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", q.getId().toString());
            item.put("questionType", q.getQuestionType());
            item.put("questionTitle", q.getQuestionTitle());
            item.put("passage", q.getPassage());
            item.put("score", q.getScore());
            item.put("sort", q.getSort());

            List<QuestionOption> options = optionMapper.selectList(
                new LambdaQueryWrapper<QuestionOption>()
                    .eq(QuestionOption::getQuestionId, q.getId())
                    .orderByAsc(QuestionOption::getSort)
            );
            List<Map<String, Object>> optList = new ArrayList<>();
            for (QuestionOption o : options) {
                Map<String, Object> om = new LinkedHashMap<>();
                om.put("label", o.getOptionLabel());
                om.put("text", o.getOptionText());
                optList.add(om);
            }
            item.put("options", optList);
            questionList.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paperId", paper.getPaperNo());
        result.put("testPaperId", paper.getId().toString());
        result.put("documentId", paper.getDocumentId().toString());
        result.put("docName", paper.getPaperName());
        result.put("totalScore", paper.getTotalScore());
        result.put("totalCount", paper.getTotalCount());
        result.put("durationMinutes", 30);
        result.put("questions", questionList);
        return result;
    }

    @Transactional
    public Map<String, Object> submit(Long testPaperId, Long userId,
                                       String startTime, List<Map<String, Object>> answers) {
        TestPaper paper = testPaperMapper.selectById(testPaperId);
        if (paper == null) throw new BusinessException("试题不存在");

        // 从试卷答案表加载正确答案
        List<TestPaperAnswer> paperAnswers = answerMapper.selectList(
            new LambdaQueryWrapper<TestPaperAnswer>()
                .eq(TestPaperAnswer::getTestPaperId, testPaperId)
                .orderByAsc(TestPaperAnswer::getSort));
        Map<Long, List<String>> correctAnswerMap = new LinkedHashMap<>();
        Map<Long, TestPaperAnswer> answerInfoMap = new LinkedHashMap<>();
        for (TestPaperAnswer pa : paperAnswers) {
            correctAnswerMap.put(pa.getQuestionId(), parseAnswerLabels(pa.getCorrectAnswer()));
            answerInfoMap.put(pa.getQuestionId(), pa);
        }

        List<Question> questions = questionMapper.selectList(
            new LambdaQueryWrapper<Question>()
                .eq(Question::getDocumentId, paper.getDocumentId())
                .eq(Question::getStatus, 1)
        );

        Map<String, List<String>> userAnswerMap = toAnswerMap(answers);

        int totalScore = 0;
        int correctCount = 0;
        List<Map<String, Object>> details = new ArrayList<>();

        for (Question q : questions) {
            List<String> correctLabels = correctAnswerMap.getOrDefault(q.getId(), List.of());
            TestPaperAnswer answerInfo = answerInfoMap.get(q.getId());
            List<String> userAnswer = userAnswerMap.getOrDefault(q.getId().toString(), List.of());
            int earnedScore = gradeQuestion(q, correctLabels, userAnswer);
            totalScore += earnedScore;
            if (earnedScore > 0) correctCount++;

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("questionId", q.getId().toString());
            detail.put("questionTitle", q.getQuestionTitle());
            detail.put("passage", q.getPassage());
            detail.put("questionType", q.getQuestionType());
            detail.put("userAnswer", userAnswer);
            detail.put("correctAnswer", correctLabels);
            detail.put("isCorrect", earnedScore > 0);
            detail.put("score", answerInfo != null ? answerInfo.getScore() : q.getScore());
            detail.put("earnedScore", earnedScore);
            detail.put("analysis", q.getAnalysis());
            details.add(detail);
        }

        QuizResult result = new QuizResult();
        result.setTestPaperId(testPaperId);
        result.setDocumentId(paper.getDocumentId());
        result.setUserId(userId);
        result.setTotalScore(paper.getTotalScore());
        result.setUserScore(totalScore);
        result.setCorrectCount(correctCount);
        result.setTotalCount(questions.size());
        LocalDateTime start = Instant.parse(startTime)
                .atZone(ZoneId.of("Asia/Shanghai")).toLocalDateTime();
        result.setStartTime(start);
        result.setSubmitTime(LocalDateTime.now());

        try {
            result.setAnswersJson(objectMapper.writeValueAsString(answers));
        } catch (JsonProcessingException ignored) {}

        result.setDurationSeconds((int) Duration.between(start, LocalDateTime.now()).getSeconds());
        quizResultMapper.insert(result);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("resultId", result.getId().toString());
        resp.put("totalScore", paper.getTotalScore());
        resp.put("userScore", totalScore);
        resp.put("correctCount", correctCount);
        resp.put("totalCount", questions.size());
        resp.put("durationSeconds", result.getDurationSeconds());
        resp.put("details", details);
        return resp;
    }

    private int gradeQuestion(Question q, List<String> correctLabels, List<String> userAnswer) {
        if (userAnswer == null || userAnswer.isEmpty()) return 0;
        if (correctLabels.isEmpty()) return 0;

        List<String> sortedCorrect = correctLabels.stream().sorted().toList();
        List<String> sortedUser = userAnswer.stream().sorted().toList();

        String type = q.getQuestionType();
        if ("SINGLE".equals(type) || "JUDGE".equals(type)) {
            return sortedUser.equals(sortedCorrect) ? q.getScore() : 0;
        } else if ("MULTI".equals(type)) {
            return new HashSet<>(sortedUser).equals(new HashSet<>(sortedCorrect)) ? q.getScore() : 0;
        } else if ("FILL".equals(type)) {
            String u = String.join("", sortedUser).trim().toLowerCase();
            String c = String.join("", sortedCorrect).trim().toLowerCase();
            return u.equals(c) ? q.getScore() : 0;
        } else if ("SHORT".equals(type)) {
            String ref = String.join("", sortedCorrect).trim().toLowerCase();
            String u = String.join("", userAnswer).trim().toLowerCase();
            if (u.equals(ref)) return q.getScore();
            if (!u.isEmpty() && (ref.contains(u) || u.contains(ref))) return q.getScore() / 2;
            return 0;
        }
        return 0;
    }

    private List<String> parseAnswerLabels(String correctAnswerJson) {
        try {
            return objectMapper.readValue(correctAnswerJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, List<String>> toAnswerMap(List<Map<String, Object>> answers) {
        Map<String, List<String>> map = new HashMap<>();
        for (Map<String, Object> a : answers) {
            String qid = String.valueOf(a.get("questionId"));
            Object ans = a.get("userAnswer");
            if (ans instanceof List<?> list) {
                map.put(qid, list.stream().map(String::valueOf).toList());
            }
        }
        return map;
    }

    public Page<Map<String, Object>> getResults(Long userId, int page, int pageSize, String keyword) {
        List<QuizResult> allResults = quizResultMapper.selectList(
            new LambdaQueryWrapper<QuizResult>()
                .eq(QuizResult::getUserId, userId)
                .orderByDesc(QuizResult::getCreateTime));

        Set<Long> paperIds = new HashSet<>();
        for (QuizResult r : allResults) {
            paperIds.add(r.getTestPaperId());
        }
        Map<Long, String> paperNameMap = new HashMap<>();
        if (!paperIds.isEmpty()) {
            List<TestPaper> papers = testPaperMapper.selectBatchIds(paperIds);
            for (TestPaper p : papers) {
                paperNameMap.put(p.getId(), p.getPaperName());
            }
        }

        List<Map<String, Object>> enriched = new ArrayList<>();
        for (QuizResult r : allResults) {
            String docName = paperNameMap.getOrDefault(r.getTestPaperId(), "");

            if (keyword != null && !keyword.isBlank()) {
                if (!docName.contains(keyword)) continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId().toString());
            item.put("testPaperId", r.getTestPaperId().toString());
            item.put("documentId", r.getDocumentId().toString());
            item.put("docName", docName);
            item.put("totalScore", r.getTotalScore());
            item.put("userScore", r.getUserScore());
            item.put("correctCount", r.getCorrectCount());
            item.put("totalCount", r.getTotalCount());
            item.put("durationSeconds", r.getDurationSeconds());
            item.put("submitTime", r.getSubmitTime() != null ? r.getSubmitTime().toString() : "");
            enriched.add(item);
        }

        int total = enriched.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<Map<String, Object>> paged = fromIndex < total
            ? enriched.subList(fromIndex, toIndex)
            : List.of();

        Page<Map<String, Object>> enrichedPage = new Page<>(page, pageSize);
        enrichedPage.setRecords(paged);
        enrichedPage.setTotal(total);
        return enrichedPage;
    }

    @Transactional
    public void deleteResult(Long resultId, Long userId) {
        QuizResult result = quizResultMapper.selectById(resultId);
        if (result == null) throw new BusinessException("答题记录不存在");
        if (!result.getUserId().equals(userId)) throw new BusinessException("无权删除");
        quizResultMapper.deleteById(resultId);
    }

    public Map<String, Object> getResultById(Long resultId) {
        QuizResult result = quizResultMapper.selectById(resultId);
        if (result == null) throw new BusinessException("答题记录不存在");

        List<TestPaperAnswer> paperAnswers = answerMapper.selectList(
            new LambdaQueryWrapper<TestPaperAnswer>()
                .eq(TestPaperAnswer::getTestPaperId, result.getTestPaperId())
                .orderByAsc(TestPaperAnswer::getSort));

        Map<Long, List<String>> correctAnswerMap = new LinkedHashMap<>();
        Map<Long, TestPaperAnswer> answerInfoMap = new LinkedHashMap<>();
        for (TestPaperAnswer pa : paperAnswers) {
            correctAnswerMap.put(pa.getQuestionId(), parseAnswerLabels(pa.getCorrectAnswer()));
            answerInfoMap.put(pa.getQuestionId(), pa);
        }

        List<Question> questions = questionMapper.selectList(
            new LambdaQueryWrapper<Question>()
                .eq(Question::getDocumentId, result.getDocumentId())
                .eq(Question::getStatus, 1));

        List<Map<String, Object>> userAnswers = parseUserAnswers(result.getAnswersJson());
        Map<String, List<String>> userAnswerMap = toAnswerMap(userAnswers);

        // 批量加载选项
        List<Long> qIds = questions.stream().map(Question::getId).toList();
        Map<Long, List<QuestionOption>> optionMap = new HashMap<>();
        if (!qIds.isEmpty()) {
            List<QuestionOption> allOptions = optionMapper.selectList(
                new LambdaQueryWrapper<QuestionOption>()
                    .in(QuestionOption::getQuestionId, qIds)
                    .orderByAsc(QuestionOption::getSort));
            for (QuestionOption opt : allOptions) {
                optionMap.computeIfAbsent(opt.getQuestionId(), k -> new ArrayList<>()).add(opt);
            }
        }

        List<Map<String, Object>> details = new ArrayList<>();
        for (Question q : questions) {
            List<String> correctLabels = correctAnswerMap.getOrDefault(q.getId(), List.of());
            TestPaperAnswer answerInfo = answerInfoMap.get(q.getId());
            List<String> userAnswer = userAnswerMap.getOrDefault(q.getId().toString(), List.of());

            int earnedScore = 0;
            if (answerInfo != null) {
                earnedScore = gradeQuestion(q, correctLabels, userAnswer);
            }

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("questionId", q.getId().toString());
            detail.put("questionTitle", q.getQuestionTitle());
            detail.put("passage", q.getPassage());
            detail.put("questionType", q.getQuestionType());
            detail.put("userAnswer", userAnswer);
            detail.put("correctAnswer", correctLabels);
            detail.put("isCorrect", earnedScore > 0);
            detail.put("score", answerInfo != null ? answerInfo.getScore() : q.getScore());
            detail.put("earnedScore", earnedScore);
            detail.put("analysis", q.getAnalysis());

            List<QuestionOption> opts = optionMap.getOrDefault(q.getId(), List.of());
            List<Map<String, Object>> optList = new ArrayList<>();
            for (QuestionOption o : opts) {
                Map<String, Object> om = new LinkedHashMap<>();
                om.put("label", o.getOptionLabel());
                om.put("text", o.getOptionText());
                om.put("isCorrect", o.getIsCorrect() == 1);
                optList.add(om);
            }
            detail.put("options", optList);
            details.add(detail);
        }

        TestPaper paper = testPaperMapper.selectById(result.getTestPaperId());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("resultId", result.getId().toString());
        resp.put("testPaperId", String.valueOf(result.getTestPaperId()));
        resp.put("documentId", String.valueOf(result.getDocumentId()));
        resp.put("docName", paper != null ? paper.getPaperName() : "");
        resp.put("totalScore", result.getTotalScore());
        resp.put("userScore", result.getUserScore());
        resp.put("correctCount", result.getCorrectCount());
        resp.put("totalCount", result.getTotalCount());
        resp.put("durationSeconds", result.getDurationSeconds());
        resp.put("details", details);
        return resp;
    }

    private List<Map<String, Object>> parseUserAnswers(String answersJson) {
        if (answersJson == null || answersJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(answersJson, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
