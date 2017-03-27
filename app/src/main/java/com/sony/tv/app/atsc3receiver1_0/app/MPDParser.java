package com.sony.tv.app.atsc3receiver1_0.app;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

/**
 * Created by xhamc on 3/24/17.
 */

public class MPDParser {

    private XmlPullParserFactory factory;
    private XmlPullParser xpp;

    public MPD mpd;
    public StringBuilder stringBuilder=new StringBuilder(10000);
    private HashMap<String, ContentFileLocation> videoMap;
    private HashMap<String, ContentFileLocation> audioMap;
    private String data;


    public MPDParser(String data, HashMap<String, ContentFileLocation> videoMap, HashMap<String, ContentFileLocation> audioMap){
        this.videoMap=videoMap;
        this.audioMap=audioMap;
        this.data=data;
        stringBuilder.append("<?xml version=\"1.0\"?>\n");
        mpd=new MPD(stringBuilder);
    }


    public boolean MPDParse()
    {
        try {
            factory = XmlPullParserFactory.newInstance();
            xpp = factory.newPullParser();
            StringReader s=new StringReader(data);
            xpp.setInput(s);
            int eventType = xpp.getEventType();
            while (eventType!=XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_DOCUMENT) {

                } else if(eventType == XmlPullParser.START_TAG) {
                    mpd.addTag(xpp);
                } else if(eventType == XmlPullParser.END_TAG) {

                } else if(eventType == XmlPullParser.TEXT) {
                    mpd.addText(xpp);
                }else{

                }
                eventType = xpp.next();
            }
            return true;
        }catch (Exception e){
            e.printStackTrace();
        }

        return false;
    }

    public StringBuilder MPDgenerate(){
        return mpd.toStringBuilder();
    }

    public StringBuilder MPDdynamic(){
        mpd.toDynamic(videoMap, audioMap);
        return mpd.toStringBuilder();
    }


}
