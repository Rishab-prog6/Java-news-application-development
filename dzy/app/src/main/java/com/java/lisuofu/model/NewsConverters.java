package com.java.lisuofu.model;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

public class NewsConverters {

    private static final Gson gson = new Gson();

    // Keywords转换
    @TypeConverter
    public static String fromKeywordsList(List<NewsItem.Keyword> keywords) {
        if (keywords == null) return null;
        return gson.toJson(keywords);
    }

    @TypeConverter
    public static List<NewsItem.Keyword> toKeywordsList(String data) {
        if (data == null) return null;
        Type listType = new TypeToken<List<NewsItem.Keyword>>(){}.getType();
        return gson.fromJson(data, listType);
    }

    // Persons转换
    @TypeConverter
    public static String fromPersonsList(List<NewsItem.Person> persons) {
        if (persons == null) return null;
        return gson.toJson(persons);
    }

    @TypeConverter
    public static List<NewsItem.Person> toPersonsList(String data) {
        if (data == null) return null;
        Type listType = new TypeToken<List<NewsItem.Person>>(){}.getType();
        return gson.fromJson(data, listType);
    }

    // Organizations转换
    @TypeConverter
    public static String fromOrganizationsList(List<NewsItem.Organization> organizations) {
        if (organizations == null) return null;
        return gson.toJson(organizations);
    }

    @TypeConverter
    public static List<NewsItem.Organization> toOrganizationsList(String data) {
        if (data == null) return null;
        Type listType = new TypeToken<List<NewsItem.Organization>>(){}.getType();
        return gson.fromJson(data, listType);
    }

    // Locations转换
    @TypeConverter
    public static String fromLocationsList(List<NewsItem.Location> locations) {
        if (locations == null) return null;
        return gson.toJson(locations);
    }

    @TypeConverter
    public static List<NewsItem.Location> toLocationsList(String data) {
        if (data == null) return null;
        Type listType = new TypeToken<List<NewsItem.Location>>(){}.getType();
        return gson.fromJson(data, listType);
    }

    // TimeReferences转换
    @TypeConverter
    public static String fromTimeReferencesList(List<NewsItem.TimeReference> timeReferences) {
        if (timeReferences == null) return null;
        return gson.toJson(timeReferences);
    }

    @TypeConverter
    public static List<NewsItem.TimeReference> toTimeReferencesList(String data) {
        if (data == null) return null;
        Type listType = new TypeToken<List<NewsItem.TimeReference>>(){}.getType();
        return gson.fromJson(data, listType);
    }

    // LocationReferences转换
    @TypeConverter
    public static String fromLocationReferencesList(List<NewsItem.LocationReference> locationReferences) {
        if (locationReferences == null) return null;
        return gson.toJson(locationReferences);
    }

    @TypeConverter
    public static List<NewsItem.LocationReference> toLocationReferencesList(String data) {
        if (data == null) return null;
        Type listType = new TypeToken<List<NewsItem.LocationReference>>(){}.getType();
        return gson.fromJson(data, listType);
    }

    // PersonReferences转换
    @TypeConverter
    public static String fromPersonReferencesList(List<NewsItem.PersonReference> personReferences) {
        if (personReferences == null) return null;
        return gson.toJson(personReferences);
    }

    @TypeConverter
    public static List<NewsItem.PersonReference> toPersonReferencesList(String data) {
        if (data == null) return null;
        Type listType = new TypeToken<List<NewsItem.PersonReference>>(){}.getType();
        return gson.fromJson(data, listType);
    }
}