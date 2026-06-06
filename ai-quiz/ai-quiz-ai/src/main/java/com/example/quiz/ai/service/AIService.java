package com.example.quiz.ai.service;

import com.example.quiz.ai.config.AIClientFactory;
import com.example.quiz.ai.entity.AIConfig;
import com.example.quiz.sys.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class AIService {

    private final AIClientFactory clientFactory;
    private final AIConfigService aiConfigService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String PROMPT_TEMPLATE = """
            你是一个专业的考试出题专家。请根据以下文档内容，生成一套高质量的题目。

            【题目要求】
            - 总题数：%d 道
            - 题型分布：
            %s
            %s
            【出题规范】
            1. 题目覆盖文档的核心知识点，避免重复考点
            2. 每道题标注难度：1-简单 2-中等 3-困难，三种难度均匀分布
            3. 每道题包含答案解析
            4. 单选题和多选题均需4个选项(A/B/C/D)，并标注正确答案
            5. 判断题必须有2个选项："正确"和"错误"，isCorrect 标注正确答案
            6. 填空题必须有正确选项，label 为"答案"
            7. 每道题默认分值 5 分
            8. 关键：每一道题都必须包含 "options" 字段（数组），不能省略
            9. 如果出题方向要求"阅读理解"或"文章题"，请在 passage 字段中提供约200字的阅读材料
            10. 只生成上述题型分布中列出的题型，不要生成任何未列出的题型
            11. 返回严格的 JSON 格式，不要包含任何其他文字

            返回 JSON 格式如下（注意每道题都必须有 options 字段，passage 为选填）：
            {
              "questions": [
                {
                  "questionType": "SINGLE",
                  "questionTitle": "题干内容",
                  "passage": "约200字的阅读材料（仅阅读理解题需要，非阅读题可省略此字段）",
                  "options": [
                    { "label": "A", "text": "选项A", "isCorrect": false },
                    { "label": "B", "text": "选项B", "isCorrect": true },
                    { "label": "C", "text": "选项C", "isCorrect": false },
                    { "label": "D", "text": "选项D", "isCorrect": false }
                  ],
                  "analysis": "答案解析",
                  "difficulty": 1,
                  "score": 5
                },
                {
                  "questionType": "JUDGE",
                  "questionTitle": "判断题示例",
                  "options": [
                    { "label": "A", "text": "正确", "isCorrect": true },
                    { "label": "B", "text": "错误", "isCorrect": false }
                  ],
                  "analysis": "答案解析",
                  "difficulty": 1,
                  "score": 5
                }
              ]
            }
            """;

    public AIService(AIClientFactory clientFactory, AIConfigService aiConfigService) {
        this.clientFactory = clientFactory;
        this.aiConfigService = aiConfigService;
    }

    /**
     * 根据配置动态构建 Prompt 并调用 AI 生成题目
     * @return AI 返回的 JSON 字符串
     */
    public String generateQuestions(String contentText, int totalCount,
                                    boolean includeFill, Map<String, Integer> typeRatios,
                                    String direction) {
        AIConfig aiConfig = aiConfigService.getActive();

        int singlePct = typeRatios.getOrDefault("single", 50);
        int multiPct  = typeRatios.getOrDefault("multi", 30);
        int judgePct  = typeRatios.getOrDefault("judge", 20);

        int singleCount = totalCount * singlePct / 100;
        int multiCount  = totalCount * multiPct  / 100;
        int judgeCount  = totalCount * judgePct  / 100;

        // 余数归入单选
        int remainder = totalCount - singleCount - multiCount - judgeCount;
        singleCount += remainder;

        String fillLine = "";
        if (includeFill) {
            int fillCount = totalCount / 10;
            if (fillCount < 1) fillCount = 1;
            int deductEach = fillCount / 3;
            singleCount -= deductEach;
            multiCount  -= deductEach;
            judgeCount  -= (fillCount - deductEach * 2);
            fillLine = String.format("填空题(FILL)：%d 道", fillCount);
        }

        // 动态构建题型分布行，只包含数量 > 0 的题型
        StringBuilder typeDist = new StringBuilder();
        if (singleCount > 0) {
            typeDist.append(String.format("  单选题(SINGLE)：%d 道（%d%%）\n", singleCount, singlePct));
        }
        if (multiCount > 0) {
            typeDist.append(String.format("  多选题(MULTI)：%d 道（%d%%）\n", multiCount, multiPct));
        }
        if (judgeCount > 0) {
            typeDist.append(String.format("  判断题(JUDGE)：%d 道（%d%%）\n", judgeCount, judgePct));
        }
        if (!fillLine.isEmpty()) {
            typeDist.append("  ").append(fillLine).append("\n");
        }

        String directionLine = "";
        if (direction != null && !direction.isBlank()) {
            directionLine = "【出题方向】\n" + direction.strip() + "\n";
        }

        String systemPrompt = String.format(PROMPT_TEMPLATE,
                totalCount,
                typeDist.toString(),
                directionLine
        );

        // 根据题目数量动态计算 max_tokens，防止输出截断
        int maxTokens = Math.max(configMaxTokens(aiConfig), totalCount * 600 + 1200);

        // 启用 JSON 模式，强制 AI 返回合法 JSON
        String rawResponse = clientFactory.callAI(aiConfig, systemPrompt, contentText, maxTokens, true);
        String json = extractJson(rawResponse);

        // 预校验 JSON 结构完整性
        validateQuestionJson(json);

        return json;
    }

    private void validateQuestionJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode questions = root.path("questions");
            if (!questions.isArray() || questions.isEmpty()) {
                throw new BusinessException("AI 返回的 JSON 中缺少 questions 数组或为空");
            }
            for (int i = 0; i < questions.size(); i++) {
                JsonNode q = questions.get(i);
                if (q.path("questionType").asText().isBlank()) {
                    throw new BusinessException(String.format("第 %d 道题缺少 questionType 字段", i + 1));
                }
                if (q.path("questionTitle").asText().isBlank()) {
                    throw new BusinessException(String.format("第 %d 道题缺少 questionTitle 字段", i + 1));
                }
                JsonNode opts = q.path("options");
                if (!opts.isArray() || opts.isEmpty()) {
                    throw new BusinessException(String.format(
                        "第 %d 道题「%s」缺少 options 选项数据，请重新生成",
                        i + 1, q.path("questionTitle").asText("未命名")));
                }
                for (int j = 0; j < opts.size(); j++) {
                    JsonNode opt = opts.get(j);
                    if (opt.path("label").asText().isBlank()) {
                        throw new BusinessException(String.format(
                            "第 %d 道题第 %d 个选项缺少 label", i + 1, j + 1));
                    }
                    if (opt.path("text").asText().isBlank()) {
                        throw new BusinessException(String.format(
                            "第 %d 道题第 %d 个选项缺少 text", i + 1, j + 1));
                    }
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("AI 返回的 JSON 格式无效: " + e.getMessage());
        }
    }

    private int configMaxTokens(AIConfig config) {
        return config.getMaxTokens() != null ? config.getMaxTokens() : 4096;
    }

    /**
     * 简答题 AI 评分
     */
    public int scoreShortAnswer(String referenceAnswer, String userAnswer, int maxScore) {
        AIConfig aiConfig = aiConfigService.getActive();
        String prompt = String.format("""
                你是一个严格的阅卷老师。请根据以下信息评判简答题。

                参考答案：%s
                用户答案：%s
                本题满分：%d 分

                评判标准：
                - 语义与参考答案高度一致：给满分
                - 语义部分正确但不够完整：给一半分
                - 语义错误或完全无关：给 0 分

                请只返回一个 0 到 %d 之间的整数分数，不要返回任何其他文字。
                """, referenceAnswer, userAnswer, maxScore, maxScore);

        String response = clientFactory.callAI(aiConfig, "你是一个严格的阅卷老师。", prompt);
        try {
            return Integer.parseInt(response.trim());
        } catch (NumberFormatException e) {
            return maxScore / 2;
        }
    }

    private String extractJson(String raw) {
        raw = raw.trim();
        // 移除 markdown 代码块标记
        if (raw.startsWith("```")) {
            int start = raw.indexOf("\n") + 1;
            int end = raw.lastIndexOf("```");
            if (end > start) {
                raw = raw.substring(start, end).trim();
            }
        }
        // 如果被引号包裹，去掉外层引号
        if (raw.startsWith("\"") && raw.endsWith("\"")) {
            raw = raw.substring(1, raw.length() - 1);
        }
        // 找到首尾大括号，去除 AI 多余的说明文字
        int firstBrace = raw.indexOf('{');
        int lastBrace = raw.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            raw = raw.substring(firstBrace, lastBrace + 1);
        }
        return raw;
    }
}
