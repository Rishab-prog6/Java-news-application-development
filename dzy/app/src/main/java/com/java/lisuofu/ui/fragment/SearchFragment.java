package com.java.lisuofu.ui.fragment;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.java.lisuofu.R;
import com.java.lisuofu.ui.activity.NewsDetailActivity;
import com.java.lisuofu.ui.adapter.NewsAdapter;
import com.java.lisuofu.model.NewsItem;
import com.java.lisuofu.model.NewsResponse;
import com.java.lisuofu.service.NewsApiService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SearchFragment extends Fragment {
    private static final String TAG = "SearchFragment";

    private View rootView;
    private EditText searchEditText;
    private ImageView searchButton;
    private RecyclerView searchResultRecyclerView;
    private TextView emptyTextView;
    
    private NewsAdapter newsAdapter;
    private List<NewsItem> searchResults;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_search, container, false);
        
        // 初始化数据
        searchResults = new ArrayList<>();
        
        initViews();
        setupRecyclerView();
        setupListeners();
        return rootView;
    }

    private void initViews() {
        searchEditText = rootView.findViewById(R.id.search_edit_text);
        searchButton = rootView.findViewById(R.id.search_button);
        searchResultRecyclerView = rootView.findViewById(R.id.search_result_recycler_view);
        emptyTextView = rootView.findViewById(R.id.empty_text_view);
    }
    
    private void setupRecyclerView() {
        newsAdapter = new NewsAdapter(getContext());
        searchResultRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        searchResultRecyclerView.setAdapter(newsAdapter);
        
        // 设置新闻项点击监听器
        newsAdapter.setOnNewsItemClickListener(new NewsAdapter.OnNewsItemClickListener() {
            @Override
            public void onNewsClick(NewsItem newsItem) {
                // 打开新闻详情页面
                Intent intent = new Intent(getContext(), NewsDetailActivity.class);
                intent.putExtra("news_item", newsItem);
                startActivity(intent);
            }
        });
        
        // 初始显示空状态
        updateEmptyState();
    }
    
    private void updateEmptyState() {
        if (searchResults.isEmpty()) {
            searchResultRecyclerView.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.VISIBLE);
            
            // 根据搜索框的状态显示不同的提示文本
            String query = searchEditText.getText().toString().trim();
            if (query.isEmpty()) {
                emptyTextView.setText("输入关键词开始搜索");
            } else {
                emptyTextView.setText("未找到相关新闻");
            }
        } else {
            searchResultRecyclerView.setVisibility(View.VISIBLE);
            emptyTextView.setVisibility(View.GONE);
        }
    }
    
    /**
     * 重置到默认状态
     */
    private void resetToDefaultState() {
        Log.d(TAG, "重置搜索页面到默认状态");
        
        // 清空搜索结果
        searchResults.clear();
        newsAdapter.setNewsList(searchResults);
        
        // 显示默认的空状态文本
        searchResultRecyclerView.setVisibility(View.GONE);
        emptyTextView.setVisibility(View.VISIBLE);
        emptyTextView.setText("输入关键词开始搜索");
    }

    /**
     * 搜索按钮动画效果
     */
    private void animateSearchButton() {
        // 1. 创建X轴缩放动画
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(searchButton, "scaleX", 1.0f, 0.9f, 1.0f);

        // 2. 创建Y轴缩放动画  
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(searchButton, "scaleY", 1.0f, 0.9f, 1.0f);

        // 3. 组合动画同时执行
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);

        // 4. 设置动画时长并启动
        animatorSet.setDuration(150);
        animatorSet.start();
    }

    private void setupListeners() {
        // 搜索按钮点击事件
        searchButton.setOnClickListener(v -> {
            // 添加点击动画效果
            animateSearchButton();
            
            String query = searchEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                performSearch(query);
            } else {
                Toast.makeText(getContext(), "请输入搜索关键词", Toast.LENGTH_SHORT).show();
            }
        });

        // 搜索框回车事件
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String query = searchEditText.getText().toString().trim();
                if (!query.isEmpty()) {
                    performSearch(query);
                } else {
                    Toast.makeText(getContext(), "请输入搜索关键词", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });

        // 搜索框文本变化监听 - 当删除搜索内容时回到默认状态
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 不需要处理
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 不需要处理
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    // 当搜索框为空时，回到默认状态
                    resetToDefaultState();
                }
            }
        });
    }

    private void performSearch(String query) {
        Log.d(TAG, "开始API搜索: " + query);
        
        // 显示搜索中状态
        searchResults.clear();
        newsAdapter.setNewsList(searchResults);
        searchResultRecyclerView.setVisibility(View.GONE);
        emptyTextView.setVisibility(View.VISIBLE);
        emptyTextView.setText("搜索中...");
        
        NewsApiService newsApiService = new NewsApiService();
        
        // 进行API关键词搜索
        NewsApiService.NewsRequestParams searchParams = NewsApiService.getSearchParams(query, null, 1);
        newsApiService.getNewsList(searchParams, new NewsApiService.NewsCallback() {
            @Override
            public void onSuccess(NewsResponse response) {
                if (response != null && response.getData() != null) {
                    Log.d(TAG, "API搜索找到 " + response.getData().size() + " 条结果");
                    
                    // 按相关度排序
                    List<NewsItem> sortedResults = sortByRelevanceScore(response.getData(), query);
                    
                    // 更新UI
                    updateSearchResults(sortedResults, query);
                } else {
                    // 没有搜索结果
                    updateSearchResults(new ArrayList<>(), query);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "API搜索失败: " + error);
                // 搜索失败，显示空结果
                updateSearchResults(new ArrayList<>(), query);
            }
        });
    }
    
    /**
     * 更新搜索结果UI
     */
    private void updateSearchResults(List<NewsItem> results, String query) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                searchResults.clear();
                searchResults.addAll(results);
                newsAdapter.setNewsList(searchResults);
                updateEmptyState();
                
                Log.d(TAG, "最终搜索完成，共找到 " + results.size() + " 条结果");
                
                if (results.isEmpty()) {
                    Toast.makeText(getContext(), "未找到包含 \"" + query + "\" 的相关新闻", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    /**
     * 根据相关度对搜索结果排序 - 改进版本
     */
    private List<NewsItem> sortByRelevanceScore(List<NewsItem> newsItems, String searchQuery) {
        List<NewsItem> sortedList = new ArrayList<>(newsItems);
        
        // 计算每个新闻项的相关度得分
        for (NewsItem item : sortedList) {
            double relevanceScore = calculateRelevanceScore(item, searchQuery);
            item.setTotalKeywordScore(relevanceScore);
        }
        
        // 按相关度降序排序
        Collections.sort(sortedList, new Comparator<NewsItem>() {
            @Override
            public int compare(NewsItem o1, NewsItem o2) {
                return Double.compare(o2.getTotalKeywordScore(), o1.getTotalKeywordScore());
            }
        });
        
        return sortedList;
    }
    
    /**
     * 计算新闻项的相关度得分 - 利用API提供的数据
     */
    private double calculateRelevanceScore(NewsItem newsItem, String searchQuery) {
        double totalScore = 0.0;
        String searchLower = searchQuery.toLowerCase();
        String[] searchWords = searchLower.split("\\s+");
        
        Log.d(TAG, "计算相关度: " + newsItem.getTitle() + " -> 搜索词: " + searchQuery);
        
        // 1. 关键词匹配得分（使用API返回的keywords和score）
        if (newsItem.getKeywords() != null && !newsItem.getKeywords().isEmpty()) {
            Log.d(TAG, "新闻关键词数量: " + newsItem.getKeywords().size());
            for (NewsItem.Keyword keyword : newsItem.getKeywords()) {
                String keywordText = keyword.getWord().toLowerCase();
                double keywordScore = keyword.getScore();
                
                for (String searchWord : searchWords) {
                    if (keywordText.contains(searchWord) || searchWord.contains(keywordText)) {
                        double addedScore = keywordScore * 100.0; // 将API的0-1分数转换为更大权重
                        totalScore += addedScore;
                        Log.d(TAG, "关键词匹配: '" + keyword.getWord() + "' (分数:" + keywordScore + ") -> 加分: " + addedScore);
                        break;
                    }
                }
            }
        }
        
        // 2. 标题匹配得分（最高权重）
        if (newsItem.getTitle() != null) {
            String titleLower = newsItem.getTitle().toLowerCase();
            
            // 完全匹配整个搜索词
            if (titleLower.contains(searchLower)) {
                totalScore += 50.0;
                Log.d(TAG, "标题完全匹配: +" + 50.0);
            }
            
            // 单词匹配
            int titleMatches = 0;
            for (String searchWord : searchWords) {
                if (titleLower.contains(searchWord)) {
                    titleMatches++;
                    totalScore += 20.0;
                    
                    // 如果是标题开头匹配，额外加分
                    if (titleLower.startsWith(searchWord)) {
                        totalScore += 10.0;
                        Log.d(TAG, "标题开头匹配: +" + 10.0);
                    }
                }
            }
            
            // 匹配词数量奖励
            if (titleMatches == searchWords.length && searchWords.length > 1) {
                totalScore += 30.0;
                Log.d(TAG, "标题全词匹配奖励: +" + 30.0);
            }
        }
        
        // 3. 内容匹配得分（中等权重）
        if (newsItem.getContent() != null) {
            String contentLower = newsItem.getContent().toLowerCase();
            
            // 完全匹配整个搜索词
            if (contentLower.contains(searchLower)) {
                totalScore += 15.0;
            }
            
            // 单词匹配
            for (String searchWord : searchWords) {
                if (contentLower.contains(searchWord)) {
                    totalScore += 5.0;
                    
                    // 统计出现次数，但有上限
                    int occurrences = contentLower.split(searchWord, -1).length - 1;
                    totalScore += Math.min(occurrences * 2.0, 10.0);
                }
            }
        }
        
        // 4. 分类匹配得分（使用API返回的category）
        if (newsItem.getCategory() != null) {
            String categoryLower = newsItem.getCategory().toLowerCase();
            for (String searchWord : searchWords) {
                if (categoryLower.contains(searchWord)) {
                    totalScore += 15.0; // 提高分类匹配权重
                    Log.d(TAG, "分类匹配: '" + newsItem.getCategory() + "' -> +" + 15.0);
                }
            }
        }
        
        // 5. 发布者匹配得分
        if (newsItem.getPublisher() != null) {
            String publisherLower = newsItem.getPublisher().toLowerCase();
            for (String searchWord : searchWords) {
                if (publisherLower.contains(searchWord)) {
                    totalScore += 8.0;
                }
            }
        }
        
        Log.d(TAG, "最终得分: " + totalScore + " (新闻: " + newsItem.getTitle() + ")");
        return totalScore;
    }
}