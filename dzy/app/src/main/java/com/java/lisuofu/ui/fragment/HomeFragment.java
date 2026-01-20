package com.java.lisuofu.ui.fragment;

import android.app.AlertDialog;
import android.content.SharedPreferences;     // 本地数据存储
import android.os.Bundle;                     // 状态保存和恢复
import android.os.Handler;                    // 主线程消息处理
import android.os.Looper;                     // 消息循环

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import android.widget.Button;                 // 按钮
import android.widget.EditText;               // 输入框
import android.widget.ImageButton;            // 图片按钮
import android.widget.LinearLayout;           // 线性布局
import android.widget.TextView;               // 文本显示
import android.widget.Toast;                  // 提示信息

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.java.lisuofu.R;
import com.java.lisuofu.model.NewsItem;
import com.java.lisuofu.model.NewsResponse;
import com.java.lisuofu.service.GLMService;
import com.java.lisuofu.service.NewsApiService;
import com.java.lisuofu.ui.activity.NewsDetailActivity;
import com.java.lisuofu.ui.adapter.NewsAdapter;
import com.java.lisuofu.ui.adapter.CategoryManageAdapter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final String PREF_NAME = "news_prefs";
    private static final String KEY_SELECTED_CATEGORIES = "selected_categories";

    // 所有可用的新闻分类（不包含"其他"，因为"其他"分类的新闻不会显示）
    private static final String[] ALL_CATEGORIES = {
            "全部", "娱乐", "军事", "教育", "文化", "健康", "财经", "体育", "汽车", "科技", "社会"
    };

    // 当前显示的新闻分类（用户选择的）
    private List<String> selectedCategories = new ArrayList<>();

    // UI组件
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private LinearLayout categoryContainer;
    private View noNewsView;
    private View bottomLoadingIndicator;  // 底部加载指示器
    private ImageButton btnScrollToTop;
    private ImageButton btnCategoryManage;
    private EditText searchEditText;
    private Button searchButton;
    private NewsAdapter newsAdapter;

    // 服务和数据
    private NewsApiService newsApiService;
    private GLMService glmService;
    private SharedPreferences preferences;
    private Gson gson;

    // 状态变量
    private String currentCategory = "全部";
    private int currentPage = 1;
    private boolean isLoading = false;
    private List<NewsItem> newsList = new ArrayList<>();
    private String currentSearchKeyword = ""; // 当前搜索关键词
    
    // 防抖处理
    private Handler filterHandler = new Handler(Looper.getMainLooper());
    private Runnable filterRunnable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initServices();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerView();
        setupCategoryTabs();
        loadNews(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        // 刷新新闻状态（收藏、已读状态可能已改变）
        updateNewsStatus(newsList);
        newsAdapter.notifyDataSetChanged();
    }

    private void initServices() {
        newsApiService = new NewsApiService();
        glmService = new GLMService();
        preferences = requireContext().getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        gson = new Gson();
        
        // 加载用户选择的分类
        loadSelectedCategories();
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        recyclerView = view.findViewById(R.id.recycler_news);
        categoryContainer = view.findViewById(R.id.category_container);
        noNewsView = view.findViewById(R.id.tv_no_news);
        bottomLoadingIndicator = view.findViewById(R.id.bottom_loading_indicator);  // 初始化底部加载指示器
        btnScrollToTop = view.findViewById(R.id.btn_scroll_to_top);
        btnCategoryManage = view.findViewById(R.id.btn_category_manage);
        searchEditText = view.findViewById(R.id.search_edit_text);
        searchButton = view.findViewById(R.id.search_button);

        // 设置下拉刷新
        swipeRefreshLayout.setOnRefreshListener(() -> loadNews(true));
        swipeRefreshLayout.setColorSchemeResources(R.color.primary_color);
        
        // 设置回到顶部按钮点击事件
        btnScrollToTop.setOnClickListener(v -> scrollToTop());
        
        // 设置分类管理按钮点击事件
        btnCategoryManage.setOnClickListener(v -> showCategoryManageDialog());
        
        // 设置搜索功能
        setupSearchFunctionality();
    }

    /**
     * 加载用户选择的分类
     */
    private void loadSelectedCategories() {
        String json = preferences.getString(KEY_SELECTED_CATEGORIES, "");
        if (!json.isEmpty()) {
            Type type = new TypeToken<List<String>>(){}.getType();
            selectedCategories = gson.fromJson(json, type);
        }
        
        // 如果没有保存的分类或列表为空，使用默认分类
        if (selectedCategories == null || selectedCategories.isEmpty()) {
            selectedCategories = new ArrayList<>(Arrays.asList(ALL_CATEGORIES));
        }
        
        // 确保"全部"分类始终存在且在第一位
        if (!selectedCategories.contains("全部")) {
            selectedCategories.add(0, "全部");
        } else if (!selectedCategories.get(0).equals("全部")) {
            selectedCategories.remove("全部");
            selectedCategories.add(0, "全部");
        }
    }

    /**
     * 保存用户选择的分类
     */
    private void saveSelectedCategories() {
        String json = gson.toJson(selectedCategories);
        preferences.edit().putString(KEY_SELECTED_CATEGORIES, json).apply();
    }

    /**
     * 显示分类管理对话框
     */
    private void showCategoryManageDialog() {
        // 创建对话框布局
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_category_manage, null);
        
        RecyclerView recyclerView = dialogView.findViewById(R.id.recycler_category_manage);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        
        // 准备分类数据
        List<CategoryManageAdapter.CategoryItem> categoryItems = new ArrayList<>();
        for (String category : ALL_CATEGORIES) {
            boolean isSelected = selectedCategories.contains(category);
            boolean isRequired = "全部".equals(category); // "全部"为必选项
            categoryItems.add(new CategoryManageAdapter.CategoryItem(category, isSelected, isRequired));
        }
        
        // 设置适配器
        CategoryManageAdapter adapter = new CategoryManageAdapter(categoryItems);
        // 2. 设置布局管理器（决定如何排列）
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        // 3. 将适配器绑定到RecyclerView
        recyclerView.setAdapter(adapter);
        
        // 创建对话框 - 设置合适的大小
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();
        
        // 取消按钮
        btnCancel.setOnClickListener(v -> {
            Log.d(TAG, "分类管理对话框：用户点击取消");
            dialog.dismiss();
        });
        
        // 确定按钮
        btnConfirm.setOnClickListener(v -> {
            Log.d(TAG, "分类管理对话框：用户点击确定");
            
            // 获取用户选择的分类
            List<String> newSelectedCategories = new ArrayList<>();
            for (CategoryManageAdapter.CategoryItem item : adapter.getCategoryList()) {
                if (item.isSelected()) {
                    newSelectedCategories.add(item.getName());
                }
            }
            
            Log.d(TAG, "用户选择的分类: " + newSelectedCategories.toString());
            
            // 确保至少有"全部"分类
            if (newSelectedCategories.isEmpty() || !newSelectedCategories.contains("全部")) {
                newSelectedCategories.add(0, "全部");
            }
            
            // 更新分类
            selectedCategories.clear();
            selectedCategories.addAll(newSelectedCategories);
            
            // 保存设置
            saveSelectedCategories();
            
            // 重新设置分类标签
            setupCategoryTabs();
            
            // 如果当前分类不在新的分类列表中，切换到"全部"分类
            if (!selectedCategories.contains(currentCategory)) {
                currentCategory = "全部";
                loadNews(true);
            }
            
            dialog.dismiss();
            Toast.makeText(requireContext(), "分类设置已保存", Toast.LENGTH_SHORT).show();
        });
        
        // 显示对话框
        dialog.show();
        
        // 调整对话框窗口大小
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.9), 
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void setupRecyclerView() {
        newsAdapter = new NewsAdapter(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(newsAdapter);

        // 设置新闻项目点击监听
        newsAdapter.setOnNewsItemClickListener(new NewsAdapter.OnNewsItemClickListener() {
            @Override
            public void onNewsClick(NewsItem newsItem) {
                // 添加到阅读记录
                HistoryFragment.addReadRecord(requireContext(), newsItem);
                // 标记为已读
                newsItem.setRead(true);
                // 打开新闻详情页
                NewsDetailActivity.start(requireContext(), newsItem);
            }
        });

        // 设置滚动监听，实现上拉加载更多
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading && dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if (pastVisibleItems + visibleItemCount >= totalItemCount - 2) {
                        loadMoreNews();
                    }
                }
            }
        });
    }

    private void setupCategoryTabs() {
        categoryContainer.removeAllViews();

        for (String category : selectedCategories) {
            TextView categoryTab = createCategoryTab(category);
            categoryContainer.addView(categoryTab);
        }

        // 默认选中第一个分类
        if (!selectedCategories.isEmpty()) {
            updateCategorySelection(selectedCategories.get(0));
        }
    }

    private TextView createCategoryTab(String category) {
        TextView textView = new TextView(requireContext());
        textView.setText(category);
        textView.setTextSize(14);
        textView.setPadding(24, 12, 24, 12);
        textView.setBackground(getResources().getDrawable(R.drawable.category_chip_background, null));
        textView.setTextColor(getResources().getColorStateList(R.color.category_chip_text_color, null));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMarginEnd(12);
        textView.setLayoutParams(params);

        textView.setOnClickListener(v -> {
            if (!category.equals(currentCategory)) {
                currentCategory = category;
                
                // 切换分类时清除搜索状态
                currentSearchKeyword = "";
                searchEditText.setText("");
                
                updateCategorySelection(category);
                loadNews(true);
            }
        });

        return textView;
    }

    private void updateCategorySelection(String selectedCategory) {
        for (int i = 0; i < categoryContainer.getChildCount(); i++) {
            TextView categoryTab = (TextView) categoryContainer.getChildAt(i);
            boolean isSelected = categoryTab.getText().toString().equals(selectedCategory);
            categoryTab.setSelected(isSelected);
        }
    }

    private void loadNews(boolean refresh) {
        if (isLoading) return;

        // 如果有搜索关键词，根据类型选择相应的搜索方法
        if (!currentSearchKeyword.isEmpty()) {
            // 检查是否为日期格式搜索
            if (isDateFormat(currentSearchKeyword)) {
                loadDateNews(currentSearchKeyword, refresh);
                return;
            } else {
                loadSearchNews(currentSearchKeyword, refresh);
                return;
            }
        }

        isLoading = true;

        if (refresh) {
            currentPage = 1;
            swipeRefreshLayout.setRefreshing(true);
        }

        NewsApiService.NewsRequestParams params;
        if ("全部".equals(currentCategory)) {
            params = NewsApiService.getDefaultTodayParams();
        } else {
            params = NewsApiService.getCategoryParams(currentCategory, currentPage);
        }
        params.setPage(currentPage);

        Log.d(TAG, "加载新闻: 分类=" + currentCategory + ", 页码=" + currentPage + ", 刷新=" + refresh);

        newsApiService.getNewsList(params, new NewsApiService.NewsCallback() {
            @Override
            public void onSuccess(NewsResponse response) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isLoading = false;
                        swipeRefreshLayout.setRefreshing(false);
                        // 隐藏底部加载指示器
                        if (bottomLoadingIndicator != null) {
                            bottomLoadingIndicator.setVisibility(View.GONE);
                        }

                        if (refresh) {
                            // 刷新时总是清空现有列表
                            newsList.clear();
                            newsAdapter.clearNews();
                        }

                        if (response.getData() != null && !response.getData().isEmpty()) {
                            List<NewsItem> newsItems = response.getData();

                            // 调试：统计分类分布
                            logCategoryDistribution(newsItems, "获取到的原始新闻");

                            // 更新新闻的读取状态和收藏状态
                            updateNewsStatus(newsItems);

                            // 调试：统计处理后的分类分布（直接使用API分类）
                            logCategoryDistribution(newsItems, "使用API分类的新闻");

                            // 根据当前分类过滤新闻
                            List<NewsItem> filteredNews = filterNewsByCategory(newsItems, currentCategory);
                            
                            // 调试：统计过滤后的分类分布
                            logCategoryDistribution(filteredNews, "过滤后的新闻 (分类: " + currentCategory + ")");

                            if (refresh) {
                                newsList.addAll(filteredNews);  // 使用过滤后的新闻
                                newsAdapter.setNewsList(newsList);
                            } else {
                                newsList.addAll(filteredNews);  // 使用过滤后的新闻
                                newsAdapter.addNews(newsItems);
                            }

                            currentPage++;
                            Log.d(TAG, "成功加载 " + newsItems.size() + " 条新闻");
                        } else {
                            // 无论是否刷新，都显示无数据提示
                            Log.d(TAG, "当前分类 '" + currentCategory + "' 无新闻数据");
                        }

                        // 更新空状态显示
                        updateEmptyState();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isLoading = false;
                        swipeRefreshLayout.setRefreshing(false);
                        // 隐藏底部加载指示器
                        if (bottomLoadingIndicator != null) {
                            bottomLoadingIndicator.setVisibility(View.GONE);
                        }
                        
                        if (refresh) {
                            // 刷新失败时也要清空现有列表
                            newsList.clear();
                            newsAdapter.clearNews();
                        }
                        
                        // 更新空状态显示
                        updateEmptyState();
                        
                        Toast.makeText(requireContext(), "加载失败: " + error, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "加载新闻失败: " + error);
                    });
                }
            }
        });
    }

    private void loadMoreNews() {
        // 上拉加载更多时显示底部加载指示器
        if (bottomLoadingIndicator != null) {
            bottomLoadingIndicator.setVisibility(View.VISIBLE);
        }
        loadNews(false);
    }

    /**
     * 更新新闻状态（已读、收藏）
     */
    private void updateNewsStatus(List<NewsItem> newsItems) {
        for (NewsItem newsItem : newsItems) {
            if (newsItem.getNewsId() != null) {
                newsItem.setRead(HistoryFragment.isRead(requireContext(), newsItem.getNewsId()));
                newsItem.setFavorite(FavoriteFragment.isFavorite(requireContext(), newsItem.getNewsId()));
            }
        }
    }

    /**
     * 更新空状态显示
     */
    private void updateEmptyState() {
        if (newsList.isEmpty()) {
            noNewsView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            noNewsView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 调试方法：统计分类分布
     */
    private void logCategoryDistribution(List<NewsItem> newsItems, String stage) {
        Map<String, Integer> categoryCount = new HashMap<>();
        for (NewsItem item : newsItems) {
            String category = item.getCategory();
            if (category == null) category = "null";
            categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);
        }
        
        Log.d(TAG, "=== " + stage + " 分类统计 ===");
        for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
            Log.d(TAG, "分类: " + entry.getKey() + " -> " + entry.getValue() + " 条");
        }
        Log.d(TAG, "总计: " + newsItems.size() + " 条新闻");
    }

    /**
     * 根据分类过滤新闻
     */
    private List<NewsItem> filterNewsByCategory(List<NewsItem> newsItems, String targetCategory) {
        if ("全部".equals(targetCategory)) {
            return newsItems;
        }
        
        List<NewsItem> filtered = new ArrayList<>();
        for (NewsItem item : newsItems) {
            if (targetCategory.equals(item.getCategory())) {
                filtered.add(item);
            }
        }
        
        Log.d(TAG, "分类过滤: " + targetCategory + " -> 找到 " + filtered.size() + " 条新闻");
        return filtered;
    }

    /**
     * 调度过滤更新 - 防抖机制，避免频繁过滤
     */
    private void scheduleFilterUpdate() {
        // 取消之前的任务
        if (filterRunnable != null) {
            filterHandler.removeCallbacks(filterRunnable);
        }
        
        // 创建新的延迟任务
        filterRunnable = () -> {
            Log.d(TAG, "执行延迟过滤更新");
            // 重新过滤并更新显示
            List<NewsItem> filteredNews = filterNewsByCategory(newsList, currentCategory);
            newsAdapter.setNewsList(filteredNews);
            updateEmptyState();
        };
        
        // 延迟500毫秒执行，等待可能的其他智能分类完成
        filterHandler.postDelayed(filterRunnable, 500);
    }

    /**
     * 滚动到顶部
     */
    private void scrollToTop() {
        if (recyclerView != null) {
            recyclerView.smoothScrollToPosition(0);
        }
    }

    /**
     * 设置搜索功能
     */
    private void setupSearchFunctionality() {
        // 搜索按钮点击事件
        searchButton.setOnClickListener(v -> performSearch());
        
        // 搜索框回车事件
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performSearch();
                return true;
            }
            return false;
        });
        
        // 搜索框文本变化监听（可选：实时搜索）
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // 如果搜索框为空，恢复正常的分类显示
                if (s.toString().trim().isEmpty() && !currentSearchKeyword.isEmpty()) {
                    currentSearchKeyword = "";
                    loadNews(true);
                }
            }
        });
    }

    /**
     * 执行搜索
     */
    private void performSearch() {
        String keyword = searchEditText.getText().toString().trim();
        if (keyword.isEmpty()) {
            Toast.makeText(getContext(), "请输入搜索关键词", Toast.LENGTH_SHORT).show();
            return;
        }
        
        currentSearchKeyword = keyword;
        currentPage = 1;
        Log.d(TAG, "执行搜索: " + keyword);
        
        // 检查是否为日期格式搜索
        if (isDateFormat(keyword)) {
            Log.d(TAG, "检测到日期格式搜索: " + keyword);
            // 使用日期范围搜索，而不是关键词搜索
            loadDateNews(keyword, true);
        } else {
            // 使用搜索参数加载新闻
            loadSearchNews(keyword, true);
        }
    }

    /**
     * 检查是否为日期格式 (yyyy-mm-dd)
     */
    private boolean isDateFormat(String input) {
        if (input == null || input.length() != 10) {
            return false;
        }
        
        // 检查格式：yyyy-mm-dd
        String datePattern = "^\\d{4}-\\d{2}-\\d{2}$";
        if (!input.matches(datePattern)) {
            return false;
        }
        
        try {
            // 进一步验证日期的有效性
            String[] parts = input.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            
            // 基本范围检查
            if (year < 2020 || year > 2030) return false;
            if (month < 1 || month > 12) return false;
            if (day < 1 || day > 31) return false;
            
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 获取指定日期的第二天
     */
    private String getNextDay(String date) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            java.util.Date currentDate = sdf.parse(date);
            
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.setTime(currentDate);
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
            
            return sdf.format(calendar.getTime());
        } catch (Exception e) {
            Log.e(TAG, "计算第二天日期失败: " + date, e);
            // 如果解析失败，返回原日期
            return date;
        }
    }

    /**
     * 将日期格式转换为API需要的完整时间格式
     */
    private String formatDateForAPI(String date, boolean isStartDate) {
        // 先尝试纯日期格式，不加时间
        return date;
        
        /* 如果需要时间格式，可以启用以下代码：
        if (isStartDate) {
            // 开始时间：当天的00:00:00
            return date + " 00:00:00";
        } else {
            // 结束时间：当天的23:59:59
            return date + " 23:59:59";
        }
        */
    }

    /**
     * 加载指定日期的新闻
     */
    private void loadDateNews(String date, boolean refresh) {
        if (isLoading) return;
        
        isLoading = true;
        if (refresh) {
            swipeRefreshLayout.setRefreshing(true);
        }

        // 计算结束日期：输入日期的第二天
        String endDate = getNextDay(date);
        
        // 使用时间范围参数：startDate为输入日期，endDate为第二天
        NewsApiService.NewsRequestParams params = 
            NewsApiService.getTimeRangeParams(date, endDate, currentPage);
        
        Log.d(TAG, "使用日期搜索参数详情:");
        Log.d(TAG, "  - startDate: " + params.getStartDate());
        Log.d(TAG, "  - endDate: " + params.getEndDate());
        Log.d(TAG, "  - words: " + params.getWords());
        Log.d(TAG, "  - categories: " + params.getCategories());
        Log.d(TAG, "  - size: " + params.getSize());
        Log.d(TAG, "  - page: " + params.getPage());
        
        newsApiService.getNewsList(params, new NewsApiService.NewsCallback() {
            @Override
            public void onSuccess(NewsResponse response) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isLoading = false;
                        swipeRefreshLayout.setRefreshing(false);
                        // 隐藏底部加载指示器
                        if (bottomLoadingIndicator != null) {
                            bottomLoadingIndicator.setVisibility(View.GONE);
                        }

                        if (refresh) {
                            newsList.clear();
                            newsAdapter.clearNews();
                        }

                        if (response.getData() != null && !response.getData().isEmpty()) {
                            List<NewsItem> newsItems = response.getData();
                            updateNewsStatus(newsItems);

                            // 根据当前分类过滤新闻
                            List<NewsItem> filteredNews = filterNewsByCategory(newsItems, currentCategory);

                            if (refresh) {
                                newsList.addAll(filteredNews);
                                newsAdapter.setNewsList(newsList);
                            } else {
                                newsList.addAll(filteredNews);
                                newsAdapter.addNews(filteredNews);
                            }

                            currentPage++;
                            Log.d(TAG, "找到 " + filteredNews.size() + " 条 " + date + " 的新闻");
                            
                            // 显示搜索结果提示
                            if (filteredNews.size() > 0) {
                                Toast.makeText(getContext(), "找到 " + filteredNews.size() + " 条 " + date + " 的新闻", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "未找到 " + date + " 的新闻", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.d(TAG, "日期 '" + date + "' 无新闻数据");
                            Toast.makeText(getContext(), "未找到 " + date + " 的新闻", Toast.LENGTH_SHORT).show();
                        }

                        updateEmptyState();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isLoading = false;
                        swipeRefreshLayout.setRefreshing(false);
                        // 隐藏底部加载指示器
                        if (bottomLoadingIndicator != null) {
                            bottomLoadingIndicator.setVisibility(View.GONE);
                        }
                        if (refresh) {
                            newsList.clear();
                            newsAdapter.clearNews();
                        }
                        updateEmptyState();
                        
                        Log.e(TAG, "日期搜索失败: " + error);
                        
                        // 对于日期搜索失败，直接提示用户，不进行降级
                        String errorMessage;
                        if (error.contains("500") || error.contains("NullPointerException")) {
                            errorMessage = "该日期 (" + date + ") 暂无新闻数据";
                        } else {
                            errorMessage = "日期搜索失败: " + error;
                        }
                        
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "日期 '" + date + "' 搜索结束，未找到相关新闻");
                    });
                }
            }
        });
    }

    /**
     * 加载搜索新闻
     */
    private void loadSearchNews(String keywords, boolean refresh) {
        if (isLoading) return;
        
        isLoading = true;
        if (refresh) {
            swipeRefreshLayout.setRefreshing(true);
        }

        NewsApiService.NewsRequestParams params = 
            NewsApiService.getSearchParams(keywords, currentCategory, currentPage);
        
        newsApiService.getNewsList(params, new NewsApiService.NewsCallback() {
            @Override
            public void onSuccess(NewsResponse response) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isLoading = false;
                        swipeRefreshLayout.setRefreshing(false);
                        if (bottomLoadingIndicator != null) {
                            bottomLoadingIndicator.setVisibility(View.GONE);
                        }

                        if (refresh) {
                            newsList.clear();
                            newsAdapter.clearNews();
                        }

                        if (response.getData() != null && !response.getData().isEmpty()) {
                            List<NewsItem> newsItems = response.getData();
                            updateNewsStatus(newsItems);

                            // 根据搜索关键词进一步过滤
                            List<NewsItem> filteredNews = filterNewsByKeyword(newsItems, keywords);

                            if (refresh) {
                                newsList.addAll(filteredNews);
                                newsAdapter.setNewsList(newsList);
                            } else {
                                newsList.addAll(filteredNews);
                                newsAdapter.addNews(filteredNews);
                            }

                            currentPage++;
                            Log.d(TAG, "搜索到 " + filteredNews.size() + " 条相关新闻");
                        } else {
                            Log.d(TAG, "搜索 '" + keywords + "' 无结果");
                        }

                        updateEmptyState();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isLoading = false;
                        swipeRefreshLayout.setRefreshing(false);
                        if (bottomLoadingIndicator != null) {
                            bottomLoadingIndicator.setVisibility(View.GONE);
                        }
                        Toast.makeText(getContext(), "搜索失败: " + error, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "搜索新闻失败: " + error);
                    });
                }
            }
        });
    }

    /**
     * 根据关键词过滤新闻
     */
    private List<NewsItem> filterNewsByKeyword(List<NewsItem> newsList, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>(newsList);
        }

        List<NewsItem> filtered = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();

        for (NewsItem news : newsList) {
            if (news.getTitle() != null && news.getTitle().toLowerCase().contains(lowerKeyword) ||
                news.getContent() != null && news.getContent().toLowerCase().contains(lowerKeyword)) {
                filtered.add(news);
            }
        }

        return filtered;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // 清理防抖任务
        if (filterHandler != null && filterRunnable != null) {
            filterHandler.removeCallbacks(filterRunnable);
        }
        
        if (newsApiService != null) {
            newsApiService.shutdown();
        }
        if (glmService != null) {
            glmService.shutdown();
        }
    }
}
