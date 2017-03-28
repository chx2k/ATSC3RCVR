package com.sony.tv.app.atsc3receiver1_0.app;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.exoplayer2.ParserException;

import org.xmlpull.v1.XmlPullParser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

import static com.google.android.exoplayer2.util.Util.parseXsDateTime;
import static com.google.android.exoplayer2.util.Util.parseXsDuration;

/**
 * Created by xhamc on 3/24/17.
 */

public final class MPD {
    private final static String TAG="MPD";
    private final String tag_MPD="MPD";
    private final String attr_xmlString="xmlns";
    private final String attr_minBufferTime="monBufferTime";
    private final String attr_minimumUpdatePeriod="minimumUpdatePeriod";
    private final String attr_type="type";
    private final String attr_availabilityStartTime="availabilityStartTime";
    private final String attr_timeShiftBufferDepth="timeShiftBufferDepth";
    private final String attr_mediaPresentationDuration="mediaPresentationDuration";
    private final String attr_profiles="profiles";
    private final String tag_ProgramInformation="ProgramInformation";
    private final String attr_moreInformationURL="moreInformationURL";
    private final String tag_Title="Title";
    private final String tag_Period="Period";
    private final String attr_start="start";
    private final String attr_duration="duration";
    private final String tag_BaseUrl="BaseUrl";
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
    private final String attr_timeScale="timescale";
    private final String attr_media="media";
    private final String attr_startNumber="startNumber";
    private final String attr_initialization="initialization";
    private final String tag_SegmentTemplate="SegmentTemplate";
    private final String tag_AudioChannelConfiguration="AudioChannelConfiguration";


    public String attributes;

    private ProgramInformation programInformation;
    private StringBuilder sb;
    public String getAttributes(){return attributes;}

    private HashMap<String, String> attrs=new HashMap<>();

    private ArrayList<Period> periods=new ArrayList<>();

    public MPD(StringBuilder sb){
        this.sb=sb;
    }

    private int currentPeriodCount=0;
    private int currentAdaptationSetCount=0;
    private int currentRepresentationsCount=0;
    private Object textObject;
    private boolean titleText=false;
    private BaseUrl currentbaseUrl;

