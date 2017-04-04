package com.sony.tv.app.atsc3receiver1_0.app;

import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSpec;

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
 * Created by xhamc on 4/2/17.
 */

public class FluteFileManagerNAB implements FluteFileManagerBase {


    private static final long AVAILABILITY_TIME_OFFSET=2500;
    private static final String MIN_BUFFER_TIME="PT10S";
    private static final String TIME_SHIFT_BUFFER_OFFSET="PT0S";
    private static final String MINIMUM_UPDATE_PERIOD="PT0.75S";
    private static final String SUGGESTED_PRESENTATION_DELAY="PT0S";

    //        HashMap<String, ContentFileLocation> mapContentLocations;
    private static final String TAG="FileManager";
    public static final int MAX_SIGNALING_BUFFERSIZE=10000;
    public static final int MAX_VIDEO_BUFFERSIZE=40000000;
    public static final int MAX_AUDIO_BUFFERSIZE=2000000;
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
    private HashMap<Integer/*BufferNo*/, Integer/*TSI*/> mapGetTSIFromBufferNumber;             //retrieve the relevant FileManager from the TSI value
    private HashMap<Integer/*TSI*/, Integer/*BufferNo*/> mapGetBufferNumberFromTSI;             //retrieve the relevant FileManager from the TSI value

    private byte[] signalingStorage;
    private byte[] videoStorage;
    private byte[] audioStorage;

    private static ArrayList<byte[]> storage=new ArrayList<>();
    private ReentrantLock lock = new ReentrantLock();
    private byte[] patchedMPD;
    private int[] firstAvailablePosition={0,0,0};
    private int[] maxAvailablePosition={MAX_SIGNALING_BUFFERSIZE-1,  MAX_VIDEO_BUFFERSIZE-1, MAX_AUDIO_BUFFERSIZE-1,};

    //    private static FluteFileManager sInstance=new FluteFileManager();
    private FluteTaskManagerNAB mFluteTaskManager;
    private static FluteFileManagerNAB sInstance;
    public DataSpec baseDataSpec;

    private static boolean first;
    private static long availabilityStartTime;
    private static long availabilityStartTimeOffset;

    private SLS sls=new SLS();

