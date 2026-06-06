package com.example.quiz.ai.config;

import com.example.quiz.ai.entity.AIConfig;
import com.example.quiz.sys.common.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class AIClientFactory {

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String callAI(AIConfig config, String systemPrompt, String userMessage) {
        int maxTokens = config.getMaxTokens() != null ? config.getMaxTokens() : 4096;
        return callAI(config, systemPrompt, userMessage, maxTokens);
    }

    public String callAI(AIConfig config, String systemPrompt, String userMessage, int maxTokens) {
        return callAI(config, systemPrompt, userMessage, maxTokens, false);
    }

    public String callAI(AIConfig config, String systemPrompt, String userMessage, int maxTokens, boolean jsonMode) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", config.getModelName());
            body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
            ));
            body.put("max_tokens", maxTokens);
            BigDecimal temperature = config.getTemperature();
            body.put("temperature", temperature != null ? temperature : BigDecimal.valueOf(0.7));
            if (jsonMode) {
                body.put("response_format", Map.of("type", "json_object"));
            }

            Request request = new Request.Builder()
                    .url(config.getApiUrl())
                    .post(RequestBody.create(
                        objectMapper.writeValueAsString(body),
                        MediaType.parse("application/json")))
                    .header("Authorization", "Bearer " + config.getApiToken())
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    throw new BusinessException("AI 调用失败: HTTP " + response.code() + " " + errorBody);
                }
                JsonNode root = objectMapper.readTree(response.body().string());
                return root.path("choices").get(0)
                           .path("message").path("content").asText();
            }
        } catch (IOException e) {
            throw new BusinessException("AI 调用网络异常: " + e.getMessage());
        }
    }

    /**
     * 测试连通性，发送简单请求并返回模型回复内容；失败抛异常
     */
    public String testConnection(AIConfig config) throws JsonProcessingException {
        Map<String, Object> body = new HashMap<>();
        body.put("model", config.getModelName());
        body.put("messages", List.of(
            Map.of("role", "user", "content", "你好，请回复'连接测试成功'这六个字")
        ));
        body.put("max_tokens", 50);

        Request request = new Request.Builder()
                .url(config.getApiUrl())
                .post(RequestBody.create(
                    objectMapper.writeValueAsString(body),
                    MediaType.parse("application/json")))
                .header("Authorization", "Bearer " + config.getApiToken())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new BusinessException("HTTP " + response.code() + ": " + errorBody);
            }
            JsonNode root = objectMapper.readTree(response.body().string());
            return root.path("choices").get(0)
                       .path("message").path("content").asText();
        } catch (IOException e) {
            throw new BusinessException("网络异常: " + e.getMessage());
        }
    }
}
