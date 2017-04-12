package com.sony.tv.app.atsc3receiver1_0.app;

import android.net.Uri;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by valokafor on 4/12/17.
 */
public class Ad extends RealmObject{
    @PrimaryKey
    private long id;
    private String title;
    private String period;
    private String duration;
    private String scheme;
    private String replaceStartString;
    private int displayCount;
    private boolean enabled;
    private String uri;

    public Ad(){

    }

    public Ad(String title, String period, String duration, String scheme, String replaceStartString, Uri uri) {
        this.title = title;
        this.period = period;
        this.duration = duration;
        this.scheme = scheme;
        this.replaceStartString = replaceStartString;
        this.uri = uri.toString();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getReplaceStartString() {
        return replaceStartString;
    }

    public void setReplaceStartString(String replaceStartString) {
        this.replaceStartString = replaceStartString;
    }

    public int getDisplayCount() {
        return displayCount;
    }

    public void setDisplayCount(int displayCount) {
        this.displayCount = displayCount;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