    public FluteFileManagerNAB(DataSpec dataSpec){
        sInstance=this;
        baseDataSpec=dataSpec;
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
        //These get added to when discovered from STSID
        mapGetTSIFromBufferNumber.put(0, 0);
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


    public FluteFileManagerNAB getInstance(){ return sInstance; }
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

    /**
     * Open a file to read from buffer
     * @param dataSpec  path
     * @param thread    which exoplayer thread is accessing
     * @return          How many bytes can be read
     * @throws IOException
     */
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
            if (baseDataSpec.uri.getHost().equals(host) && baseDataSpec.uri.getPort()==port){

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

    /**
     * Player reads from buffers. Emulate the action of a stream
     * @param buffer        destination buffer
     * @param offset        offset into the buffer
     * @param readLength    max length to write into the buffer
     * @param thread        tags the Exoplayer DataSource to allow multithreading
     * @return              actual bytes written
     * @throws IOException
     */
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


    /**
     * Create the mapping for the buffer storage locations based on filename
     * @param r     Route header
     * @return      return true if the buffer has been allocated to the relevant TSI
     * @throws Exception
     */
    public boolean create(RouteDecodeBase r) throws Exception {
        int tsi=r.tsi();
        Integer index=mapGetBufferNumberFromTSI.get(tsi);
        if (null==index){
            Log.e(TAG,"Asking for tsi that isn't there: :"+tsi);
            return false;
        }
        HashMap<Integer,String> t=array_MapTOI_FileName.get(index);                                       //TOI maps to a filename
        HashMap<String, ContentFileLocation> m=arrayMapFileLocations.get(index);
        firstAvailablePosition[index] = (firstAvailablePosition[index] + r.contentLength()) > maxAvailablePosition[index] ? 0 : firstAvailablePosition[index];
        Date now=Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();
        ContentFileLocation c = new ContentFileLocation(r.fileName(), r.toi(), firstAvailablePosition[index], r.contentLength(),
                now.getTime(), now.getTime() + MAX_FILE_RETENTION_MS);
        Iterator<Map.Entry<String,ContentFileLocation>> iterator= m.entrySet().iterator();
        while (iterator.hasNext()) {                                                                      //remove any content from the map that will get overwritten by this file
            Map.Entry<String,ContentFileLocation> it= iterator.next();
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
        t.put(r.toi(),c.fileName);
        firstAvailablePosition[index] += r.contentLength();

        snapshot_of_Filemanager();
        return true;
    }

    /**
     * write the contents received into required buffer.
     *
     * @param r         The decoded header information
     * @param input     data received
     * @param offset    header offset to get to real payload
     * @param length    length of rela payload
     * @return          return the filename is the write is complete else empty string;
     */
    public String write(RouteDecodeBase r, byte[] input, int offset, int length){
        int toi=r.toi();
        lock.lock();
        try{
//            Log.d(TAG,"TSI: "+r.tsi()+"  TOI: "+r.toi()+ " POS: "+r.arrayPosition()+"  Length: "+r.contentLength());
            if (r.tsi() == 0  && r.arrayPosition()==0) {
                if (r.toi()!=0) {                                                            //signaling
                    r.fileName("sls.xml.new");
                    create(r);
                }else{
                    return  "";                                                              //EFDT instance, do nothing for now as not needed!!!!
                }
            }else if (r.toi() == 0xFFFFFFFF && r.tsi() != 0 && r.arrayPosition()==0) {       //init file
                r.fileName(generateInitFileName(r.tsi()));
                if (!("").equals(r.fileName())) {
                    r.fileName(r.fileName().concat(".new"));
                    create(r);
                }
            } else if (r.arrayPosition()==0) {                                               //media file
                r.fileName(generateFileName(r.toi(), r.tsi()));
                if (!("").equals(r.fileName())) {
                    r.fileName (r.fileName().concat(".new"));
                    create(r);
                }
            }
            if (!mapGetBufferNumberFromTSI.containsKey(r.tsi())){
//                Log.d(TAG, "TSI value not found yet so skip object");
                return "";
            }

            int index=mapGetBufferNumberFromTSI.get(r.tsi());                               //maps to the storage buffer assigned this tsi

            String fileName;

            HashMap<Integer,String> t=array_MapTOI_FileName.get(index);                     //TOI maps to a filename
            HashMap<String, ContentFileLocation> m=arrayMapFileLocations.get(index);        //and the fileName maps to the COntent location in buffer info
            if (t.containsKey(toi) && m.containsKey(t.get(toi))){
                ContentFileLocation l= m.get(t.get(toi));
                if (length<=(l.contentLength - l.nextWritePosition)){
                    System.arraycopy(input, offset, storage.get(index), l.start + l.nextWritePosition, length);
                    l.nextWritePosition+=length;
                    if (l.nextWritePosition==l.contentLength){
                        fileName=l.fileName.substring(0,l.fileName.length()-4);    //Finished so copy object to not ".new"
                        Iterator<Map.Entry<Integer, String>> it=t.entrySet().iterator();
                        while (it.hasNext()){
                            Map.Entry<Integer, String> entry=it.next();
                            if (entry.getValue().equals(fileName)){
                                it.remove();
                            }
                        }
                        m.remove(l.fileName);
                        l.fileName=fileName;
                        Date now= Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();
                        l.time=now.getTime();
                        m.put(fileName, l);
                        t.remove(toi);
                        t.put(toi, fileName);
                        if (r.tsi() == 0){
                            sls.create(l,storage.get(index));
                            mapGetBufferNumberFromTSI.put(sls.stsidParser.getTSI(0),1); //TODO: Use bw to determine video if there is both audio and video
                            mapGetTSIFromBufferNumber.put(1,sls.stsidParser.getTSI(0));

                        }


                        Log.d(TAG, "Wrote file: "+ fileName +" of size "+ l.contentLength + " to buffer: "+index + "which is connected to TSI: " + mapGetTSIFromBufferNumber.get(index));
                        return fileName;
                    }
                }else {
                    Log.e(TAG, "Attempt at buffer write overrun: "+l.fileName);
                }
            }
            return "";
        } catch (Exception e){
            e.printStackTrace();
            return "";
        }finally{
            lock.unlock();
        }

    }

    private FileBuffer openInternal(String fileName, int thread){
        Log.d(TAG,"Opening new File: ***********"+fileName);

        int index=0; ContentFileLocation f; int contentLength=0;

        if (fileName.toLowerCase().endsWith(".mpd")) {
            String mpdData;
            if (ATSC3.FAKEMANIFEST) {
                mpdData = ATSC3.manifestContents;
            }else{
                mpdData=sls.getManifest();
            }

            mMPDbytes = parseManifest(mpdData);
            contentLength=mMPDbytes.length;

            return new FileBuffer(mMPDbytes, contentLength, 0);

        }else{

            do {
                f = arrayMapFileLocations.get(index).get(fileName);
                index++;
            }while (index < arrayMapFileLocations.size() && f==null) ;

            if (f != null) {
                index--;
                return new FileBuffer(storage.get(index), f.contentLength, f.start);
            }else{
                Log.d(TAG,"Couldn't fine file while trying to open: "+fileName);
                return null;

            }
        }

    }


    private byte[] parseManifest(String mpdData){

        if (first){
            MPDParser mpdParser=new MPDParser(mpdData, mapFileLocationsVid, mapFileLocationsAud);
            mpdParser.MPDParse();

            availabilityStartTimeOffset=AVAILABILITY_TIME_OFFSET;
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
//        availabilityStartTimeOffset=AVAILABILITY_TIME_OFFSET;
//        Date now= c.getTime();
//        availabilityStartTime=now.getTime()+AVAILABILITY_TIME_OFFSET;

        String availabilityStartTimeString= formatter.format(availabilityStartTime);
        Log.d(TAG, availabilityStartTimeString);

        MPDParser mpdParser=new MPDParser(mpdHeader, mapFileLocationsVid, mapFileLocationsAud);
        mpdParser.MPDParse();
//        mpdParser.mpd.getAttributes().put("minBufferTime",MIN_BUFFER_TIME);
//        mpdParser.mpd.getAttributes().put("timeShiftBufferOffset",TIME_SHIFT_BUFFER_OFFSET);
        mpdParser.mpd.getAttributes().put("minimumUpdatePeriod",MINIMUM_UPDATE_PERIOD);
        mpdParser.mpd.getAttributes().put("suggestedPresentationDelay",SUGGESTED_PRESENTATION_DELAY);
        mpdParser.mpd.getAttributes().put("availabilityStartTime",availabilityStartTimeString);
        mpdData=mpdParser.mMPDgenerate().toString().split("</MPD>")[0].concat(mpdDataSplit[1]);

        return mpdData.getBytes();

//
//        String finalResult=mpdParser.mMPDgenerate().toString();
////        Log.d(TAG, finalResult);
//        return finalResult.getBytes();
    }

    private void snapshot_of_Filemanager(){

    }



    private String generateFileName(int toi, int tsi){

        if (null!=sls.getSTSIDParse()){

            String template=sls.mapTSItoFileTemplate.get(tsi);
            if (null!=template){
                String toi$ = String.format("%d", toi);
                template=template.replaceAll("\\$TOI\\$", toi$);
                if (!template.startsWith("/"))
                    template="/".concat(template);
                return template;
            }

        }
        return "";
    }

    private String generateInitFileName(int tsi){

        if (null!=sls.getSTSIDParse()){

            String template=sls.mapTSItoFileInitTemplate.get(tsi);
            if (null!=template) {
                if (!template.startsWith("/"))
                    template="/".concat(template);
                return template;
            }

        }
        return "";
    }

    private class SLS{

        private String manifest="";
        private String usbd="";
        private String stsid="";
        private String fileTemplate="";
        private STSIDParser stsidParser;
        public HashMap<Integer,String> mapTSItoFileTemplate=new HashMap<>();
        public HashMap<Integer,String> mapTSItoFileInitTemplate=new HashMap<>();



        public void create(ContentFileLocation contentFileLocation, byte[] storage){
            String sls=new String(storage,contentFileLocation.start,contentFileLocation.contentLength);
            if (extractManifest(sls)){
                //TODO create a manifest file in buffer
            }
            if (extractUSBD(sls)){
                //TODO create a usbd file in buffer
            }
            if (extractSTSID(sls)){

            }

        }


        public String getManifest(){
            return manifest;
        }
        public String getUSBD(){
            return usbd;
        }
        public String getSTSID(){
            return stsid;
        }
        public STSIDParser getSTSIDParse(){
            return stsidParser;
        }

        public void mapTSIToFileTemplate(){
            for (int i=0; i<stsidParser.getLSSize(); i++){
                mapTSItoFileTemplate.put(stsidParser.getTSI(i),stsidParser.getFileTempate(i));
            }
        }

        public void mapTSIToInitTemplate(){
            for (int i=0; i<stsidParser.getLSSize(); i++){
                mapTSItoFileInitTemplate.put(stsidParser.getTSI(i),stsidParser.getFileInitTempate(i));
            }
        }

        private boolean extractManifest(String sls){
            if (sls.contains("<MPD") && sls.contains("/MPD>")){
                int start=sls.indexOf("<MPD");
                int end=sls.indexOf("/MPD>")+5;
                manifest= ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").concat(sls.subSequence(start,end).toString());
                return true;
            }
            return false;
        }
        private boolean extractUSBD (String sls){
            if (sls.contains("<bundleDescriptionROUTE") && sls.contains("/bundleDescriptionROUTE>")){
                int start=sls.indexOf("<bundleDescriptionROUTE");
                int end=sls.indexOf("/bundleDescriptionROUTE>")+24;
                usbd= ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").concat(sls.subSequence(start,end).toString());
                return true;
            }
            return false;


        }
        private boolean extractSTSID(String sls){
            if (sls.contains("<S-TSID") && sls.contains("/S-TSID>")){
                int start=sls.indexOf("<S-TSID");
                int end=sls.indexOf("/S-TSID>")+8;
                stsid= ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").concat(sls.subSequence(start,end).toString());
                stsidParser=new STSIDParser(stsid);
                mapTSIToFileTemplate();
                mapTSIToInitTemplate();

                return true;
            }
            return false;
        }


    }






}
