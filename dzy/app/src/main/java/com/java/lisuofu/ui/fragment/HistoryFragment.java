package com.java.lisuofu.ui.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.java.lisuofu.R;
import com.java.lisuofu.model.NewsItem;
import com.java.lisuofu.ui.activity.NewsDetailActivity;
import com.java.lisuofu.ui.adapter.NewsAdapter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class HistoryFragment extends Fragment {

    private static final String TAG = "HistoryFragment";
    private static final String PREF_NAME = "news_prefs";
    private static final String KEY_READ_NEWS = "read_news";
    private static final String KEY_READ_NEWS_DATA = "read_news_data";
    private static final String KEY_READ_TIMES = "read_times";

    // UI组件
    private RecyclerView recyclerView;
    private LinearLayout emptyLayout;
    private NewsAdapter newsAdapter;

    // 数据
    private SharedPreferences preferences;
    private Gson gson;
    private List<HistoryItem> historyList = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = requireContext().getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        gson = new Gson();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerView();
        loadReadHistory();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadReadHistory();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_history);
        emptyLayout = view.findViewById(R.id.layout_empty);
    }

    private void setupRecyclerView() {
        newsAdapter = new NewsAdapter(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(newsAdapter);

        newsAdapter.setOnNewsItemClickListener(new NewsAdapter.OnNewsItemClickListener() {
            @Override
            public void onNewsClick(NewsItem newsItem) {
                NewsDetailActivity.start(requireContext(), newsItem);
            }
        });
    }

    private void loadReadHistory() {
        List<String> readNewsIds = getReadNewsList();
        List<NewsItem> savedReadNews = getSavedReadNewsData();
        java.util.Map<String, Long> readTimes = getReadTimes();

        historyList.clear();

        for (String newsId : readNewsIds) {
            NewsItem newsItem = findNewsById(savedReadNews, newsId);
            if (newsItem != null) {
                Long readTime = readTimes.get(newsId);
                HistoryItem historyItem = new HistoryItem(newsItem, readTime != null ? readTime : System.currentTimeMillis());
                historyList.add(historyItem);
            }
        }

        Collections.sort(historyList, (a, b) -> Long.compare(b.readTime, a.readTime));
        updateUI();

        Log.d(TAG, "加载阅读历史数量: " + historyList.size());
    }

    private void updateUI() {
        if (historyList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);

            List<NewsItem> newsItems = new ArrayList<>();
            for (HistoryItem historyItem : historyList) {
                NewsItem newsItem = historyItem.newsItem;
                newsItem.setFavorite(FavoriteFragment.isFavorite(requireContext(), newsItem.getNewsId()));
                newsItems.add(newsItem);
            }

            newsAdapter.setNewsList(newsItems);
        }
    }

    private NewsItem findNewsById(List<NewsItem> newsList, String newsId) {
        for (NewsItem newsItem : newsList) {
            if (newsId.equals(newsItem.getNewsId())) {
                return newsItem;
            }
        }
        return null;
    }

    private List<String> getReadNewsList() {
        String json = preferences.getString(KEY_READ_NEWS, "[]");
        Type type = new TypeToken<List<String>>(){}.getType();
        List<String> readNews = gson.fromJson(json, type);
        return readNews != null ? readNews : new ArrayList<>();
    }

    private List<NewsItem> getSavedReadNewsData() {
        String json = preferences.getString(KEY_READ_NEWS_DATA, "[]");
        Type type = new TypeToken<List<NewsItem>>(){}.getType();
        List<NewsItem> readNews = gson.fromJson(json, type);
        return readNews != null ? readNews : new ArrayList<>();
    }

    private java.util.Map<String, Long> getReadTimes() {
        String json = preferences.getString(KEY_READ_TIMES, "{}");
        Type type = new TypeToken<java.util.Map<String, Long>>(){}.getType();
        java.util.Map<String, Long> readTimes = gson.fromJson(json, type);
        return readTimes != null ? readTimes : new java.util.HashMap<>();
    }

    // ========== 静态方法供外部调用 ==========

    public static void addReadRecord(android.content.Context context, NewsItem newsItem) {
        if (newsItem == null || newsItem.getNewsId() == null) return;

        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        Gson gson = new Gson();
        long currentTime = System.currentTimeMillis();

        String readNewsJson = preferences.getString(KEY_READ_NEWS, "[]");
        Type listType = new TypeToken<List<String>>(){}.getType();
        List<String> readNews = gson.fromJson(readNewsJson, listType);
        if (readNews == null) readNews = new ArrayList<>();

        readNews.remove(newsItem.getNewsId());
        readNews.add(0, newsItem.getNewsId());

        if (readNews.size() > 100) {
            readNews = readNews.subList(0, 100);
        }

        String updatedReadNewsJson = gson.toJson(readNews);
        preferences.edit().putString(KEY_READ_NEWS, updatedReadNewsJson).apply();

        String newsDataJson = preferences.getString(KEY_READ_NEWS_DATA, "[]");
        Type newsListType = new TypeToken<List<NewsItem>>(){}.getType();
        List<NewsItem> readNewsData = gson.fromJson(newsDataJson, newsListType);
        if (readNewsData == null) readNewsData = new ArrayList<>();

        readNewsData.removeIf(item -> newsItem.getNewsId().equals(item.getNewsId()));
        readNewsData.add(0, newsItem);

        if (readNewsData.size() > 100) {
            readNewsData = readNewsData.subList(0, 100);
        }

        String updatedNewsDataJson = gson.toJson(readNewsData);
        preferences.edit().putString(KEY_READ_NEWS_DATA, updatedNewsDataJson).apply();

        String readTimesJson = preferences.getString(KEY_READ_TIMES, "{}");
        Type mapType = new TypeToken<java.util.Map<String, Long>>(){}.getType();
        java.util.Map<String, Long> readTimes = gson.fromJson(readTimesJson, mapType);
        if (readTimes == null) readTimes = new java.util.HashMap<>();

        readTimes.put(newsItem.getNewsId(), currentTime);

        String updatedReadTimesJson = gson.toJson(readTimes);
        preferences.edit().putString(KEY_READ_TIMES, updatedReadTimesJson).apply();

        Log.d(TAG, "添加阅读记录: " + newsItem.getTitle());
    }

    public static boolean isRead(android.content.Context context, String newsId) {
        if (newsId == null) return false;

        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        Gson gson = new Gson();

        String readNewsJson = preferences.getString(KEY_READ_NEWS, "[]");
        Type listType = new TypeToken<List<String>>(){}.getType();
        List<String> readNews = gson.fromJson(readNewsJson, listType);

        return readNews != null && readNews.contains(newsId);
    }

    private static class HistoryItem {
        NewsItem newsItem;
        long readTime;

        HistoryItem(NewsItem newsItem, long readTime) {
            this.newsItem = newsItem;
            this.readTime = readTime;
        }
    }
}