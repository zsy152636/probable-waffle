package com.example.quiz.dome.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.quiz.dome.entity.*;
import com.example.quiz.dome.mapper.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StatsService {

    private final QuizResultMapper quizResultMapper;
    private final DocumentMapper documentMapper;
    private final TestPaperMapper testPaperMapper;

    public StatsService(QuizResultMapper quizResultMapper, DocumentMapper documentMapper,
                        TestPaperMapper testPaperMapper) {
        this.quizResultMapper = quizResultMapper;
        this.documentMapper = documentMapper;
        this.testPaperMapper = testPaperMapper;
    }

    public Map<String, Object> getOverview(Long userId) {
        // 基础数量统计
        long docCount = documentMapper.selectCount(new LambdaQueryWrapper<>());
        long paperCount = testPaperMapper.selectCount(new LambdaQueryWrapper<>());

        List<QuizResult> allResults = quizResultMapper.selectList(
            new LambdaQueryWrapper<QuizResult>()
                .eq(QuizResult::getUserId, userId)
                .orderByDesc(QuizResult::getCreateTime));

        int quizCount = allResults.size();
        int totalQuestions = 0;
        int totalCorrect = 0;
        int totalScoreSum = 0;
        int totalMaxScore = 0;

        for (QuizResult r : allResults) {
            totalQuestions += r.getTotalCount();
            totalCorrect += r.getCorrectCount();
            totalScoreSum += r.getUserScore();
            totalMaxScore += r.getTotalScore();
        }

        double accuracyRate = totalQuestions > 0
            ? Math.round(totalCorrect * 1000.0 / totalQuestions) / 10.0
            : 0.0;
        double avgScore = quizCount > 0
            ? Math.round(totalScoreSum * 10.0 / quizCount) / 10.0
            : 0.0;
        int avgDuration = quizCount > 0
            ? (int) allResults.stream().mapToInt(QuizResult::getDurationSeconds).average().orElse(0)
            : 0;

        // 分数段分布
        int[] ranges = new int[4]; // 0-59, 60-79, 80-89, 90-100
        for (QuizResult r : allResults) {
            double pct = r.getTotalScore() > 0
                ? r.getUserScore() * 100.0 / r.getTotalScore()
                : 0;
            if (pct < 60) ranges[0]++;
            else if (pct < 80) ranges[1]++;
            else if (pct < 90) ranges[2]++;
            else ranges[3]++;
        }
        List<Map<String, Object>> scoreDistribution = new ArrayList<>();
        String[] rangeLabels = {"不及格", "及格", "良好", "优秀"};
        String[] rangeKeys = {"0-59", "60-79", "80-89", "90-100"};
        String[] rangeColors = {"#f56c6c", "#e6a23c", "#409eff", "#67c23a"};
        for (int i = 0; i < 4; i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("range", rangeKeys[i]);
            item.put("label", rangeLabels[i]);
            item.put("count", ranges[i]);
            item.put("color", rangeColors[i]);
            scoreDistribution.add(item);
        }

        // 批量取试卷名
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

        // 近期答题记录（最近 10 条）
        List<Map<String, Object>> recentQuizzes = new ArrayList<>();
        int limit = Math.min(allResults.size(), 10);
        for (int i = 0; i < limit; i++) {
            QuizResult r = allResults.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("submitTime", r.getSubmitTime() != null ? r.getSubmitTime().toString() : "");
            item.put("userScore", r.getUserScore());
            item.put("totalScore", r.getTotalScore());
            item.put("docName", paperNameMap.getOrDefault(r.getTestPaperId(), ""));
            recentQuizzes.add(item);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("docCount", docCount);
        resp.put("paperCount", paperCount);
        resp.put("quizCount", quizCount);
        resp.put("totalQuestions", totalQuestions);
        resp.put("correctCount", totalCorrect);
        resp.put("accuracyRate", accuracyRate);
        resp.put("avgScore", avgScore);
        resp.put("avgDuration", avgDuration);
        resp.put("scoreDistribution", scoreDistribution);
        resp.put("recentQuizzes", recentQuizzes);
        return resp;
    }
}
