package com.sony.tv.app.atsc3receiver1_0.app;

import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.util.Util;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.locks.ReentrantLock;




/**
 * Created by xhamc on 3/21/17.
 */

public class FluteFileManager  implements FluteFileManagerBase {

    private static final long AVAILABILITY_TIME_OFFSET=3500;
    private static final String MIN_BUFFER_TIME="PT1S";
    private static final String TIME_SHIFT_BUFFER_OFFSET="PT3S";
    private static final String MINIMUM_UPDATE_PERIOD="PT0.75S";
    private static final String SUGGESTED_PRESENTATION_DELAY="PT0S";

    //        HashMap<String, ContentFileLocation> mapContentLocations;
    private static final String TAG="FileManager";
    private static final int MAX_SIGNALING_BUFFERSIZE=10000;
    private static final int MAX_VIDEO_BUFFERSIZE=20000000;
    private static final int MAX_AUDIO_BUFFERSIZE=2000000;
    private static final int MAX_FILE_RETENTION_MS=10000;

    private static final int SERVER_TIME_OFFSET=7100;

    private HashMap<String, ContentFileLocation> mapFileLocationsSig;
    private HashMap<String, ContentFileLocation> mapFileLocationsAud;
    private HashMap<String, ContentFileLocation> mapFileLocationsVid;

    private ArrayList<HashMap<String, ContentFileLocation>> arrayMapFileLocations;
    private HashMap<Integer,String> map_TOI_FileNameSig;
    private HashMap<Integer,String> map_TOI_FileNameAud;
    private HashMap<Integer,String> map_TOI_FileNameVid;
    private ArrayList<HashMap<Integer,String>> array_MapTOI_FileName =new ArrayList<>();
    private HashMap<Integer, Integer> mapGetTSIFromBufferNumber;             //retrieve the relevant FileManager from the TSI value
    private HashMap<Integer, Integer> mapGetBufferNumberFromTSI;             //retrieve the relevant FileManager from the TSI value

    private byte[] signalingStorage;
    private byte[] videoStorage;
    private byte[] audioStorage;




    private ArrayList<byte[]> storage=new ArrayList<>();
    private ReentrantLock lock = new ReentrantLock();
    private byte[] patchedMPD;
    private int[] firstAvailablePosition={0,0,0};
    private int[] maxAvailablePosition={MAX_SIGNALING_BUFFERSIZE-1,  MAX_VIDEO_BUFFERSIZE-1, MAX_AUDIO_BUFFERSIZE-1,};

//    private static FluteFileManager sInstance=new FluteFileManager();
    private FluteTaskManager mFluteTaskManager;
    private static FluteFileManager sInstance;
    private DataSpec signalingDataSpec;
    private DataSpec avDataSpec;



    private static boolean first;
    private static long availabilityStartTime;
    private static long availabilityStartTimeOffset;

    public FluteFileManager(DataSpec dataSpec){
        sInstance=this;
        signalingDataSpec=dataSpec;

    }

    public FluteFileManager(DataSpec signallingDataSpec, DataSpec av){
        sInstance=this;
        this.signalingDataSpec=signallingDataSpec;
        this.avDataSpec=av;
    }

    public void reset(){

        mapFileLocationsSig=new HashMap<>();
        mapFileLocationsAud=new HashMap<>();
        mapFileLocationsVid=new HashMap<>();

        arrayMapFileLocations = new ArrayList<>();
        map_TOI_FileNameSig = new HashMap<>();
        map_TOI_FileNameAud = new HashMap<>();
        map_TOI_FileNameVid = new HashMap<>();
        array_MapTOI_FileName =new ArrayList<>();
        mapGetTSIFromBufferNumber=new HashMap<>();             //retrieve the relevant FileManager from the TSI value
        mapGetBufferNumberFromTSI=new HashMap<>();             //retrieve the relevant FileManager from the TSI value

        signalingStorage=new byte[MAX_SIGNALING_BUFFERSIZE];
        videoStorage=new byte[MAX_VIDEO_BUFFERSIZE];
        audioStorage=new byte[MAX_AUDIO_BUFFERSIZE];

        arrayMapFileLocations.add(0,mapFileLocationsSig);
        arrayMapFileLocations.add(1,mapFileLocationsVid);
        arrayMapFileLocations.add(2,mapFileLocationsAud);
        array_MapTOI_FileName.add(0,map_TOI_FileNameSig);
        array_MapTOI_FileName.add(1,map_TOI_FileNameVid);
        array_MapTOI_FileName.add(2,map_TOI_FileNameAud);

            /*default mapping of TSI to buffer positions*/
        mapGetTSIFromBufferNumber.put(0,0);
        mapGetTSIFromBufferNumber.put(1,1);
        mapGetTSIFromBufferNumber.put(2,2);
        mapGetBufferNumberFromTSI.put(0, 0);
        mapGetBufferNumberFromTSI.put(1, 1);
        mapGetBufferNumberFromTSI.put(2, 2);

        storage.add(0,signalingStorage);
        storage.add(1,videoStorage);
        storage.add(2,audioStorage);

        first=true;

    }

