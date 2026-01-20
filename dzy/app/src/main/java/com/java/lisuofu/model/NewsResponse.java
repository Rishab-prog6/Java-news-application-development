package com.java.lisuofu.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class NewsResponse {

    @SerializedName("pageSize")
    private String pageSize;

    @SerializedName("total")
    private int total;

    @SerializedName("data")
    private List<NewsItem> data;

    @SerializedName("currentPage")
    private String currentPage;

    // 构造函数
    public NewsResponse() {}

    public NewsResponse(String pageSize, int total, List<NewsItem> data, String currentPage) {
        this.pageSize = pageSize;
        this.total = total;
        this.data = data;
        this.currentPage = currentPage;
    }

    // Getter和Setter方法
    public String getPageSize() { return pageSize; }
    public void setPageSize(String pageSize) { this.pageSize = pageSize; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public List<NewsItem> getData() { return data; }
    public void setData(List<NewsItem> data) { this.data = data; }

    public String getCurrentPage() { return currentPage; }
    public void setCurrentPage(String currentPage) { this.currentPage = currentPage; }
}
