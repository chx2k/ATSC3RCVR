package com.sony.tv.app.atsc3receiver1_0.app;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.upstream.AssetDataSource;
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
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by xhamc on 4/11/17.
 */

public class Ads {

    private static String title;
    private static String Period;
    private static String duration;
    private static String scheme;
    private static String parsedPeriod;

    private static DataSource dataSource;
    private static DataSource fileDataSource;
    private static DataSource assetDataSource;
    private static DataSource httpDataDataSource;
    private static XmlPullParserFactory factory;
    private static XmlPullParser xpp;

    private static final String TAG="Ads";
    public static final String SCHEME_ASSET = "asset";
    public static final String SCHEME_FLUTE = "flute";
    public static final String SCHEME_HTTP  = "http";
    public final static String XLINK_HREF  ="/@xlink:href ";
    private final static int MANIFEST_BUFFER_SIZE=5000;
    private static int adCount=0;


    private static byte[] buffer=new byte[MANIFEST_BUFFER_SIZE];
    private static ArrayList<Ad> adArrayList=new ArrayList<>();

    public Ads(Context context){
        DataSource fileDataSource=new FileDataSource(null);
        assetDataSource=new AssetDataSource(context);
        httpDataDataSource= new DefaultHttpDataSource(ATSC3.userAgent, null, null,
                DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, true);
    }

    public static boolean addAd(String url, boolean enabled){
        String title="",period="",duration="",replaceStartString="",manifest="";
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
                    if (xpp.getName().equals("Title")){
                        titleTag=true;
                    }else{
                        titleTag=false;
                    }
                    if (xpp.getName().equals("Period")){

                        for (int i = 0; i < xpp.getAttributeCount(); i++) {
                            if (xpp.getAttributeName(i).equals("start")){
                                replaceStartString=xpp.getAttributeValue(i);
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
            int baseUrlStart=period.indexOf("<Period");
            baseUrlStart=period.indexOf(">",baseUrlStart+7)+1;

            String newPeriod=period.substring(0,baseUrlStart).concat("<BaseURL>").concat(uri.toString().substring(0,uri.toString().lastIndexOf("/")+1)).concat("</BaseURL>").concat(period.substring(baseUrlStart,period.length()));

//            String newPeriod=period.substring(0,baseUrlStart).concat("<BaseURL>").concat("/asset").concat(uri.getPath().substring(0,uri.getPath().lastIndexOf("/")+1)).concat("</BaseURL>").concat(period.substring(baseUrlStart,period.length()));

            adArrayList.add(new Ad(title,newPeriod,duration,scheme,replaceStartString,uri,enabled));
            return  true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static ArrayList<Ad> getAds(boolean enabled){
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

    public static Ad getAdByTitle(String title){

        for (Ad ad:adArrayList)
        {
            if (ad.title.equals(title)) {
               return ad;
            }
        }
        return null;

    }


    public static Ad getNextAd(boolean random){
        ArrayList<Ad> enabledArrayAds=new ArrayList<>();

        for (Ad ad:adArrayList)
        {
            if (ad.enabled) {
                enabledArrayAds.add(ad);
            }
        }
        if (enabledArrayAds.size()==0) return null;
        if (random){
            if (enabledArrayAds.size()==0) return null;
            int randomAd=(int) Math.floor(enabledArrayAds.size()*Math.random());
            return enabledArrayAds.get(randomAd);

        }else {
            adCount++;
            if (adCount > enabledArrayAds.size()) {
                adCount = 0;
            }
            return enabledArrayAds.get(adCount);
        }

    }




    public static class Ad{
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