    public void resetTimeStamp(){
        first=true;
    }


    public FluteFileManager getInstance(){ return sInstance; }
//    public static FluteFileManager getInstance(){ return sInstance; }


    private static final int MPD=0;
    private static final int SLS=1;
    private static final int AV=2;

    private byte[] mMPDbytes;

    private HashMap<Integer, byte[]> threadBufferPointer=new HashMap<>();

    private int[] bytesToRead=new int[100];
    private int[] bytesRead=new int [100];
    private int[] bytesToSkip=new int[100];
    private int[] byteOffset=new int[100];
    private boolean[] timeOffsetFirst=new boolean[100];

    private class FileBuffer{
        public byte[] buffer;
        public int contentLength;
        public int startOfContent;
        public FileBuffer(byte[] buffer, int contentLength, int startOfContent){
            this.buffer=buffer; this.contentLength=contentLength; this.startOfContent=startOfContent;
        }
    }
    private FileBuffer[] fileBuffer=new FileBuffer[100];


    public long open(DataSpec dataSpec, int thread) throws IOException {
        lock.lock();
//        mFluteTaskManager=FluteReceiver.getInstance().mFluteTaskManager;

        try{

            bytesToRead[thread]=0;
            bytesToSkip[thread]=0;
            bytesRead[thread]=0;
            byteOffset[thread]=0;


            Log.d("TAG", "ExoPlayer trying to open :"+dataSpec.uri);
            String host = dataSpec.uri.getHost();
            int port = dataSpec.uri.getPort();
//            if ( mFluteTaskManager.dataSpec.uri.getHost().equals(host) && mFluteTaskManager.dataSpec.uri.getPort()==port){
            if (signalingDataSpec.uri.getHost().equals(host) && signalingDataSpec.uri.getPort()==port ||
                    avDataSpec.uri.getHost().equals(host) && avDataSpec.uri.getPort()==port){

            String path=dataSpec.uri.getPath();
                bytesToSkip[thread]=(int) dataSpec.position;
                FileBuffer fb = openInternal(path, thread);
                if (fb==null){
                    Log.d(TAG, "Couldn't fine file while trying to open: "+path);

                    //throw new IOException("Couldn't fine file while trying to open: "+path);
                    return -1;
                }else{
                    fileBuffer[thread]=fb;
                    bytesToRead[thread]=fb.contentLength-bytesToSkip[thread];
                    byteOffset[thread]=fb.startOfContent+bytesToSkip[thread];
                }
                return (bytesToRead[thread]-bytesToSkip[thread]);


            } else{

                throw new IOException("Attempted to open a url that is not active: ".concat(dataSpec.toString()));
            }

        }finally{

            lock.unlock();
        }

    }

    public int read(byte[] buffer, int offset, int readLength, int thread) throws IOException {
        lock.lock();
        try {
            if (readLength == 0) {
                return 0;
            }
            if (bytesToRead[thread] != C.LENGTH_UNSET) {
                long bytesRemaining = bytesToRead[thread] - bytesRead[thread];
                if (bytesRemaining == 0) {
                    return C.RESULT_END_OF_INPUT;
                }
                readLength = (int) Math.min(readLength, bytesRemaining);
            } else {
                return C.LENGTH_UNSET;
            }
            if (readLength < 0) {
                Log.d(TAG, "BytesToRead: " + bytesToRead + "  bytesRead: " + bytesRead + " readLength:  " + readLength);
                throw new IOException("Read Length is less than 0");

            }
            if (byteOffset[thread] + bytesRead[thread] + readLength < MAX_VIDEO_BUFFERSIZE) {
                System.arraycopy(fileBuffer[thread].buffer, byteOffset[thread] + bytesRead[thread], buffer, offset, readLength);
                bytesRead[thread] += readLength;
            } else {
                Log.e(TAG, "Error trying to read from local buffer, overrun: bytesRead: " + bytesRead[thread] + "  byteOffset: " + byteOffset[thread] + "  length:  " + readLength);
            }
            //            listenertener.onBytesTransferred(this, read);
            //        }
            return readLength;
        }finally{
            lock.unlock();
        }
    }

