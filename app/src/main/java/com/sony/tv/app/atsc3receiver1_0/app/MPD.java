package com.sony.tv.app.atsc3receiver1_0.app;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by xhamc on 3/24/17.
 */

public final class MPD {

    private final String tag_MPD="MPD";
    private final String attr_xmlString="xmlns";
    private final String attr_minBufferTime="monBufferTime";
    private final String attr_minimumUpdatePeriod="minimumUpdatePeriod";
    private final String attr_type="type";
    private final String attr_availabilityStartTime="availabilityStartTime";
    private final String attr_timeShiftBufferDepth="timeSHiftBufferDepth";
    private final String attr_mediaPresentationDuration="mediaPresentationDuration";
    private final String attr_profiles="profiles";
    private final String tag_ProgramInformation="ProgramInformation";
    private final String attr_moreInformationURL="moreInformationURL";
    private final String tag_Period="Period";
    private final String attr_start="start";
    private final String attr_duration="duration";
    private final String tag_AdaptationSet="AdaptationSet";
    private final String attr_segmentAligment="segmentAlignment";
    private final String attr_maxWidth="maxWidth";
    private final String attr_maxHeight="maxHeight";
    private final String attr_maxFrameRate="maxFrameRate";
    private final String attr_par="par";
    private final String attr_lang="lang";
    private final String tag_Representation="Representation";
    private final String attr_id="id";
    private final String attr_mimeType="mimeType";
    private final String attr_codecs="codecs";
    private final String attr_width="width";
    private final String attr_height="height";
    private final String attr_frameRate="frameRate";
    private final String attr_sar="sar";
    private final String attr_startsWithSAP="startsWithSAP";
    private final String attr_bandwidth="bandwidth";
    private final String tag_SegmentTemplate="SegmentTemplate";
    public String attributes;

    private ProgramInformation programInformation;
    private StringBuilder sb=new StringBuilder();
    public String getAttributes(){return attributes;}

    private HashMap<String, String> attrs;

    private ArrayList<Period> periods;

    public MPD(XmlPullParser xpp){
        for (int i=0; i<xpp.getAttributeCount(); i++){
            attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
        }
    }
    public void addPeriod(XmlPullParser xpp){
        periods.add(new Period(xpp));

    }
    public String getAttribute(String attribute){
        return getAttribute(this.attrs, attribute);
    }
    public String toString(){
        sb.append("<MPD");
        Iterator<Map.Entry<String,String>> iterator=attrs.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String,String> it=iterator.next();
            sb.append(" ").append( it.getKey()).append("=\"").append(it.getValue()).append("\"");
        }
        sb.append(">\n");

