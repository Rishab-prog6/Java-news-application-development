package com.java.lisuofu.ui.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class FavoriteFragment extends Fragment {

    private static final String TAG = "FavoriteFragment";
    private static final String PREF_NAME = "news_prefs";
    private static final String KEY_FAVORITES = "favorites";
    private static final String KEY_FAVORITE_NEWS_DATA = "favorite_news_data";

    // UI组件
    private RecyclerView recyclerView;
    private LinearLayout emptyLayout;
    private TextView favoriteCountText;
    private NewsAdapter newsAdapter;

    // 数据
    private SharedPreferences preferences;
    private Gson gson;
    private List<NewsItem> favoriteNewsList = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = requireContext().getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        gson = new Gson();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorite, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerView();
        loadFavoriteNews();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次回到页面时刷新数据，因为可能有新的收藏或取消收藏
        loadFavoriteNews();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_favorites);
        emptyLayout = view.findViewById(R.id.layout_empty);
        favoriteCountText = view.findViewById(R.id.tv_favorite_count);
    }

    private void setupRecyclerView() {
        newsAdapter = new NewsAdapter(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(newsAdapter);

        // 设置点击监听
        newsAdapter.setOnNewsItemClickListener(new NewsAdapter.OnNewsItemClickListener() {
            @Override
            public void onNewsClick(NewsItem newsItem) {
                // 打开新闻详情页
                NewsDetailActivity.start(requireContext(), newsItem);
            }
        });
    }

    /**
     * 加载收藏的新闻
     */
    private void loadFavoriteNews() {
        List<String> favoriteIds = getFavoritesList();
        List<NewsItem> savedFavoriteNews = getSavedFavoriteNewsData();

        favoriteNewsList.clear();

        // 根据收藏ID列表，获取对应的新闻数据
        for (String newsId : favoriteIds) {
            NewsItem newsItem = findNewsById(savedFavoriteNews, newsId);
            if (newsItem != null) {
                newsItem.setFavorite(true); // 确保收藏状态正确
                favoriteNewsList.add(newsItem);
            }
        }

        // 更新UI
        updateUI();

        Log.d(TAG, "加载收藏新闻数量: " + favoriteNewsList.size());
    }

    /**
     * 更新UI显示
     */
    private void updateUI() {
        if (favoriteNewsList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
            favoriteCountText.setText("0篇");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);
            favoriteCountText.setText(favoriteNewsList.size() + "篇");

            newsAdapter.setNewsList(favoriteNewsList);
        }
    }

    /**
     * 根据ID查找新闻
     */
    private NewsItem findNewsById(List<NewsItem> newsList, String newsId) {
        for (NewsItem newsItem : newsList) {
            if (newsId.equals(newsItem.getNewsId())) {
                return newsItem;
            }
        }
        return null;
    }

    // ========== SharedPreferences 相关方法 ==========

    /**
     * 获取收藏列表ID
     */
    private List<String> getFavoritesList() {
        String json = preferences.getString(KEY_FAVORITES, "[]");
        Type type = new TypeToken<List<String>>(){}.getType();
        List<String> favorites = gson.fromJson(json, type);
        return favorites != null ? favorites : new ArrayList<>();
    }

    /**
     * 保存收藏列表ID
     */
    private void saveFavoritesList(List<String> favorites) {
        String json = gson.toJson(favorites);
        preferences.edit().putString(KEY_FAVORITES, json).apply();
    }

    /**
     * 获取收藏的新闻完整数据
     */
    private List<NewsItem> getSavedFavoriteNewsData() {
        String json = preferences.getString(KEY_FAVORITE_NEWS_DATA, "[]");
        Type type = new TypeToken<List<NewsItem>>(){}.getType();
        List<NewsItem> favoriteNews = gson.fromJson(json, type);
        return favoriteNews != null ? favoriteNews : new ArrayList<>();
    }

    /**
     * 保存收藏的新闻完整数据
     */
    private void saveFavoriteNewsData(List<NewsItem> favoriteNews) {
        String json = gson.toJson(favoriteNews);
        preferences.edit().putString(KEY_FAVORITE_NEWS_DATA, json).apply();
    }

    // ========== 静态方法，供其他地方调用 ==========

    /**
     * 添加收藏（供其他Fragment或Activity调用）
     */
    public static void addFavorite(android.content.Context context, NewsItem newsItem) {
        if (newsItem == null || newsItem.getNewsId() == null) return;

        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        Gson gson = new Gson();

        // 更新收藏ID列表
        String favoritesJson = preferences.getString(KEY_FAVORITES, "[]");
        Type listType = new TypeToken<List<String>>(){}.getType();
        List<String> favorites = gson.fromJson(favoritesJson, listType);
        if (favorites == null) favorites = new ArrayList<>();

        if (!favorites.contains(newsItem.getNewsId())) {
            favorites.add(newsItem.getNewsId());

            // 保存收藏ID列表
            String updatedFavoritesJson = gson.toJson(favorites);
            preferences.edit().putString(KEY_FAVORITES, updatedFavoritesJson).apply();

            // 更新收藏新闻数据
            String newsDataJson = preferences.getString(KEY_FAVORITE_NEWS_DATA, "[]");
            Type newsListType = new TypeToken<List<NewsItem>>(){}.getType();
            List<NewsItem> favoriteNewsData = gson.fromJson(newsDataJson, newsListType);
            if (favoriteNewsData == null) favoriteNewsData = new ArrayList<>();

            // 检查是否已存在，不存在则添加
            boolean exists = false;
            for (NewsItem item : favoriteNewsData) {
                if (newsItem.getNewsId().equals(item.getNewsId())) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                favoriteNewsData.add(newsItem);
                String updatedNewsDataJson = gson.toJson(favoriteNewsData);
                preferences.edit().putString(KEY_FAVORITE_NEWS_DATA, updatedNewsDataJson).apply();
            }

            Log.d(TAG, "添加收藏: " + newsItem.getTitle());
        }
    }

    /**
     * 移除收藏（供其他Fragment或Activity调用）
     */
    public static void removeFavorite(android.content.Context context, NewsItem newsItem) {
        if (newsItem == null || newsItem.getNewsId() == null) return;

        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        Gson gson = new Gson();

        // 更新收藏ID列表
        String favoritesJson = preferences.getString(KEY_FAVORITES, "[]");
        Type listType = new TypeToken<List<String>>(){}.getType();
        List<String> favorites = gson.fromJson(favoritesJson, listType);
        if (favorites == null) favorites = new ArrayList<>();

        favorites.remove(newsItem.getNewsId());

        // 保存收藏ID列表
        String updatedFavoritesJson = gson.toJson(favorites);
        preferences.edit().putString(KEY_FAVORITES, updatedFavoritesJson).apply();

        // 更新收藏新闻数据
        String newsDataJson = preferences.getString(KEY_FAVORITE_NEWS_DATA, "[]");
        Type newsListType = new TypeToken<List<NewsItem>>(){}.getType();
        List<NewsItem> favoriteNewsData = gson.fromJson(newsDataJson, newsListType);
        if (favoriteNewsData == null) favoriteNewsData = new ArrayList<>();

        favoriteNewsData.removeIf(item -> newsItem.getNewsId().equals(item.getNewsId()));

        String updatedNewsDataJson = gson.toJson(favoriteNewsData);
        preferences.edit().putString(KEY_FAVORITE_NEWS_DATA, updatedNewsDataJson).apply();

        Log.d(TAG, "移除收藏: " + newsItem.getTitle());
    }

    /**
     * 检查是否已收藏
     */
    public static boolean isFavorite(android.content.Context context, String newsId) {
        if (newsId == null) return false;

        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        Gson gson = new Gson();

        String favoritesJson = preferences.getString(KEY_FAVORITES, "[]");
        Type listType = new TypeToken<List<String>>(){}.getType();
        List<String> favorites = gson.fromJson(favoritesJson, listType);

        return favorites != null && favorites.contains(newsId);
    }
}