package com.java.lisuofu.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.java.lisuofu.R;
import com.java.lisuofu.model.NewsItem;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private Context context;
    private List<NewsItem> newsList;
    private OnNewsItemClickListener listener;

    public interface OnNewsItemClickListener {
        void onNewsClick(NewsItem newsItem);
    }

    public NewsAdapter(Context context) {
        this.context = context;
        this.newsList = new ArrayList<>();
    }

    public void setOnNewsItemClickListener(OnNewsItemClickListener listener) {
        this.listener = listener;
    }

    public void setNewsList(List<NewsItem> newsList) {
        this.newsList = newsList;
        notifyDataSetChanged();
    }

    public void addNews(List<NewsItem> newNews) {
        int startPosition = this.newsList.size();
        this.newsList.addAll(newNews);
        notifyItemRangeInserted(startPosition, newNews.size());
    }

    public void clearNews() {
        this.newsList.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsItem newsItem = newsList.get(position);
        holder.bind(newsItem);
    }

    @Override
    public int getItemCount() {
        return newsList.size();
    }

    class NewsViewHolder extends RecyclerView.ViewHolder {

        private TextView tvTitle;
        private TextView tvContent;
        private TextView tvCategory;
        private TextView tvSource;
        private TextView tvTime;
        private ImageView ivImage;

        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            initViews();
            setupClickListeners();
        }

        private void initViews() {
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvSource = itemView.findViewById(R.id.tv_source);
            tvTime = itemView.findViewById(R.id.tv_time);
            ivImage = itemView.findViewById(R.id.iv_image);
        }

        private void setupClickListeners() {
            // 整个条目点击
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onNewsClick(newsList.get(position));
                    }
                }
            });
        }

        public void bind(NewsItem newsItem) {
            // 设置标题
            tvTitle.setText(newsItem.getTitle() != null ? newsItem.getTitle() : "无标题");

            // 设置内容预览
            String content = newsItem.getContent();
            if (content != null && !content.isEmpty()) {
                tvContent.setText(content);
                tvContent.setVisibility(View.VISIBLE);
            } else {
                tvContent.setVisibility(View.GONE);
            }

            // 设置分类
            String category = newsItem.getCategory();
            if (category != null && !category.isEmpty()) {
                tvCategory.setText(category);
                tvCategory.setVisibility(View.VISIBLE);
            } else {
                tvCategory.setVisibility(View.GONE);
            }

            // 设置来源
            tvSource.setText(newsItem.getPublisher() != null ? newsItem.getPublisher() : "未知来源");

            // 设置时间
            tvTime.setText(formatTime(newsItem.getPublishTime()));

            // 设置图片
            loadNewsImage(newsItem);

            // 设置已读状态（已读新闻显示为灰色）
            updateReadStatus(newsItem.isRead());
        }

        private void loadNewsImage(NewsItem newsItem) {
            String imageUrl = getFirstImageUrl(newsItem.getImage());
            if (imageUrl != null && !imageUrl.isEmpty()) {
                ivImage.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .into(ivImage);
            } else {
                ivImage.setVisibility(View.GONE);
            }
        }

        private String getFirstImageUrl(String imageField) {
            if (imageField == null || imageField.isEmpty() || "[]".equals(imageField)) {
                return null;
            }

            try {
                // 处理数组格式的图片URL：'["URL"]' 或 '[URL]'
                if (imageField.startsWith("[") && imageField.endsWith("]")) {
                    String cleanUrl = imageField.substring(1, imageField.length() - 1);
                    if (cleanUrl.startsWith("\"") && cleanUrl.endsWith("\"")) {
                        cleanUrl = cleanUrl.substring(1, cleanUrl.length() - 1);
                    }
                    return cleanUrl.startsWith("http") ? cleanUrl : null;
                } else {
                    return imageField.startsWith("http") ? imageField : null;
                }
            } catch (Exception e) {
                return null;
            }
        }

        private void updateReadStatus(boolean isRead) {
            if (isRead) {
                // 已读新闻显示为灰色
                tvTitle.setTextColor(context.getResources().getColor(R.color.text_secondary, null));
                tvContent.setTextColor(context.getResources().getColor(R.color.text_hint, null));
                itemView.setAlpha(0.7f);
            } else {
                // 未读新闻显示正常颜色
                tvTitle.setTextColor(context.getResources().getColor(R.color.text_primary, null));
                tvContent.setTextColor(context.getResources().getColor(R.color.text_secondary, null));
                itemView.setAlpha(1.0f);
            }
        }

        private String formatTime(String publishTime) {
            if (publishTime == null || publishTime.isEmpty()) {
                return "未知时间";
            }

            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date date = inputFormat.parse(publishTime);

                if (date != null) {
                    long now = System.currentTimeMillis();
                    long diff = now - date.getTime();

                    // 计算时间差
                    if (diff < 60 * 1000) {
                        return "刚刚";
                    } else if (diff < 60 * 60 * 1000) {
                        return (diff / (60 * 1000)) + "分钟前";
                    } else if (diff < 24 * 60 * 60 * 1000) {
                        return (diff / (60 * 60 * 1000)) + "小时前";
                    } else if (diff < 7 * 24 * 60 * 60 * 1000) {
                        return (diff / (24 * 60 * 60 * 1000)) + "天前";
                    } else {
                        // 超过一周显示具体日期
                        SimpleDateFormat outputFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
                        return outputFormat.format(date);
                    }
                }
            } catch (Exception e) {
                // 解析失败，直接显示原始时间的前16位
                return publishTime.length() > 16 ? publishTime.substring(0, 16) : publishTime;
            }

            return publishTime;
        }
    }
}