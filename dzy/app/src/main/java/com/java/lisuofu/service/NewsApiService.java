package com.java.lisuofu.service;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.java.lisuofu.model.NewsItem;
import com.java.lisuofu.model.NewsResponse;
import okhttp3.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NewsApiService {
    private static final String TAG = "NewsApiService";
    private static final String BASE_URL = "https://api2.newsminer.net/svc/news/queryNewsList";

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final ExecutorService executorService;
    private final GLMService glmService;

    public NewsApiService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        this.gson = new GsonBuilder()
                .setLenient()
                .create();

        this.executorService = Executors.newFixedThreadPool(4);
        this.glmService = new GLMService();
    }

    /**
     * 获取新闻列表
     */
    public void getNewsList(NewsRequestParams params, NewsCallback callback) {
        CompletableFuture.supplyAsync(() -> {
            try {
                String url = buildUrl(params);
                Log.d(TAG, "请求URL: " + url);

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", "NewsApp/1.0")
                        .addHeader("Accept", "application/json")
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        Log.d(TAG, "API响应长度: " + responseBody.length());
                        Log.d(TAG, "API响应前500字符: " + responseBody.substring(0, Math.min(500, responseBody.length())));

                        NewsResponse newsResponse = parseNewsResponse(responseBody);

                        if (newsResponse != null && newsResponse.getData() != null) {
                            List<NewsItem> newsList = newsResponse.getData();
                            List<NewsItem> filteredNewsList = new ArrayList<>();
                            
                            for (int i = 0; i < newsList.size(); i++) {
                                NewsItem news = newsList.get(i);
                                processNewsItem(news, i);
                                
                                // 只保留非"其他"分类的新闻
                                if (!"其他".equals(news.getCategory())) {
                                    filteredNewsList.add(news);
                                }
                            }
                            
                            // 更新响应数据为过滤后的列表
                            newsResponse.setData(filteredNewsList);

                            Log.d(TAG, "成功解析新闻数量: " + newsList.size() + ", 过滤后: " + filteredNewsList.size());
                            return newsResponse;
                        } else {
                            Log.e(TAG, "解析响应失败或数据为空");
                            return null;
                        }
                    } else {
                        Log.e(TAG, "HTTP请求失败: " + response.code() + " " + response.message());
                        // 尝试获取错误响应内容
                        if (response.body() != null) {
                            String errorBody = response.body().string();
                            Log.e(TAG, "错误响应内容: " + errorBody);
                        }
                        return null;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "网络请求异常", e);
                return null;
            }
        }, executorService).thenAccept(result -> {
            if (result != null) {
                callback.onSuccess(result);
            } else {
                callback.onError("获取新闻数据失败");
            }
        });
    }

    /**
     * 构建请求URL - 使用正确的URL编码
     */
    private String buildUrl(NewsRequestParams params) {
        StringBuilder urlBuilder = new StringBuilder(BASE_URL);
        urlBuilder.append("?");

        boolean hasParam = false;

        if (params.getSize() > 0) {
            urlBuilder.append("size=").append(params.getSize());
            hasParam = true;
        }

        if (params.getStartDate() != null && !params.getStartDate().isEmpty()) {
            if (hasParam) urlBuilder.append("&");
            urlBuilder.append("startDate=").append(urlEncode(params.getStartDate()));
            hasParam = true;
        }

        if (params.getEndDate() != null && !params.getEndDate().isEmpty()) {
            if (hasParam) urlBuilder.append("&");
            urlBuilder.append("endDate=").append(urlEncode(params.getEndDate()));
            hasParam = true;
        }

        if (params.getWords() != null) {
            if (hasParam) urlBuilder.append("&");
            urlBuilder.append("words=").append(urlEncode(params.getWords()));
            hasParam = true;
        }

        if (params.getCategories() != null && !params.getCategories().isEmpty()) {
            if (hasParam) urlBuilder.append("&");
            urlBuilder.append("categories=").append(urlEncode(params.getCategories()));
            hasParam = true;
        }

        if (params.getPage() > 0) {
            if (hasParam) urlBuilder.append("&");
            urlBuilder.append("page=").append(params.getPage());
        }

        String finalUrl = urlBuilder.toString();
        Log.d(TAG, "构建的完整URL: " + finalUrl);
        return finalUrl;
    }

    /**
     * URL编码工具方法
     */
    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "URL编码失败: " + value, e);
            return value; // 如果编码失败，返回原值
        }
    }

    /**
     * 解析新闻响应数据
     */
    private NewsResponse parseNewsResponse(String responseBody) {
        try {
            NewsResponse newsResponse = gson.fromJson(responseBody, NewsResponse.class);

            if (newsResponse == null) {
                Log.e(TAG, "JSON解析失败，返回null");
                return null;
            }

            if (newsResponse.getData() == null) {
                Log.e(TAG, "新闻数据列表为空");
                return null;
            }

            return newsResponse;

        } catch (Exception e) {
            Log.e(TAG, "解析新闻响应失败", e);
            return null;
        }
    }

    /**
     * 处理单个新闻项目
     */
    private void processNewsItem(NewsItem news, int index) {
        try {
            if (news.getNewsId() == null || news.getNewsId().isEmpty()) {
                String tempId = "news_" + System.currentTimeMillis() + "_" + index;
                news.setNewsId(tempId);
                Log.d(TAG, "为新闻生成临时ID: " + tempId);
            }

            if (news.getTitle() == null) {
                news.setTitle("无标题");
            }

            if (news.getContent() == null) {
                news.setContent("");
            }

            if (news.getPublisher() == null) {
                news.setPublisher("未知来源");
            }

            if (news.getPublishTime() == null) {
                news.setPublishTime(getCurrentTime());
            }

            // 处理分类 - 规范化API返回的分类
            String category = news.getCategory();
            Log.d(TAG, "API返回分类: '" + category + "' (新闻: " + news.getTitle() + ")");
            
            if (category == null || category.trim().isEmpty()) {
                // 当API没有返回分类时，设置为"其他"
                news.setCategory("其他");
                Log.d(TAG, "API未提供分类，设置为'其他': " + news.getTitle());
            } else {
                // 规范化分类：只允许预定义的分类
                String normalizedCategory = normalizeCategory(category.trim());
                news.setCategory(normalizedCategory);
                Log.d(TAG, "分类规范化: " + news.getTitle() + " -> '" + category + "' -> '" + normalizedCategory + "'");
            }

            Log.d(TAG, "处理新闻: " + news.getTitle() + " (ID: " + news.getNewsId() + ", 分类: " + news.getCategory() + ")");

        } catch (Exception e) {
            Log.e(TAG, "处理新闻项目失败", e);
        }
    }

    /**
     * 规范化分类：将非预定义分类映射到"其他"
     */
    private String normalizeCategory(String category) {
        // 预定义的分类列表（与HomeFragment保持一致）
        String[] validCategories = {
            "全部", "娱乐", "军事", "教育", "文化", "健康", "财经", "体育", "汽车", "科技", "社会"
        };
        
        // 检查是否为有效分类
        for (String validCategory : validCategories) {
            if (validCategory.equals(category)) {
                return category; // 返回有效分类
            }
        }
        
        // 如果不是预定义分类，归类到"其他"
        Log.d(TAG, "非预定义分类 '" + category + "' 归类到'其他'");
        return "其他";
    }

    private String getCurrentTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return formatter.format(new Date());
    }

    private static String getCurrentDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return formatter.format(new Date());
    }

    /**
     * 获取默认的新闻参数 - 使用更广泛的时间范围
     */
    public static NewsRequestParams getDefaultTodayParams() {
        NewsRequestParams params = new NewsRequestParams();
        params.setSize(30);  // 与日志中的请求一致
        params.setPage(1);

        // 使用更广泛的时间范围，与日志中的请求一致
        params.setStartDate("2019-01-01");
        params.setEndDate(getCurrentDate());

        // 添加默认的words参数，使用一个通用的词
        params.setWords("新闻");

        return params;
    }

    /**
     * 获取分类新闻参数 - 增强版本
     */
    public static NewsRequestParams getCategoryParams(String category, int page) {
        NewsRequestParams params = getDefaultTodayParams();
        
        if (category != null && !category.isEmpty() && !"全部".equals(category)) {
            // 直接使用分类名作为搜索关键词
            params.setWords(category);
            params.setCategories(category);
            Log.d(TAG, "分类搜索参数: category=" + category + ", words=" + category);
        } else {
            // 全部分类使用默认参数
            Log.d(TAG, "全部分类参数: 使用默认参数");
        }
        
        params.setPage(page);
        return params;
    }
    
    /**
     * 获取搜索新闻参数 - 改进版本
     */
    public static NewsRequestParams getSearchParams(String keywords, String category, int page) {
        NewsRequestParams params = getDefaultTodayParams();
        if (keywords != null && !keywords.isEmpty()) {
            params.setWords(keywords);
        }
        if (category != null && !category.isEmpty() && !"全部".equals(category)) {
            params.setCategories(category);
        }
        params.setPage(page);
        return params;
    }

    /**
     * 获取带时间范围的新闻参数
     */
    public static NewsRequestParams getTimeRangeParams(String startDate, String endDate, int page) {
        NewsRequestParams params = new NewsRequestParams();
        params.setSize(30);  // 与默认搜索保持一致
        params.setPage(page);
        params.setStartDate(startDate);
        params.setEndDate(endDate);
        // 必须设置words参数为空字符串，API要求有这个参数
        params.setWords("");
        return params;
    }

    /**
     * 释放资源
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (glmService != null) {
            glmService.shutdown();
        }
    }

    // 回调接口
    public interface NewsCallback {
        void onSuccess(NewsResponse response);
        void onError(String error);
    }

    // 请求参数类
    public static class NewsRequestParams {
        private int size = 15;
        private String startDate;
        private String endDate;
        private String words;
        private String categories;
        private int page = 1;

        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }

        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }

        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }

        public String getWords() { return words; }
        public void setWords(String words) { this.words = words; }

        public String getCategories() { return categories; }
        public void setCategories(String categories) { this.categories = categories; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        @Override
        public String toString() {
            return "NewsRequestParams{" +
                    "size=" + size +
                    ", startDate='" + startDate + '\'' +
                    ", endDate='" + endDate + '\'' +
                    ", words='" + words + '\'' +
                    ", categories='" + categories + '\'' +
                    ", page=" + page +
                    '}';
        }
    }
}