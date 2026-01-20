package com.java.lisuofu.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

@Entity(tableName = "news_items")
@TypeConverters({NewsConverters.class})
public class NewsItem implements Serializable {

    @PrimaryKey
    @SerializedName("newsID")
    private String newsId;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("image")
    private String image;

    @SerializedName("video")
    private String video;

    @SerializedName("publisher")
    private String publisher;

    @SerializedName("category")
    private String category;

    @SerializedName("publishTime")
    private String publishTime;

    @SerializedName("crawlTime")
    private String crawlTime;

    @SerializedName("language")
    private String language;

    // API返回的复杂字段
    @SerializedName("keywords")
    private List<Keyword> keywords;

    @SerializedName("persons")
    private List<Person> persons;

    @SerializedName("organizations")
    private List<Organization> organizations;

    @SerializedName("locations")
    private List<Location> locations;

    @SerializedName("when")
    private List<TimeReference> timeReferences;

    @SerializedName("where")
    private List<LocationReference> locationReferences;

    @SerializedName("who")
    private List<PersonReference> personReferences;

    // 本地数据库字段
    private boolean isRead = false;
    private boolean isFavorite = false;
    private long readTime = 0;
    private String aiSummary;
    
    // 临时字段用于搜索排序
    private transient double totalKeywordScore = 0.0;

    // 构造函数
    public NewsItem() {}

    // Getter和Setter方法
    public String getNewsID() { return newsId; }
    public void setNewsID(String newsId) { this.newsId = newsId; }

    public String getNewsId() { return newsId; }
    public void setNewsId(String newsId) { this.newsId = newsId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getVideo() { return video; }
    public void setVideo(String video) { this.video = video; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPublishTime() { return publishTime; }
    public void setPublishTime(String publishTime) { this.publishTime = publishTime; }

    public String getCrawlTime() { return crawlTime; }
    public void setCrawlTime(String crawlTime) { this.crawlTime = crawlTime; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public List<Keyword> getKeywords() { return keywords; }
    public void setKeywords(List<Keyword> keywords) { this.keywords = keywords; }

    public List<Person> getPersons() { return persons; }
    public void setPersons(List<Person> persons) { this.persons = persons; }

    public List<Organization> getOrganizations() { return organizations; }
    public void setOrganizations(List<Organization> organizations) { this.organizations = organizations; }

    public List<Location> getLocations() { return locations; }
    public void setLocations(List<Location> locations) { this.locations = locations; }

    public List<TimeReference> getTimeReferences() { return timeReferences; }
    public void setTimeReferences(List<TimeReference> timeReferences) { this.timeReferences = timeReferences; }

    public List<LocationReference> getLocationReferences() { return locationReferences; }
    public void setLocationReferences(List<LocationReference> locationReferences) { this.locationReferences = locationReferences; }

    public List<PersonReference> getPersonReferences() { return personReferences; }
    public void setPersonReferences(List<PersonReference> personReferences) { this.personReferences = personReferences; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    public long getReadTime() { return readTime; }
    public void setReadTime(long readTime) { this.readTime = readTime; }

    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }

    public double getTotalKeywordScore() { return totalKeywordScore; }
    public void setTotalKeywordScore(double totalKeywordScore) { this.totalKeywordScore = totalKeywordScore; }

    // 内部类定义
    public static class Keyword implements Serializable {
        @SerializedName("score")
        private double score;
        @SerializedName("word")
        private String word;

        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }
    }

    public static class Person implements Serializable {
        @SerializedName("count")
        private int count;
        @SerializedName("linkedURL")
        private String linkedURL;
        @SerializedName("mention")
        private String mention;

        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public String getLinkedURL() { return linkedURL; }
        public void setLinkedURL(String linkedURL) { this.linkedURL = linkedURL; }
        public String getMention() { return mention; }
        public void setMention(String mention) { this.mention = mention; }
    }

    public static class Organization implements Serializable {
        @SerializedName("count")
        private int count;
        @SerializedName("linkedURL")
        private String linkedURL;
        @SerializedName("mention")
        private String mention;

        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public String getLinkedURL() { return linkedURL; }
        public void setLinkedURL(String linkedURL) { this.linkedURL = linkedURL; }
        public String getMention() { return mention; }
        public void setMention(String mention) { this.mention = mention; }
    }

    public static class Location implements Serializable {
        @SerializedName("lng")
        private double longitude;
        @SerializedName("lat")
        private double latitude;
        @SerializedName("count")
        private int count;
        @SerializedName("linkedURL")
        private String linkedURL;
        @SerializedName("mention")
        private String mention;

        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public String getLinkedURL() { return linkedURL; }
        public void setLinkedURL(String linkedURL) { this.linkedURL = linkedURL; }
        public String getMention() { return mention; }
        public void setMention(String mention) { this.mention = mention; }
    }

    public static class TimeReference implements Serializable {
        @SerializedName("score")
        private double score;
        @SerializedName("word")
        private String word;

        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }
    }

    public static class LocationReference implements Serializable {
        @SerializedName("score")
        private double score;
        @SerializedName("word")
        private String word;

        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }
    }

    public static class PersonReference implements Serializable {
        @SerializedName("score")
        private double score;
        @SerializedName("word")
        private String word;

        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }
    }
}