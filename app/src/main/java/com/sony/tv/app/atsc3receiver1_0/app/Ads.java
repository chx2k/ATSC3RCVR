package com.sony.tv.app.atsc3receiver1_0.app;

import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.util.Util;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

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
    private static final String SCHEME_ASSET = "asset";
    private static final String SCHEME_FLUTE = "flute";
    private static final String SCHEME_HTTP  = "http";
    private final static String XLINK_HREF  ="/@xlink:href ";
    private final static int MANIFEST_BUFFER_SIZE=1000;

    private byte[] buffer;
    private ArrayList<Ad> adArrayList=new ArrayList<>();


    public Ads(String uri, String scheme){

        fileDataSource=new FileDataSource(null);
        assetDataSource=new FileDataSource(null);
        httpDataDataSource=new DefaultHttpDataSource(ATSC3.userAgent, null, null, DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, true) {
        };
        buffer=new byte[MANIFEST_BUFFER_SIZE];
    }

    public void addAd(String url, String scheme){
        String title="",period="",duration="",replaceStartString="";
        Uri uri=Uri.parse(url);
        if (Util.isLocalFileUri(uri)) {
            dataSource = fileDataSource;
        } else if (SCHEME_ASSET.equals(scheme)) {
            dataSource = assetDataSource;
        } else if (SCHEME_HTTP.equals(scheme)) {
            dataSource = httpDataDataSource;
        }else{
            Log.e(TAG,"URI scheme not recognized");
            dataSource = null;
            return;
        }
        DataSpec dataSpec=new DataSpec(uri);
        String manifest="";
        try {
            dataSource.open(dataSpec);
            int len=dataSource.read(buffer,0,MANIFEST_BUFFER_SIZE);
            manifest=new String (buffer,0,len);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        XmlPullParserFactory factory = null;
        try {
            factory = XmlPullParserFactory.newInstance();
            xpp = factory.newPullParser();
            StringReader s=new StringReader(manifest);
            xpp.setInput(s);
            int eventType = xpp.getEventType();
            boolean titleTag=false;
            while (eventType!=XmlPullParser.END_DOCUMENT) {
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
                    }
                } else if(eventType == XmlPullParser.END_TAG) {
                } else if(eventType == XmlPullParser.TEXT) {
                    if (titleTag) {
                        title = xpp.getText();
                    }
                }
                eventType = xpp.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        period=manifest.substring(manifest.indexOf("<Period"),manifest.indexOf("</Period>")+9);
        adArrayList.add(new Ad(title,period,duration,scheme,replaceStartString,uri));

    }

    public static List<Ads> getAds() {
        return null;
    }

}
