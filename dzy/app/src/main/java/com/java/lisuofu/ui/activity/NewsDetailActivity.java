package com.java.lisuofu.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.java.lisuofu.R;
import com.java.lisuofu.model.NewsItem;
import com.java.lisuofu.service.GLMService;
import com.java.lisuofu.ui.fragment.FavoriteFragment;
import com.java.lisuofu.ui.fragment.HistoryFragment;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NewsDetailActivity extends AppCompatActivity {

    private static final String TAG = "NewsDetailActivity";
    private static final String EXTRA_NEWS_ITEM = "news_item";
    private static final String PREF_NAME = "news_prefs";
    private static final String KEY_SUMMARIES = "summaries";

    private NewsItem newsItem;
    private GLMService glmService;
    private SharedPreferences preferences;
    private Gson gson;

    // UI 组件
    private TextView titleText;
    private TextView publishTimeText;
    private TextView publisherText;
    private TextView categoryText;
    private TextView contentText;
    private TextView summaryText;
    private VideoView videoView;
    private FrameLayout videoContainer;
    private ProgressBar videoLoadingProgress;
    private ImageButton favoriteButton;
    private Button generateSummaryButton;
    private ProgressBar summaryProgressBar;
    private LinearLayout summaryLayout;
    private MediaController mediaController;
    private int videoPosition = 0; // 保存视频播放位置
    private boolean wasPlaying = false; // 保存播放状态

    public static void start(Context context, NewsItem newsItem) {
        Intent intent = new Intent(context, NewsDetailActivity.class);
        intent.putExtra(EXTRA_NEWS_ITEM, newsItem);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_detail);

        initializeComponents();
        setupToolbar();
        getIntentData();
        setupViews();

        // 自动添加到阅读记录
        if (newsItem != null) {
            HistoryFragment.addReadRecord(this, newsItem);
        }
    }

    private void initializeComponents() {
        glmService = new GLMService();
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        gson = new Gson();

        // 初始化UI组件
        titleText = findViewById(R.id.tv_news_title);
        publishTimeText = findViewById(R.id.tv_publish_time);
        publisherText = findViewById(R.id.tv_publisher);
        categoryText = findViewById(R.id.tv_category);
        contentText = findViewById(R.id.tv_content);
        summaryText = findViewById(R.id.tv_summary);
        videoView = findViewById(R.id.video_view);
        videoContainer = findViewById(R.id.video_container);
        videoLoadingProgress = findViewById(R.id.video_loading);
        favoriteButton = findViewById(R.id.btn_favorite);
        generateSummaryButton = findViewById(R.id.btn_generate_summary);
        summaryProgressBar = findViewById(R.id.progress_summary);
        summaryLayout = findViewById(R.id.layout_summary);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("新闻详情");
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            newsItem = (NewsItem) intent.getSerializableExtra(EXTRA_NEWS_ITEM);
        }

        if (newsItem == null) {
            Toast.makeText(this, "新闻数据获取失败", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupViews() {
        if (newsItem == null) return;

        // 设置新闻基本信息
        titleText.setText(newsItem.getTitle());
        publishTimeText.setText(formatTime(newsItem.getPublishTime()));
        publisherText.setText(newsItem.getPublisher());
        categoryText.setText(newsItem.getCategory());
        contentText.setText(newsItem.getContent());

        // 设置收藏状态
        updateFavoriteButton();

        // 设置点击事件
        favoriteButton.setOnClickListener(v -> toggleFavorite());
        generateSummaryButton.setOnClickListener(v -> generateSummary());

        // 检查是否已有缓存的摘要
        String cachedSummary = getCachedSummary(newsItem.getNewsID());
        if (!TextUtils.isEmpty(cachedSummary)) {
            showSummary(cachedSummary);
        }

        // 设置视频播放
        setupVideo();
    }

    private void setupVideo() {
        if (newsItem != null && !TextUtils.isEmpty(newsItem.getVideo())) {
            // 检查网络连接
            if (!isNetworkAvailable()) {
                Toast.makeText(this, "无网络连接，无法播放视频", Toast.LENGTH_SHORT).show();
                videoContainer.setVisibility(View.GONE);
                return;
            }
            
            try {
                Log.d(TAG, "设置视频: " + newsItem.getVideo());
                videoContainer.setVisibility(View.VISIBLE);
                videoLoadingProgress.setVisibility(View.VISIBLE);
                
                Uri videoUri = Uri.parse(newsItem.getVideo());
                videoView.setVideoURI(videoUri);

                // 设置媒体控制器
                mediaController = new MediaController(this);
                videoView.setMediaController(mediaController);
                mediaController.setAnchorView(videoView);

                // 设置播放准备监听器
                videoView.setOnPreparedListener(mediaPlayer -> {
                    Log.d(TAG, "视频准备完成");
                    videoLoadingProgress.setVisibility(View.GONE);
                    
                    // 恢复播放位置
                    if (videoPosition > 0) {
                        videoView.seekTo(videoPosition);
                        if (wasPlaying) {
                            videoView.start();
                        }
                    }
                    
                    mediaPlayer.setOnVideoSizeChangedListener((mp, width, height) -> {
                        Log.d(TAG, "视频尺寸: " + width + "x" + height);
                        mediaController.setAnchorView(videoView);
                    });
                    
                    // 设置循环播放，防止播放完就停止
                    mediaPlayer.setLooping(false);
                    
                    // 添加信息监听器来监控播放状态
                    mediaPlayer.setOnInfoListener((mp, what, extra) -> {
                        switch (what) {
                            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                                Log.d(TAG, "开始缓冲");
                                videoLoadingProgress.setVisibility(View.VISIBLE);
                                break;
                            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                                Log.d(TAG, "缓冲结束");
                                videoLoadingProgress.setVisibility(View.GONE);
                                break;
                            case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                                Log.d(TAG, "开始渲染视频");
                                break;
                        }
                        return false;
                    });
                    
                    // 调整VideoView高度以适应视频比例
                    int videoWidth = mediaPlayer.getVideoWidth();
                    int videoHeight = mediaPlayer.getVideoHeight();
                    if (videoWidth > 0 && videoHeight > 0) {
                        float aspectRatio = (float) videoHeight / videoWidth;
                        int viewWidth = videoView.getWidth();
                        if (viewWidth > 0) {
                            int newHeight = (int) (viewWidth * aspectRatio);
                            // 限制最大高度
                            int maxHeight = (int) (getResources().getDisplayMetrics().heightPixels * 0.4f);
                            newHeight = Math.min(newHeight, maxHeight);
                            
                            ViewGroup.LayoutParams layoutParams = videoView.getLayoutParams();
                            layoutParams.height = newHeight;
                            videoView.setLayoutParams(layoutParams);
                        }
                    }
                });

                // 设置完成监听器
                videoView.setOnCompletionListener(mediaPlayer -> {
                    Log.d(TAG, "视频播放完成");
                });

                // 设置错误监听器
                videoView.setOnErrorListener((mediaPlayer, what, extra) -> {
                    Log.e(TAG, "视频播放错误: what=" + what + ", extra=" + extra);
                    String errorMsg = "";
                    switch (what) {
                        case MediaPlayer.MEDIA_ERROR_IO:
                            errorMsg = "网络错误，请检查网络连接";
                            break;
                        case MediaPlayer.MEDIA_ERROR_MALFORMED:
                            errorMsg = "视频格式不支持";
                            break;
                        case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                            errorMsg = "视频编码不支持";
                            break;
                        case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                            errorMsg = "连接超时，请重试";
                            break;
                        default:
                            errorMsg = "视频播放失败";
                    }
                    
                    videoLoadingProgress.setVisibility(View.GONE);
                    Toast.makeText(NewsDetailActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    videoContainer.setVisibility(View.GONE);
                    return true;
                });

            } catch (Exception e) {
                Log.e(TAG, "视频设置失败", e);
                videoLoadingProgress.setVisibility(View.GONE);
                Toast.makeText(this, "视频链接无效", Toast.LENGTH_SHORT).show();
                videoContainer.setVisibility(View.GONE);
            }
        } else {
            Log.d(TAG, "没有视频内容");
            videoContainer.setVisibility(View.GONE);
        }
    }

    /**
     * 切换收藏状态
     */
    private void toggleFavorite() {
        if (newsItem == null || newsItem.getNewsId() == null) return;

        boolean currentFavoriteStatus = FavoriteFragment.isFavorite(this, newsItem.getNewsId());

        if (currentFavoriteStatus) {
            // 取消收藏
            FavoriteFragment.removeFavorite(this, newsItem);
            Toast.makeText(this, "已取消收藏", Toast.LENGTH_SHORT).show();
        } else {
            // 添加收藏
            FavoriteFragment.addFavorite(this, newsItem);
            Toast.makeText(this, "已添加到收藏", Toast.LENGTH_SHORT).show();
            
            // 添加收藏成功的动画效果
            favoriteButton.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(150)
                    .withEndAction(() -> {
                        favoriteButton.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(150)
                                .start();
                    })
                    .start();
        }

        updateFavoriteButton();
    }

    /**
     * 生成摘要
     */
    private void generateSummary() {
        if (newsItem == null || TextUtils.isEmpty(newsItem.getContent())) {
            Toast.makeText(this, "新闻内容为空，无法生成摘要", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查是否已有缓存的摘要
        String cachedSummary = getCachedSummary(newsItem.getNewsID());
        if (!TextUtils.isEmpty(cachedSummary)) {
            showSummary(cachedSummary);
            return;
        }

        // 显示加载状态
        generateSummaryButton.setEnabled(false);
        summaryProgressBar.setVisibility(View.VISIBLE);
        summaryText.setText("正在生成摘要...");
        summaryLayout.setVisibility(View.VISIBLE);

        // 调用GLM生成摘要
        glmService.generateNewsSummary(newsItem.getContent(), new GLMService.SummaryCallback() {
            @Override
            public void onSuccess(String summary) {
                runOnUiThread(() -> {
                    showSummary(summary);
                    // 缓存摘要
                    cacheSummary(newsItem.getNewsID(), summary);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    generateSummaryButton.setEnabled(true);
                    summaryProgressBar.setVisibility(View.GONE);
                    summaryText.setText("摘要生成失败，请重试");
                    Toast.makeText(NewsDetailActivity.this, "摘要生成失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showSummary(String summary) {
        generateSummaryButton.setEnabled(true);
        summaryProgressBar.setVisibility(View.GONE);
        summaryText.setText(summary);
        summaryLayout.setVisibility(View.VISIBLE);
    }

    /**
     * 更新收藏按钮状态
     */
    private void updateFavoriteButton() {
        if (newsItem == null || newsItem.getNewsId() == null) return;

        boolean isFavorite = FavoriteFragment.isFavorite(this, newsItem.getNewsId());

        // 设置选中状态来触发选择器
        favoriteButton.setSelected(isFavorite);
        
        // 同时手动设置图标确保显示正确
        if (isFavorite) {
            // 已收藏 - 显示红色填充心形
            favoriteButton.setImageResource(R.drawable.ic_favorite_filled);
        } else {
            // 未收藏 - 显示灰色边框心形
            favoriteButton.setImageResource(R.drawable.ic_favorite_border);
        }
    }

    // ========== SharedPreferences 摘要缓存相关方法 ==========

    private String getCachedSummary(String newsId) {
        if (newsId == null) return null;

        String json = preferences.getString(KEY_SUMMARIES, "{}");
        Type type = new TypeToken<java.util.Map<String, String>>(){}.getType();
        java.util.Map<String, String> summaries = gson.fromJson(json, type);
        return summaries != null ? summaries.get(newsId) : null;
    }

    private void cacheSummary(String newsId, String summary) {
        if (newsId == null || summary == null) return;

        String json = preferences.getString(KEY_SUMMARIES, "{}");
        Type type = new TypeToken<java.util.Map<String, String>>(){}.getType();
        java.util.Map<String, String> summaries = gson.fromJson(json, type);
        if (summaries == null) {
            summaries = new java.util.HashMap<>();
        }

        summaries.put(newsId, summary);

        // 限制缓存数量，避免占用过多存储空间
        if (summaries.size() > 200) {
            // 简单的清理策略：随机移除一些旧的摘要
            java.util.Iterator<String> iterator = summaries.keySet().iterator();
            int removeCount = 50;
            while (iterator.hasNext() && removeCount > 0) {
                iterator.next();
                iterator.remove();
                removeCount--;
            }
        }

        String updatedJson = gson.toJson(summaries);
        preferences.edit().putString(KEY_SUMMARIES, updatedJson).apply();

        Log.d(TAG, "缓存摘要: " + newsId);
    }

    private String formatTime(String timeStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(timeStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            return timeStr;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (glmService != null) {
            glmService.shutdown();
        }
        if (videoView != null) {
            videoView.stopPlayback();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 如果有视频且VideoContainer可见，确保MediaController正确显示
        if (videoView != null && videoContainer.getVisibility() == View.VISIBLE && mediaController != null) {
            mediaController.setAnchorView(videoView);
            
            // 如果之前在播放，恢复播放
            if (wasPlaying && videoPosition > 0) {
                videoView.seekTo(videoPosition);
                videoView.start();
                wasPlaying = false; // 重置状态
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null && videoView.isPlaying()) {
            // 保存当前播放位置和状态
            videoPosition = videoView.getCurrentPosition();
            wasPlaying = true;
            videoView.pause();
            Log.d(TAG, "视频暂停，保存位置: " + videoPosition);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
}