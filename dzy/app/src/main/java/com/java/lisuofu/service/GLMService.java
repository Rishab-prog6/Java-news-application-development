package com.java.lisuofu.service;

import android.util.Log;
import com.java.lisuofu.model.NewsItem;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import okhttp3.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GLMService {
    private static final String TAG = "GLMService";

    // 您的API密钥
    private static final String API_KEY = "9400acb98cbe4a8998391b74a44e807b.layYR3qEQYYX8XPL";
    private static final String API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final ExecutorService executorService;

    public GLMService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
        this.executorService = Executors.newFixedThreadPool(3);
    }

    /**
     * 生成新闻摘要
     */
    public void generateNewsSummary(String newsContent, SummaryCallback callback) {
        CompletableFuture.supplyAsync(() -> {
            try {
                String limitedContent = limitContentLength(newsContent, 2000);
                String prompt = String.format(
                        "请为以下新闻内容生成一个简洁明了的摘要，要求：\n" +
                                "1. 控制在80-120字以内\n" +
                                "2. 突出关键信息和要点\n" +
                                "3. 语言简洁流畅\n\n" +
                                "新闻内容：\n%s",
                        limitedContent
                );

                return callGLMAPI(prompt);
            } catch (Exception e) {
                Log.e(TAG, "生成新闻摘要失败", e);
                return null;
            }
        }, executorService).thenAccept(summary -> {
            if (summary != null && !summary.trim().isEmpty()) {
                callback.onSuccess(cleanResult(summary));
            } else {
                callback.onError("生成摘要失败，请重试");
            }
        });
    }

    /**
     * 调用GLM API
     */
    private String callGLMAPI(String prompt) throws IOException {
        // 构建请求体
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "glm-4");
        requestBody.addProperty("stream", false);

        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);
        requestBody.add("messages", messages);

        // 创建请求
        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        // 发送请求
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                Log.d(TAG, "GLM API响应: " + responseBody);

                return parseGLMResponse(responseBody);
            } else {
                Log.e(TAG, "GLM API请求失败: " + response.code() + " " + response.message());
                throw new IOException("GLM API请求失败: " + response.code());
            }
        }
    }

    /**
     * 解析GLM API响应
     */
    private String parseGLMResponse(String responseBody) {
        try {
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

            if (jsonResponse.has("choices")) {
                JsonArray choices = jsonResponse.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    JsonObject firstChoice = choices.get(0).getAsJsonObject();
                    if (firstChoice.has("message")) {
                        JsonObject message = firstChoice.getAsJsonObject("message");
                        if (message.has("content")) {
                            return message.get("content").getAsString();
                        }
                    }
                }
            }

            Log.e(TAG, "GLM响应格式异常");
            return null;

        } catch (Exception e) {
            Log.e(TAG, "解析GLM响应失败", e);
            return null;
        }
    }

    /**
     * 限制内容长度
     */
    private String limitContentLength(String content, int maxLength) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        if (content.length() <= maxLength) {
            return content;
        }

        String truncated = content.substring(0, maxLength);
        int lastPeriod = truncated.lastIndexOf('。');
        if (lastPeriod > maxLength * 0.7) {
            truncated = truncated.substring(0, lastPeriod + 1);
        }

        return truncated + "...";
    }

    /**
     * 清理结果
     */
    private String cleanResult(String result) {
        if (result == null) return "";

        result = result.replaceAll("^(摘要：|总结：|概述：)", "").trim();
        result = result.replaceAll("\\s+", " ").trim();

        return result;
    }

    /**
     * 释放资源
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    // 回调接口
    public interface SummaryCallback {
        void onSuccess(String summary);
        void onError(String error);
    }
}