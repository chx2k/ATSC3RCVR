package com.sony.tv.app.atsc3receiver1_0.app;

import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by xhamc on 4/11/17.
 */

public class Ads {

    private String title;
    private String Period;
    private String duration;
    private String scheme;
    private String parsedPeriod;

    private DataSource dataSource;
    private DataSource fileDataSource;
    private DataSource assetDataSource;
    private DataSource httpDataDataSource;
    private XmlPullParserFactory factory;
    private XmlPullParser xpp;

    private static final String TAG="Ads";
    public static final String SCHEME_ASSET = "asset";
    public static final String SCHEME_FLUTE = "flute";
    public static final String SCHEME_HTTP  = "http";
    public final static String XLINK_HREF  ="/@xlink:href ";
    private final static int MANIFEST_BUFFER_SIZE=1000;

    private byte[] buffer;
    private ArrayList<Ad> adArrayList=new ArrayList<>();


    public Ads(){

        fileDataSource=new FileDataSource(null);
        assetDataSource=new FileDataSource(null);
        httpDataDataSource=new DefaultHttpDataSource(ATSC3.userAgent, null, null, DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, true) {
        };
        buffer=new byte[MANIFEST_BUFFER_SIZE];
    }

    public boolean addAd(String url, boolean enabled){
        String title="",period="",duration="",replaceStartString="";
        Uri uri=Uri.parse(url);
        if (Util.isLocalFileUri(uri)) {
            dataSource = fileDataSource;
        } else if (SCHEME_ASSET.equals(uri.getScheme())) {
            dataSource = assetDataSource;
        } else if (SCHEME_HTTP.equals(uri.getScheme())) {
            dataSource = httpDataDataSource;
        }else{
            Log.e(TAG,"URI scheme not recognized");
            dataSource = null;
            return false;
        }
        DataSpec dataSpec=new DataSpec(uri);
        String manifest="";
        try {
            dataSource.open(dataSpec);
            int len=dataSource.read(buffer,0,MANIFEST_BUFFER_SIZE);
            manifest=new String (buffer,0,len);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        XmlPullParserFactory factory = null;
        try {
            factory = XmlPullParserFactory.newInstance();
            xpp = factory.newPullParser();
            StringReader s=new StringReader(manifest);
            xpp.setInput(s);
            int eventType = xpp.getEventType();
            boolean titleTag=false;
            loop:while (eventType!=XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_DOCUMENT) {

                } else if(eventType == XmlPullParser.START_TAG) {
                    if (xpp.getName()=="Title"){
                        titleTag=true;
                    }else{
                        titleTag=false;
                    }
                    if (xpp.getName()=="Period"){

                        for (int i = 0; i < xpp.getAttributeCount(); i++) {
                            if (xpp.getAttributeName(i).equals("start")){
                                replaceStartString="start=".concat(xpp.getAttributeValue(i));
                            }
                            if (xpp.getAttributeName(i).equals("duration")){
                                duration=xpp.getAttributeValue(i);
                            }
                        }
                        break loop;
                    }
                } else if(eventType == XmlPullParser.END_TAG) {
                } else if(eventType == XmlPullParser.TEXT) {
                    if (titleTag) {
                        title = xpp.getText();
                    }
                }
                eventType = xpp.next();
            }
            period=manifest.substring(manifest.indexOf("<Period"),manifest.indexOf("</Period>")+9);
            adArrayList.add(new Ad(title,period,duration,scheme,replaceStartString,uri,enabled));
            return  true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }


    }

    public ArrayList<Ad> getAds(boolean enabled){
        if (!enabled){
            return adArrayList;
        }else{
            ArrayList<Ad> enabledArrayAds=new ArrayList<>();
            for (Ad ad:adArrayList)
            {
                if (ad.enabled) {
                    enabledArrayAds.add(ad);
                }
            }
            return enabledArrayAds;
        }
    }
    private static int adCount=0;

    public Ad getNextAd(boolean random){
        ArrayList<Ad> enabledArrayAds=new ArrayList<>();

        for (Ad ad:adArrayList)
        {
            if (ad.enabled) {
                enabledArrayAds.add(ad);
            }
        }
        if (enabledArrayAds.size()==0) return null;
        if (random)
        adCount++;
        if (adCount>enabledArrayAds.size()){
            adCount=0;
        }
        return enabledArrayAds.get(adCount);
    }
    public Ad getRandomAd(){

        ArrayList<Ad> enabledArrayAds=new ArrayList<>();

        for (Ad ad:adArrayList)
        {
            if (ad.enabled) {
                enabledArrayAds.add(ad);
            }
        }
        if (enabledArrayAds.size()==0) return null;
        Math.random(1)
        adCount++;
        if (adCount>enabledArrayAds.size()){
            adCount=0;
        }
        return enabledArrayAds.get(adCount);
    }



    private class Ad{
        public String title;
        public String period;
        public String duration;
        public String scheme;
        public String replaceStartString;
        public Uri uri;
        public boolean enabled;
        public Ad(String title, String period, String duration, String scheme, String replaceStartString, Uri uri, boolean enabled){
            this.title=title;
            this.period=period;
            this.duration=duration;
            this.scheme=scheme;
            this.replaceStartString=replaceStartString;
            this.uri=uri;
            this.enabled=true;
        }

    }

}