    private FileBuffer openInternal(String fileName, int thread){
        Log.d(TAG,"Opening new File: ***********"+fileName);

        int index=0; ContentFileLocation f; int contentLength=0;
        do {
            f = arrayMapFileLocations.get(index).get(fileName);
            index++;
        }while (index < arrayMapFileLocations.size() && f==null) ;

        if (f != null) {
            index--;
            if (fileName.toLowerCase().endsWith(".mpd")) {
                String mpdData = new String(storage.get(0), f.start, f.contentLength);
                Date d = getAvailabilityStartTime(mpdData);
                mMPDbytes = MPDParse(mpdData);
                contentLength=mMPDbytes.length;

                return new FileBuffer(mMPDbytes, contentLength, 0);


            }else {

                return new FileBuffer(storage.get(index),  f.contentLength,   f.start);

            }

        } else{
            Log.d(TAG,"Couldn't fine file while trying to open: "+fileName);
            return null;
        }

    }

//
//    public int read(String fileName, byte[] output, int offset,  int length){
//        lock.lock();
//        try {
//            short tsi; int index=0; ContentFileLocation f;
//            if (fileName.toLowerCase().contains("usbd.xml") || fileName.toLowerCase().contains("s-tsid.xml") || fileName.toLowerCase().endsWith(".mpd")){
//
//                f = arrayMapFileLocations.get(0).get(fileName);
//            }else {
//                index=1;
//                tsi=mapGetTSIFromBufferNumber.get(index);          //Audio tsi
//                f = arrayMapFileLocations.get(tsi).get(fileName);     //Test for audio filename
//                if (f==null){
//                    index=2;
//                    tsi=mapGetTSIFromBufferNumber.get(index);          //Video tsi
//                    f=arrayMapFileLocations.get(tsi).get(fileName);  //test for Video filename
//                }
//            }
//            if (f != null) {
//                int bytesToFetch = Math.min(length, f.contentLength - offset);
//                bytesToFetch = (bytesToFetch < 0) ? 0 : bytesToFetch;
//                int startPosition = f.start + offset;
//                System.arraycopy(storage.get(index), startPosition, output, 0, bytesToFetch);
//                return bytesToFetch;
//            }
//            else {
//                Log.d(TAG,"File not found whilst reading: "+fileName);
//            }
//            return 0;
//        }finally{
//            lock.unlock();
//        }
//    }





    public String write(RouteDecodeBase r, byte[] input, int offset, int length){
        int toi=r.toi();
        lock.lock();
        try{
            int index=mapGetBufferNumberFromTSI.get(r.tsi());
            HashMap<Integer,String> t=array_MapTOI_FileName.get(index);
            HashMap<String, ContentFileLocation> m=arrayMapFileLocations.get(index);

            if (t.containsKey(toi) && m.containsKey(t.get(toi))){
                ContentFileLocation l= m.get(t.get(toi));
                if (length<=(l.contentLength - l.nextWritePosition)){
                    System.arraycopy(input, offset, storage.get(index), l.start + l.nextWritePosition, length);
                    l.nextWritePosition+=length;
                    if (l.nextWritePosition==l.contentLength){
                        String fileName=l.fileName.substring(0,l.fileName.length()-4);    //Finished so copy object to not ".new"
                        Iterator<Map.Entry<Integer, String>> it=t.entrySet().iterator();
                        while (it.hasNext()){
                            Map.Entry<Integer, String> entry=it.next();
                            if (entry.getValue().equals(fileName)){
                                it.remove();
                            }
                        }
                        m.remove(l.fileName);
                        l.fileName=fileName;
                        Date now=Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();
                        l.time=now.getTime();
                        m.put(fileName, l);
                        t.remove(toi);
                        t.put(toi, fileName);
                        Log.d(TAG, "Wrote file: "+ fileName +" of size "+ l.contentLength + " to buffer: "+index + "which is connected to TSI: " + mapGetTSIFromBufferNumber.get(index));
                        return fileName;  //complete
                    }
                }else {
                    Log.e(TAG, "Attempt at buffer write overrun: "+l.fileName);
                }
            }
            return "";
        } finally{
            lock.unlock();
        }

    }
    //        public boolean delete(String fileName) {
//            if (locations.containsKey(fileName)) {
//                locations.remove(fileName);
//                return true;
//            }
//            return false;
//        }