        for (int i=0; i<periods.size();i++){
            periods.get(i).addToStringBuffer();
        }
        sb.append("</MPD>");
        return sb.toString();
    }



    public class ProgramInformation{
        private String text;
        private HashMap<String, String> attrs;
        public Title title;
        public ProgramInformation(XmlPullParser xpp){
            for (int i=0; i<xpp.getAttributeCount(); i++){
                attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
            }
        }
        public String getAttribute(String attribute){
            return MPD.getAttribute(attrs,attribute);
        }
        public void setAttribute(String attribute, String value){
            MPD.setAttribute(attrs, attribute, value);
        }

        public void addToStringBuffer(){
            sb.append("<ProgramInformation");
            Iterator<Map.Entry<String,String>> iterator=attrs.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<String,String> it=iterator.next();
                sb.append(" ").append( it.getKey()).append("=").append("\"").append(it.getValue()).append("\"");
            }
            if (null!=title) {
                sb.append(">\n");
                title.addToStringBuffer();
                sb.append("</ProgramInformation>\n");
            }else {
                sb.append(" />");
            }
        }
    }

    public class Title{
        public String title;
        public void  set(XmlPullParser xpp){
            this.title=xpp.getText();
        }
        public String get(){
            return title;
        }
        public void addToStringBuffer(){
            sb.append("<Title>").append(title).append("</Title>\n");
        }
    }

    public class Period{
        private BaseUrl baseUrl;
        private ArrayList<AdaptationSet> adaptationSet;
        private HashMap<String, String> attrs;
        public Period(XmlPullParser xpp){
            for (int i=0; i<xpp.getAttributeCount(); i++){
                attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
            }
        }
        public String getAttribute(String attribute){
            return MPD.getAttribute(attrs,attribute);
        }
        public void setAttribute(String attribute, String value){
            MPD.setAttribute(attrs, attribute, value);
        }

        public void addToStringBuffer(){
            sb.append("<Period");
            Iterator<Map.Entry<String,String>> iterator=attrs.entrySet().iterator();

            while (iterator.hasNext()){
                Map.Entry<String,String> it=iterator.next();
                sb.append(" ").append( it.getKey()).append("=").append("\"").append(it.getValue()).append("\"");
            }
            if (null==baseUrl || null==adaptationSet){
                sb.append(" />");
                return;
            }
            sb.append(">\n");
            if (null!=baseUrl){
                baseUrl.addToStringBuffer();
            }
            for (int i=0; i<adaptationSet.size();i++) {
                adaptationSet.get(i).addToStringBuffer();
            }
            sb.append("</Period>\n");
        }
    }

    public class BaseUrl{
        public String baseUrl;
        public void  set(XmlPullParser xpp){
            this.baseUrl=xpp.getText();
        }
        public String get(){
            return baseUrl;
        }
        public void addToStringBuffer(){
            sb.append("<BaseUrl>").append(baseUrl).append(("</BaseUrl>\n"));
        }
    }

    public class Representation{

        private BaseUrl baseUrl;
        private AudioChannelConfiguration audioChannelConfiguration;
        private SegmentTemplate segmentTemplate;
        private HashMap<String, String> attrs;
        public Representation(XmlPullParser xpp){
            for (int i=0; i<xpp.getAttributeCount(); i++){
                attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
            }
        }
        public String getAttribute(String attribute){
            return MPD.getAttribute(attrs,attribute);
        }
        public void setAttribute(String attribute, String value){
            MPD.setAttribute(attrs, attribute, value);
        }

        public void addToStringBuffer(){
            sb.append("<Period");
            Iterator<Map.Entry<String,String>> iterator=attrs.entrySet().iterator();

            while (iterator.hasNext()){
                Map.Entry<String,String> it=iterator.next();
                sb.append(" ").append( it.getKey()).append("=").append("\"").append(it.getValue()).append("\"");
            }
            if (null==baseUrl || null==segmentTemplate || null==audioChannelConfiguration ){
                sb.append(" />");
                return;
            }
            sb.append(">\n");
            if (null!=baseUrl){
                baseUrl.addToStringBuffer();
            }
            if (null!=audioChannelConfiguration){
                audioChannelConfiguration.addToStringBuffer();
            }
            if (null!=segmentTemplate){
                segmentTemplate.addToStringBuffer();
            }

            sb.append("</Period>\n");
        }


    }

    public class AdaptationSet{

        private BaseUrl baseUrl;
        private ArrayList<Representation> representation;
        private HashMap<String, String> attrs;
        public AdaptationSet(XmlPullParser xpp){
            for (int i=0; i<xpp.getAttributeCount(); i++){
                attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
            }
        }
        public String getAttribute(String attribute){
            return MPD.getAttribute(attrs,attribute);
        }
        public void setAttribute(String attribute, String value){
            MPD.setAttribute(attrs, attribute, value);
        }

        public void addToStringBuffer(){
            sb.append("<Period");
            Iterator<Map.Entry<String,String>> iterator=attrs.entrySet().iterator();

            while (iterator.hasNext()){
                Map.Entry<String,String> it=iterator.next();
                sb.append(" ").append( it.getKey()).append("=").append("\"").append(it.getValue()).append("\"");
            }
            if (null==baseUrl || null==representation){
                sb.append(" />");
                return;
            }
            sb.append(">\n");
            if (null!=baseUrl){
                baseUrl.addToStringBuffer();
            }
            for (int i=0; i<representation.size();i++){
                representation.get(i).addToStringBuffer();
            }
            sb.append("</Period>\n");
        }


    }



    public class SegmentTemplate {
        private HashMap<String, String> attrs;

        public SegmentTemplate(XmlPullParser xpp) {
            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                attrs.put(xpp.getAttributeName(i), xpp.getAttributeValue(i));
            }
        }

        public void addToStringBuffer() {


        }
    }

    public class AudioChannelConfiguration {
        private HashMap<String, String> attrs;

        public AudioChannelConfiguration(XmlPullParser xpp) {
            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                attrs.put(xpp.getAttributeName(i), xpp.getAttributeValue(i));
            }
        }

        public void addToStringBuffer() {


        }

    }
    private static void setAttribute(HashMap<String,String> attrs, String attribute, String value){
        attrs.put(attribute,value);
        Iterator<Map.Entry<String,String>> iterator=attrs.entrySet().iterator();
    }

    private static String getAttribute(HashMap<String,String> attrs, String attribute){
        Iterator<Map.Entry<String,String>> iterator=attrs.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String,String> it=iterator.next();
            if (it.getKey().equals(attribute)) {
                return it.getValue();
            }
        }
        return "";
    }

}
