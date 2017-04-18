package com.sony.tv.app.atsc3receiver1_0.app;

import android.net.Uri;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

/**
 * Created by valokafor on 4/12/17.
 */
public class AdContent extends RealmObject{
    @PrimaryKey
    public long id;
    public String title;
    public String period;
    public String duration;
    public String scheme;
    public String replaceStartString;
    public int displayCount;
    public boolean enabled;
    @Ignore
    public Uri uri;
    public String uriString;

    public AdContent(){

    }

    public AdContent(String title, String period, String duration, String scheme, String replaceStartString, Uri uri, boolean enabled){
        this.title=title;
        this.period=period;
        this.duration=duration;
        this.scheme=scheme;
        this.replaceStartString=replaceStartString;
        this.uri=uri;
        this.enabled=true;
        this.uriString = uri.toString();
    }


    public void updateToRealm(AdContent ad) {
        title = ad.title;
        period = ad.period;
        duration = ad.duration;
        scheme = ad.scheme;
        replaceStartString = ad.replaceStartString;
        displayCount = ad.displayCount;
        enabled = ad.enabled;
        uriString = ad.uri.toString();
    }
}