    public Date getAvailabilityStartTime(String mpdData){

        TimeZone timeZone = TimeZone.getTimeZone("UTC");

        Calendar c= Calendar.getInstance(timeZone);
        Date now=c.getTime();
        String[] result=mpdData.split("availabilityStartTime[\\s]?=[\\s]?\"");
        String[] result2=result[1].split("[\"]+",2);
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        formatter.setTimeZone(timeZone);

        try{
            Date mpd=formatter.parse(result2[0]);
            Log.d(TAG, "Time now minus time MPD created in s: "+ (double)(now.getTime()-mpd.getTime())/1000);

//            formatter = new SimpleDateFormat("yyyy-MM-dd'T'H:mm:ss.SSS'Z'");

            return mpd;

        }catch (ParseException e){
            Log.e(TAG, "Error parsing date from MPD");
            return null;
        }
    }



    public byte[] MPDParse(String mpdData){




        if (first){
            MPDParser mpdParser=new MPDParser(mpdData, mapFileLocationsVid, mapFileLocationsAud);
            mpdParser.MPDParse();
//            if (!"".equals(mpdParser.mpd.getAttribute("availabilityStartTimeOffset")))
//                availabilityStartTimeOffset=Long.parseLong(mpdParser.mpd.getAttribute("availabilityStartTimeOffset"));
//            else
                availabilityStartTimeOffset=AVAILABILITY_TIME_OFFSET;

//            Log.d(TAG,"AvailabilityStartTimeOffset set to "+availabilityStartTimeOffset);
            availabilityStartTime=mpdParser.mpd.getAvailabilityStartTimeFromVideos(mapFileLocationsVid)+availabilityStartTimeOffset;
            Log.d(TAG,"AvailabilityStartTime set to "+availabilityStartTime);
            first=false;
        }
        String[] mpdSplit=mpdData.split("\\?>",2);
        String mpdHeaderStart=mpdSplit[1];
        String[] mpdDataSplit=mpdHeaderStart.split(">",2);
        String mpdHeader=mpdDataSplit[0].concat(">");

        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        Calendar c= Calendar.getInstance(timeZone);
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        formatter.setTimeZone(timeZone);
        String availabilityStartTimeString= formatter.format(new Date(availabilityStartTime));

        MPDParser mpdParser=new MPDParser(mpdHeader, mapFileLocationsVid, mapFileLocationsAud);
        mpdParser.MPDParse();
        mpdParser.mpd.getAttributes().put("minBufferTime",MIN_BUFFER_TIME);
        mpdParser.mpd.getAttributes().put("timeShiftBufferOffset",TIME_SHIFT_BUFFER_OFFSET);
        mpdParser.mpd.getAttributes().put("minimumUpdatePeriod",MINIMUM_UPDATE_PERIOD);
        mpdParser.mpd.getAttributes().put("suggestedPresentationDelay",SUGGESTED_PRESENTATION_DELAY);
        mpdParser.mpd.getAttributes().put("availabilityStartTime",availabilityStartTimeString);
        mpdData=mpdParser.mMPDgenerate().toString().split("</MPD>")[0].concat(mpdDataSplit[1]);


//        String[] result=mpdData.split("availabilityStartTime[\\s]?=[\\s]?\"");
//        String[] result2=result[1].split("[\"]+",2);
//        mpdData=result[0].concat("availabilityStartTime=\"").concat(availabilityStartTimeString).concat("\"").concat(result2[1]);
//        Log.d(TAG,"AvailabilityStartTime= "+availabilityStartTimeString);

//        double durationUs=(c.getTime().getTime()- (availabilityStartTime)+2500 )/1000;
//        String duration=String.format("PT%1.2fS", durationUs);
//        result=mpdData.split("mediaPresentationDuration[\\s]?=[\\s]?\"");
//        result2=result[1].split("[\"]+",2);
//
//        mpdData=result[0].concat("mediaPresentationDuration=\"").concat(duration).concat("\"").concat(result2[1]);
////
//        String[] result=mpdData.split("(?<=availabilityStartTime[\\s]?=[\\s]?\"[0-9\\-]{10}[\\s]?[\\s]?)");
//        String[] result2=result[2].split("[\"]+",2);
//        mpdData=result[0].trim().concat("T").concat(result2[0].concat("Z").concat("\"").concat(result2[1]));
        return mpdData.getBytes();

//
//        String finalResult=mpdParser.mMPDgenerate().toString();
////        Log.d(TAG, finalResult);
//        return finalResult.getBytes();
    }