    public void addTag(XmlPullParser xpp) {
        String tag=xpp.getName();
        if (tag.equals(tag_MPD)) {
            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                attrs.put(xpp.getAttributeName(i), xpp.getAttributeValue(i));
            }
        } else if (tag.equals(tag_ProgramInformation)){
            this.programInformation=new ProgramInformation(xpp) ;
        } else if (tag.equals(tag_Period)){
            periods.add(new Period(xpp));
            currentPeriodCount++;
            currentAdaptationSetCount=0;
            currentRepresentationsCount=0;
        }else if (tag.equals(tag_AdaptationSet)){
            periods.get(currentPeriodCount-1).addTag(new AdaptationSet(xpp));
            currentAdaptationSetCount++;
            currentRepresentationsCount=0;
        }else if (tag.equals(tag_Representation)){
            periods.get(currentPeriodCount-1).adaptationSet.get(currentAdaptationSetCount-1).addTag (new Representation(xpp));
            currentRepresentationsCount++;
        }else if (tag.equals(tag_SegmentTemplate)) {
            periods.get(currentPeriodCount - 1).adaptationSet.get(currentAdaptationSetCount - 1).representations.get(currentRepresentationsCount - 1).addTag(new SegmentTemplate(xpp));
        }else if (tag.equals(tag_AudioChannelConfiguration)){
            periods.get(currentPeriodCount - 1).adaptationSet.get(currentAdaptationSetCount - 1).representations.get(currentRepresentationsCount - 1).addTag(new AudioChannelConfiguration(xpp));
        }else if (tag.equals(tag_BaseUrl)){
            currentbaseUrl=new BaseUrl();
            if (currentRepresentationsCount>0){
                textObject=periods.get(currentPeriodCount-1).adaptationSet.get(currentAdaptationSetCount-1).representations.get(currentRepresentationsCount-1).addTag(currentbaseUrl);
            } else if (currentAdaptationSetCount>0){
                textObject=periods.get(currentPeriodCount-1).adaptationSet.get(currentAdaptationSetCount-1).addTag(currentbaseUrl);
            } else if (currentPeriodCount>0){
                textObject=periods.get(currentPeriodCount-1).addTag(currentbaseUrl);
            } else{
                Log.e(TAG, "Error parsing MPD: Base Url not associated with any tag");
            }
        } else if (tag.equals(tag_Title)){
            programInformation.title=new Title();
            textObject=programInformation.title;
        }
   }

    public void addText(XmlPullParser xpp){
        if (null==textObject){
            return;
        }
        String text=xpp.getText();
        try {
            textObject.getClass().getMethod("set",XmlPullParser.class).invoke(textObject,xpp);
            textObject=null;
        } catch (NoSuchMethodException e) {
            Log.e(TAG,"Writing text to a method that doesn't exist");
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public String getAttribute(String attribute){
        return getAttribute(this.attrs, attribute);
    }
    public void setAttribute(String attribute, String value){
        attrs.put(attribute,value);
    }

    public StringBuilder toStringBuilder(){
        sb.append("<MPD");
        Iterator<Map.Entry<String,String>> iterator=attrs.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String,String> it=iterator.next();
            sb.append(" ").append( it.getKey()).append("=\"").append(it.getValue()).append("\"");
        }
        sb.append(">\n");
        if (programInformation!=null){
            programInformation.addToStringBuffer();
        }

        for (int i=0; i<periods.size();i++){
            periods.get(i).addToStringBuffer();
        }
        sb.append("</MPD>");
        return sb;
    }

    public boolean toDynamic(@NonNull HashMap<String, ContentFileLocation> videos, @NonNull HashMap<String, ContentFileLocation> audios){
        try {

            int[] oldestVideoIndex=new int[periods.size()];
            int[] oldestAudioIndex=new int[periods.size()];
            int[] earliestVideoIndex=new int[periods.size()];
            int[] earliestAudioIndex=new int[periods.size()];
            long[] oldestVideoTime=new long[periods.size()];
            long[] oldestAudioTime=new long[periods.size()];
            long[] earliestVideoTime=new long[periods.size()];
            long[] earliestAudioTime=new long[periods.size()];


            int [] periodIndexStart=new int[periods.size()];
            long[] periodStartTime=new long[periods.size()];
            long[] periodDuration=new long[periods.size()];

            double[] videoSegmentDuration=new double[periods.size()];
            double[] audioSegmentDuration=new double[periods.size()];
            boolean[] videoIndexPresent = new boolean[periods.size()];
            boolean[] audioIndexPresent = new boolean[periods.size()];
            int[] adapSetVideoIndex=new int[periods.size()];
            int[] adapSetAudioIndex=new int[periods.size()];
            boolean [] periodToRemove=new boolean[periods.size()];

            long availabilityStartTime;


            for (int i=0; i<periods.size();i++){
                oldestVideoIndex[i]=Integer.MAX_VALUE;
                oldestAudioIndex[i]=Integer.MAX_VALUE;
                earliestVideoIndex[i]=0;
                earliestAudioIndex[i]=0;
                videoIndexPresent[i]=false;
                audioIndexPresent[i]=false;
                periodStartTime[i]=0;
                periodDuration[i]=0;
                periodIndexStart[i]=0;
                adapSetVideoIndex[i]=0;
                adapSetAudioIndex[i]=0;
            }

            getAvailabilityStartTimeFromVideos(videos);




                availabilityStartTime=parseXsDateTime(getAttribute(attr_availabilityStartTime));  //TODO We want the real startTime calculated by the receiving of packets.
                Log.d(TAG, "Availability Start Time in ms: "+availabilityStartTime);
//                for (int period=0; period<periods.size(); period++){
//                    oldestVideoTime[period]=(long) (oldestVideoIndex[period]*videoSegmentDuration[period]*1000);
//                    oldestAudioTime[period]=(long) (oldestAudioIndex[period]*audioSegmentDuration[period]*1000);
//                    earliestVideoTime[period]=(long) (earliestVideoIndex[period]*videoSegmentDuration[period]*1000);
//                    earliestAudioTime[period]=(long) (earliestAudioIndex[period]*audioSegmentDuration[period]*1000);
//
//                    Log.d(TAG, "Period No: "+period+"   videoStartTime: "+oldestVideoIndex[period]*videoSegmentDuration[period]+"   audioStartTime: "+ oldestAudioIndex[period]*audioSegmentDuration[period]);
//                    Log.d(TAG, "Period No: "+period+"   videoEndTime: "+earliestVideoIndex[period]*videoSegmentDuration[period]+"   audioEndTime: "+ earliestAudioIndex[period]*audioSegmentDuration[period]);
//                    periodStartTime[period]=parseXsDuration(periods.get(period).getAttribute(attr_start));         //TODO This depends on ExoPlayer library; recreate locally
//                    periodDuration[period]=parseXsDuration(periods.get(period).getAttribute(attr_duration));
//                    if (!audioIndexPresent[period] || !videoIndexPresent[period]) {          //no content in this period available so remove
//                        periodToRemove[period]=true;
//                    }
//                    int startVideoIndex=Integer.parseInt( periods.get(period).adaptationSet.get(adapSetVideoIndex[period]).representations.get(0).segmentTemplate.getAttribute("startNumber"));
//                    int startAudioIndex=Integer.parseInt( periods.get(period).adaptationSet.get(adapSetAudioIndex[period]).representations.get(0).segmentTemplate.getAttribute("startNumber"));
//                    if (startVideoIndex>0 || startAudioIndex>0) {
//                        oldestVideoTime[period] = (long) ((oldestVideoIndex[period] - startVideoIndex) * videoSegmentDuration[period] * 1000);
//                        oldestAudioTime[period] = (long) ((oldestAudioIndex[period] - startAudioIndex) * audioSegmentDuration[period] * 1000);
//                        if (oldestVideoTime[period]>=oldestAudioTime[period]){
//
//                        }else{
//
//                        }
//                    }
//                }
                availabilityStartTime+=FluteReceiver.getInstance().getTimeOffset();
                Log.d(TAG, "Availability adjusted Start Time in ms: "+availabilityStartTime);

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            String date=formatter.format(new Date(availabilityStartTime+ FluteReceiver.getInstance().timeOffset));

            Log.d(TAG, "Availability adjusted Start Time: "+date);

            setAttribute(attr_availabilityStartTime,date);

            setAttribute(attr_timeShiftBufferDepth,"PT5S");
                setAttribute("suggestedPresentationDelay","PT1.5S");



            }catch (Exception e){
            Log.e(TAG, e.getMessage());
            return false;
        }

        return true;
    }

    public long getAvailabilityStartTimeFromVideos(HashMap<String,ContentFileLocation> videos){

        long timeOffset=0;
        try {
            long availabilityStartTime=parseXsDateTime(getAttribute(attr_availabilityStartTime));  //TODO We want the real startTime calculated by the receiving of packets.
            loop:for (int period = 0; period < periods.size(); period++) {
                String baseUrl=periods.get(period).getAttribute(tag_BaseUrl)!=""?periods.get(period).getAttribute(tag_BaseUrl):"/";
                for (int adaptationSet = 0; adaptationSet < 2; adaptationSet++) {
                    baseUrl=baseUrl.concat(periods.get(period).adaptationSet.get(adaptationSet).getAttribute(tag_BaseUrl));
                    baseUrl=baseUrl.concat(periods.get(period).adaptationSet.get(adaptationSet).representations.get(0).getAttribute(tag_BaseUrl));

                    if (periods.get(period).adaptationSet.get(adaptationSet).representations.get(0).getAttribute("mimeType").contains("video")){
                        SegmentTemplate videoTemplate =periods.get(period).adaptationSet.get(adaptationSet).representations.get(0).segmentTemplate;
                        String[] check=baseUrl.concat(videoTemplate.getAttribute(attr_media)).split("\\$Number\\$");
                        for (Map.Entry<String,ContentFileLocation> video:videos.entrySet()){
                            if (!video.getKey().equals(baseUrl.concat(videoTemplate.getAttribute("initialization")))) {
                                if (video.getKey().startsWith(check[0]) && (video.getKey().endsWith(check[1]))) {
                                    int videoSegmentNumber = Integer.parseInt(video.getKey().replace(check[0], "").replace(check[1], ""));
                                    int periodStartNumber=Integer.parseInt(videoTemplate.getAttribute(attr_startNumber));
                                    double videoSegmentDuration=Double.parseDouble(periods.get(period).adaptationSet.get(adaptationSet).representations.get(0).segmentTemplate.getAttribute(attr_duration))/
                                            Double.parseDouble(periods.get(period).adaptationSet.get(adaptationSet).representations.get(0).segmentTemplate.getAttribute(attr_timeScale));
                                    long periodStartTime=parseXsDuration(periods.get(period).getAttribute(attr_start));
                                    long periodDuration=parseXsDuration(periods.get(period).getAttribute(attr_duration));
                                    long videoStartTime=video.getValue().time;
                                    long periodEndNumber=periodStartNumber+(long)(periodDuration/(videoSegmentDuration*1000));
                                    if (videoSegmentNumber>=periodStartNumber && videoSegmentNumber<periodEndNumber) {
                                        long videoSegmentOffset = (long) ((videoSegmentNumber - periodStartNumber) * videoSegmentDuration) + periodStartTime+availabilityStartTime;
                                        long availabilityTimeCalculated = videoStartTime - videoSegmentOffset;
                                        Log.d(TAG, "Difference in availability Times calc_from_videos - manifest: " + (availabilityTimeCalculated - availabilityStartTime));
                                        return availabilityTimeCalculated;
                                    }

                                }

                            }
                        }
                    }
                }

            }


        } catch (ParserException e) {
            e.printStackTrace();
        }
        return -1;
    }




    public class ProgramInformation{
        private HashMap<String, String> attrs=new HashMap<>();
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
            for (Map.Entry<String, String> it : attrs.entrySet()) {
                sb.append(" ").append(it.getKey()).append("=").append("\"").append(it.getValue()).append("\"");
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
        private String title;

        public Title Title(){
            return this;
        }
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
        public ArrayList<AdaptationSet> adaptationSet=new ArrayList<>();
        private HashMap<String, String> attrs=new HashMap<>();
        public Period(XmlPullParser xpp){
            for (int i=0; i<xpp.getAttributeCount(); i++){
                attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
            }
        }
        public Object addTag (BaseUrl bu){
            this.baseUrl=bu;
            return this;
        }
        public void addTag (AdaptationSet as){
            adaptationSet.add(as);
            currentRepresentationsCount=0;
        }

        public String getAttribute(String attribute){
            return MPD.getAttribute(attrs,attribute);
        }
        public void setAttribute(String attribute, String value){
            MPD.setAttribute(attrs, attribute, value);
        }

        public void addToStringBuffer(){
            sb.append("<Period");

            for (Map.Entry<String, String> it : attrs.entrySet()) {
                sb.append(" ").append(it.getKey()).append("=").append("\"").append(it.getValue()).append("\"");
            }
            if (null==baseUrl && null==adaptationSet){
                sb.append(" />\n");
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
        public void set(XmlPullParser xpp){
            this.baseUrl=xpp.getText();
        }
        public String get(){
            return baseUrl;
        }
        public void addToStringBuffer(){
            sb.append("<BaseUrl>").append(baseUrl).append(("</BaseUrl>\n"));
        }
    }


    public class AdaptationSet{

        private BaseUrl baseUrl;
        public ArrayList<Representation> representations=new ArrayList<>();
        private HashMap<String, String> attrs=new HashMap<>();
        public AdaptationSet(XmlPullParser xpp){
            for (int i=0; i<xpp.getAttributeCount(); i++){
                attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
            }
        }
        public Object addTag (BaseUrl bu){
            this.baseUrl=bu;
            return this;
        }
        public void addTag (Representation r){
            representations.add(r);
        }

        public String getAttribute(String attribute){
            return MPD.getAttribute(attrs,attribute);
        }
        public void setAttribute(String attribute, String value){
            MPD.setAttribute(attrs, attribute, value);
        }


        public void addToStringBuffer(){
            sb.append("<AdaptationSet");

            for (Map.Entry<String, String> it : attrs.entrySet()) {
                sb.append(" ").append(it.getKey()).append("=").append("\"").append(it.getValue()).append("\"");
            }
            if (null==baseUrl && null==representations){
                sb.append(" />\n");
                return;
            }
            sb.append(">\n");
            if (null!=baseUrl){
                baseUrl.addToStringBuffer();
            }
            for (int i=0; i<representations.size();i++){
                representations.get(i).addToStringBuffer();
            }
            sb.append("</AdaptationSet>\n");
        }
    }

    public class Representation{

        private BaseUrl baseUrl;
        private AudioChannelConfiguration audioChannelConfiguration;
        private SegmentTemplate segmentTemplate;
        private HashMap<String, String> attrs=new HashMap<>();
        public Representation(XmlPullParser xpp){
            for (int i=0; i<xpp.getAttributeCount(); i++){
                attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
            }
        }
        public Object addTag (BaseUrl bu){
            this.baseUrl=bu;
            return this;
        }
        public void addTag(SegmentTemplate st){
            segmentTemplate=st;
        }

        public void addTag(AudioChannelConfiguration acc){
            audioChannelConfiguration=acc;
        }

        public String getAttribute(String attribute){
            return MPD.getAttribute(attrs,attribute);
        }
        public void setAttribute(String attribute, String value){
            MPD.setAttribute(attrs, attribute, value);
        }



        public void addToStringBuffer(){
            sb.append("<Representation");
            Iterator<Map.Entry<String,String>> iterator=attrs.entrySet().iterator();

            while (iterator.hasNext()){
                Map.Entry<String,String> it=iterator.next();
                sb.append(" ").append( it.getKey()).append("=").append("\"").append(it.getValue()).append("\"");
            }
            if (null==baseUrl && null==segmentTemplate && null==audioChannelConfiguration ){
                sb.append(" />\n");
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

            sb.append("</Representation>\n");
        }


    }


    public class SegmentTemplate {
        private HashMap<String, String> attrs=new HashMap<>();

        public SegmentTemplate(XmlPullParser xpp) {
            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                attrs.put(xpp.getAttributeName(i), xpp.getAttributeValue(i));
            }
        }

        public String getAttribute(String attribute){
            return MPD.getAttribute(attrs,attribute);
        }
        public void setAttribute(String attribute, String value){
            MPD.setAttribute(attrs, attribute, value);
        }

        public void addToStringBuffer() {
                sb.append("<SegmentTemplate");
                Iterator<Map.Entry<String,String>> iterator=attrs.entrySet().iterator();
                while (iterator.hasNext()){
                    Map.Entry<String,String> it=iterator.next();
                    sb.append(" ").append( it.getKey()).append("=").append("\"").append(it.getValue()).append("\"");
                }
                sb.append(" />\n");
        }
    }

    public class AudioChannelConfiguration {
        private HashMap<String, String> attrs=new HashMap<>();

        public AudioChannelConfiguration(XmlPullParser xpp) {
            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                attrs.put(xpp.getAttributeName(i), xpp.getAttributeValue(i));
            }
        }

        public String getAttribute(String attribute){
            return MPD.getAttribute(attrs,attribute);
        }
        public void setAttribute(String attribute, String value){
            MPD.setAttribute(attrs, attribute, value);
        }

        public void addToStringBuffer() {
            sb.append("<AudioChannelConfiguration");
            Iterator<Map.Entry<String,String>> iterator=attrs.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<String,String> it=iterator.next();
                sb.append(" ").append( it.getKey()).append("=").append("\"").append(it.getValue()).append("\"");
            }
            sb.append(" />");


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


//    for (int period = 0; period < periods.size(); period++) {
//        String baseUrl=periods.get(period).getAttribute(tag_BaseUrl)!=""?periods.get(period).getAttribute(tag_BaseUrl):"/";
//        for (int adaptationSet = 0; adaptationSet < 2; adaptationSet++) {
//            baseUrl=baseUrl.concat(periods.get(period).adaptationSet.get(adaptationSet).getAttribute(tag_BaseUrl));
//            baseUrl=baseUrl.concat(periods.get(period).adaptationSet.get(adaptationSet).representations.get(0).getAttribute(tag_BaseUrl));
//
//            if (periods.get(period).adaptationSet.get(adaptationSet).representations.get(0).getAttribute("mimeType").contains("video")){
//                adapSetVideoIndex[period]=adaptationSet;
//                SegmentTemplate videoTemplate =periods.get(period).adaptationSet.get(adaptationSet).representations.get(0).segmentTemplate;
//                videoSegmentDuration[period]=Double.parseDouble(periods.get(period).adaptationSet.get(adaptationSet).representations.get(0).segmentTemplate.getAttribute(attr_duration))/
//                        Double.parseDouble(periods.get(period).adaptationSet.get(adaptationSet).representations.get(0).segmentTemplate.getAttribute(attr_timeScale));
//
//                String[] check=baseUrl.concat(videoTemplate.getAttribute(attr_media)).split("\\$Number\\$");
//
//                for (Map.Entry<String,ContentFileLocation> video:videos.entrySet()){
//                    if (!video.getKey().equals(baseUrl.concat(videoTemplate.getAttribute("initialization")))) {
//                        if (video.getKey().startsWith(check[0]) && (video.getKey().endsWith(check[1]))) {
//                            videoIndexPresent[period]=true;
//                            int result = Integer.parseInt(video.getKey().replace(check[0], "").replace(check[1], ""));
//                            oldestVideoIndex[period] = Math.min(result, oldestVideoIndex[period]);
//                            earliestVideoIndex[period] = Math.max(result, earliestVideoIndex[period]);
//                        }
//
//                    }
//                }
//            } else {
//                adapSetAudioIndex[period]=adaptationSet;
//                SegmentTemplate audioTemplate = periods.get(period).adaptationSet.get(adaptationSet).representations.get(0).segmentTemplate;
//                audioSegmentDuration[period] = Double.parseDouble(periods.get(period).adaptationSet.get(adaptationSet).representations.get(0).segmentTemplate.getAttribute(attr_duration)) /
//                        Double.parseDouble(periods.get(period).adaptationSet.get(adaptationSet).representations.get(0).segmentTemplate.getAttribute(attr_timeScale));
//                String[] check = baseUrl.concat(audioTemplate.getAttribute(attr_media)).split("\\$Number\\$");
//
//                for (Map.Entry<String, ContentFileLocation> audio : audios.entrySet()) {
//                    if (!audio.getKey().equals(baseUrl.concat(audioTemplate.getAttribute(attr_initialization)))) {
//                        if (audio.getKey().startsWith(check[0]) && (audio.getKey().endsWith(check[1]))) {
//                            audioIndexPresent[period]=true;
//                            int result = Integer.parseInt(audio.getKey().replace(check[0], "").replace(check[1], ""));
//                            oldestAudioIndex[period] = Math.min(result, oldestAudioIndex[period]);
//                            earliestAudioIndex[period] = Math.max(result, earliestAudioIndex[period]);
//                        }
//
//                    }
//                }
//            }
//        }
//
//    }

}