    public boolean create(RouteDecodeBase r) throws Exception {
        lock.lock();
        try {
            int tsi=r.tsi();
            int index=mapGetBufferNumberFromTSI.get(tsi);
            if (tsi>2){
                Log.e(TAG,"Asking for tsi> 2: :"+tsi);
                throw new Exception("Asking for tsi> 2: :"+tsi);
            }
            HashMap<Integer,String> t=array_MapTOI_FileName.get(index);
            HashMap<String, ContentFileLocation> m=arrayMapFileLocations.get(index);
            firstAvailablePosition[index] = (firstAvailablePosition[index] + r.contentLength()) > maxAvailablePosition[index] ? 0 : firstAvailablePosition[index];
            Date now=Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();

            ContentFileLocation c = new ContentFileLocation(r.fileName().concat(".new"), r.efdt_toi(), firstAvailablePosition[index], r.contentLength(),
                                                                now.getTime(), now.getTime() + MAX_FILE_RETENTION_MS);
            Iterator<Map.Entry<String,ContentFileLocation>> iterator= m.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String,ContentFileLocation> it= iterator.next();
//                    if(it.getKey().endsWith(".new")){
//                        Log.e(TAG, "Creating new file but one already in play: ");
//                        Log.e(TAG,"fileName: "+it.getValue().fileName);
//                        Log.e(TAG,"ContentLength: "+it.getValue().contentLength);
//                        Log.e(TAG,"Start: "+it.getValue().start);
//                        Log.e(TAG,"Next Write position: "+it.getValue().nextWritePosition);
//                        Log.e(TAG,"Toi: "+it.getKey());
//                    }
                int itStart=it.getValue().start;
                int itEnd=it.getValue().start+it.getValue().contentLength-1;
                int cStart=c.start;
                int cEnd=c.start+c.contentLength-1;
                if ( (itStart<=cStart && itEnd>=cStart) || (itStart>=cStart && itStart<=cEnd) ) {             //the new content will overwrite this entry so remove from list;

                    t.remove(it.getValue().toi);
                    iterator.remove();

                }
            }

            m.put(c.fileName, c);
            t.put(r.efdt_toi(),c.fileName);

            firstAvailablePosition[index] += r.contentLength();

            snapshot_of_Filemanager();
            return true;

        } finally {
            lock.unlock();
        }
    }

    public void snapshot_of_Filemanager(){

//        Log.d(TAG, "FileManager Array Sizes: Signalling "+ array_MapTOI_FileName.get(0).size()+ "  "+ arrayMapFileLocations.get(0).size());
//        Log.d(TAG, "FileManager Array Sizes: Video      "+ array_MapTOI_FileName.get(1).size()+ "  "+ arrayMapFileLocations.get(1).size());
//        Log.d(TAG, "FileManager Array Sizes: Audio      "+ array_MapTOI_FileName.get(2).size()+ "  "+ arrayMapFileLocations.get(2).size());


//
//        for (Map.Entry<Integer,String> entry : array_MapTOI_FileName.get(0).entrySet() ){
//            Log.d(TAG,"Signaling: Toi mapping to filenames:   size: "+array_MapTOI_FileName.get(0).size()+"TOI: "+entry.getKey()+"   FileName:  "+entry.getValue());
//        }
//        for (Map.Entry<String,ContentFileLocation> entry : arrayMapFileLocations.get(0).entrySet() ){
//            Log.v(TAG,"Signaling: Filename mapping to ContentLocations:  size:  "+ entry.getValue().contentLength+"FileName: "+entry.getKey() +"   TOI:  "+entry.getValue().toi);
//        }
////        for (Map.Entry<Integer,String> entry : array_MapTOI_FileName.get(1).entrySet() ){
////            Log.d(TAG,"Video: Toi mapping to filenames:   size: "+array_MapTOI_FileName.get(1).size()+"TOI: "+entry.getKey()+"   FileName:  "+entry.getValue());
////        }
//        for (Map.Entry<String,ContentFileLocation> entry : arrayMapFileLocations.get(1).entrySet() ){
//            Log.v(TAG,"Video: Filename mapping to ContentLocations:  size:  "+ entry.getValue().contentLength+"FileName: "+entry.getKey() +"   TOI:  "+entry.getValue().toi);
//        }
////        for (Map.Entry<Integer,String> entry : array_MapTOI_FileName.get(2).entrySet() ){
////            Log.d(TAG,"Audio: Toi mapping to filenames:   size: "+array_MapTOI_FileName.get(2).size()+"TOI: "+entry.getKey()+"   FileName:  "+entry.getValue());
////        }
//        for (Map.Entry<String,ContentFileLocation> entry : arrayMapFileLocations.get(2).entrySet() ){
//            Log.v(TAG,"Audio: Filename mapping to ContentLocations:  size:  "+ entry.getValue().contentLength +"FileName: "+entry.getKey() +"   TOI:  "+entry.getValue().toi);
//        }
    }

    public  String write (RouteDecodeNAB r, byte[] input, int offset, int length){
        return "";
    }
}

