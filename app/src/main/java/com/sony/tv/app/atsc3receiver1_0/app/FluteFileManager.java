package com.sony.tv.app.atsc3receiver1_0.app;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;




/**
 * Created by xhamc on 3/21/17.
 */

public class FluteFileManager {
    //        HashMap<String, ContentFileLocation> mapContentLocations;
    private static final String TAG="FileManager";
    private static final int MAX_SIGNALING_BUFFERSIZE=10000;
    private static final int MAX_VIDEO_BUFFERSIZE=10000000;
    private static final int MAX_AUDIO_BUFFERSIZE=1000000;
    private static final int MAX_FILE_RETENTION_MS=10000;

    private HashMap<String, ContentFileLocation> mapFileLocationsSig=new HashMap<>();
    private HashMap<String, ContentFileLocation> mapFileLocationsAud=new HashMap<>();
    private HashMap<String, ContentFileLocation> mapFileLocationsVid=new HashMap<>();

    private ArrayList<HashMap<String, ContentFileLocation>> arrayMapFileLocations = new ArrayList<>();
    private HashMap<Integer,String> map_TOI_FileNameSig = new HashMap<>();
    private HashMap<Integer,String> map_TOI_FileNameAud = new HashMap<>();
    private HashMap<Integer,String> map_TOI_FileNameVid = new HashMap<>();
    private ArrayList<HashMap<Integer,String>> array_MapTOI_FileName =new ArrayList<>();
    private HashMap<Integer, Short> mapGetTSIFromBufferNumber=new HashMap<>();             //retrieve the relevant FileManager from the TSI value
    private HashMap<Short, Integer> mapGetBufferNumberFromTSI=new HashMap<>();             //retrieve the relevant FileManager from the TSI value

    private byte[] signalingStorage=new byte[MAX_SIGNALING_BUFFERSIZE];
    private byte[] videoStorage=new byte[MAX_VIDEO_BUFFERSIZE];
    private byte[] audioStorage=new byte[MAX_AUDIO_BUFFERSIZE];

    private ArrayList<byte[]> storage=new ArrayList<>();
    private ReentrantLock lock = new ReentrantLock();
    private byte[] patchedMPD;
    private int[] firstAvailablePosition={0,0,0};
    private int[] maxAvailablePosition={MAX_SIGNALING_BUFFERSIZE-1,  MAX_VIDEO_BUFFERSIZE-1, MAX_AUDIO_BUFFERSIZE-1,};

    private static FluteFileManager sInstance=new FluteFileManager();

    private FluteFileManager(){
        arrayMapFileLocations.add(mapFileLocationsSig);
        arrayMapFileLocations.add(mapFileLocationsVid);
        arrayMapFileLocations.add(mapFileLocationsAud);
        array_MapTOI_FileName.add(map_TOI_FileNameSig);
        array_MapTOI_FileName.add(map_TOI_FileNameVid);
        array_MapTOI_FileName.add(map_TOI_FileNameAud);

            /*default mapping of TSI to buffer positions*/
        mapGetTSIFromBufferNumber.put(0,(short) 0);
        mapGetTSIFromBufferNumber.put(1,(short) 1);
        mapGetTSIFromBufferNumber.put(2,(short) 2);
        mapGetBufferNumberFromTSI.put((short) 0, 0);
        mapGetBufferNumberFromTSI.put((short) 1, 1);
        mapGetBufferNumberFromTSI.put((short) 2, 2);

        storage.add(signalingStorage);
        storage.add(videoStorage);
        storage.add(audioStorage);

    }

    public static FluteFileManager getInstance(){ return sInstance; }

    private static final int MPD=0;
    private static final int SLS=1;
    private static final int AV=2;

    private byte[] mMPDbytes;

    private HashMap<Integer, byte[]> threadBufferPointer=new HashMap<>();

    private int[] readStart=new int[3];
    private int[] readPosition=new int [3];
    private int[] readLength=new int[3];


    public int open(String fileName, int thread) throws IOException{
        Log.d(TAG,"Opening new File: ***********"+fileName + "  at thread:  "+thread);
        lock.lock();
        try{
            short tsi; int index=0; ContentFileLocation f;
            if (fileName.toLowerCase().contains("usbd.xml") || fileName.toLowerCase().contains("s-tsid.xml") ){

                f = arrayMapFileLocations.get(index).get(fileName);
                if (f != null) {
                    threadBufferPointer.put(thread, signalingStorage);
                    readPosition[thread]=0;
                    readStart[thread]=arrayMapFileLocations.get(index).get(fileName).start;
                    readLength[thread]=f.contentLength;
                    return readLength[thread];
                } else{
                    throw new IOException("Couldn't fine file while trying to open: "+fileName);
                }
            }else if (fileName.toLowerCase().endsWith(".mpd")){
                f = arrayMapFileLocations.get(index).get(fileName);
                if (f!=null) {
                    String mpdData = new String(storage.get(0), f.start, f.contentLength);
                    mMPDbytes = MPDParse(mpdData);
                    readPosition[thread] = 0;
                    readLength[thread] = mMPDbytes.length;
                    readStart[thread] = 0;
                    threadBufferPointer.put(thread, mMPDbytes);
                    return readLength[thread];
                }else{
                    throw new IOException("Couldn't fine file while trying to open: "+fileName);
                }
            } else {
                index=1;
                f = arrayMapFileLocations.get(index).get(fileName);     //Test for audio filename
                if (f==null){
                    index=2;
                    f=arrayMapFileLocations.get(index).get(fileName);  //test for Video filename
                }
                if (f != null) {
                    readPosition[thread]=0;
                    readStart[thread]=arrayMapFileLocations.get(index).get(fileName).start;
                    readLength[thread]=f.contentLength;
                    if (index==1) threadBufferPointer.put(thread,audioStorage);
                    if (index==2) threadBufferPointer.put(thread,videoStorage);
                    return readLength[thread];
                }
                else {
                    throw new IOException("Couldn't fine file while trying to open: "+fileName);
                }
            }


        }finally{

            lock.unlock();
        }
    }

    public int read(byte[] output, int offset,  int length, int thread) throws IOException{
        lock.lock();
        try {

            if (null!=threadBufferPointer.get(thread) && length>0){

//                readPosition[thread]+=offset;
                int len=Math.min(length,readLength[thread]-readPosition[thread]);
                if (len<=0){
                    Log.d("TEST:","reading 0 bytes from readPosition: " + (readStart[thread]+readPosition[thread]) + "on thread  "+ thread);

                    return -1;
                }

                if (len==1){
                    Log.d("TEST:","reading "+ len +" bytes from readPosition: " + (readStart[thread]+readPosition[thread]) + "on thread  "+ thread);

                    output[offset] =threadBufferPointer.get(thread)[readStart[thread]+readPosition[thread]];
                    readPosition[thread]++;
                }else if (len<25){
                    Log.d("TEST:","reading "+ len +" bytes from readPosition: " + (readStart[thread]+readPosition[thread]) + "on thread  "+ thread);

                    for (int i=0; i<len; i++){
                        output[i+offset]=threadBufferPointer.get(thread)[readStart[thread]+readPosition[thread]];
                        readPosition[thread]++;
                    }
                }else{
                    Log.d("TEST:","reading "+ len +" bytes from readPosition: " + (readStart[thread]+readPosition[thread]) + "on thread  "+ thread);

                    System.arraycopy(threadBufferPointer.get(thread),readStart[thread]+readPosition[thread],output,offset,len);
                    readPosition[thread]+=len;
                }

                return len;
            }
            throw new IOException("Attempt to read from no existent buffer or read zero bytes");
        }finally{
            lock.unlock();
        }
    }


    public int read(String fileName, byte[] output, int offset,  int length){
        lock.lock();
        try {
            short tsi; int index=0; ContentFileLocation f;
            if (fileName.toLowerCase().contains("usbd.xml") || fileName.toLowerCase().contains("s-tsid.xml") || fileName.toLowerCase().endsWith(".mpd")){

                f = arrayMapFileLocations.get(0).get(fileName);
            }else {
                index=1;
                tsi=mapGetTSIFromBufferNumber.get(index);          //Audio tsi
                f = arrayMapFileLocations.get(tsi).get(fileName);     //Test for audio filename
                if (f==null){
                    index=2;
                    tsi=mapGetTSIFromBufferNumber.get(index);          //Video tsi
                    f=arrayMapFileLocations.get(tsi).get(fileName);  //test for Video filename
                }
            }
            if (f != null) {
                int bytesToFetch = Math.min(length, f.contentLength - offset);
                bytesToFetch = (bytesToFetch < 0) ? 0 : bytesToFetch;
                int startPosition = f.start + offset;
                System.arraycopy(storage.get(index), startPosition, output, 0, bytesToFetch);
                return bytesToFetch;
            }
            else {
                Log.d(TAG,"File not found whilst reading: "+fileName);
            }
            return 0;
        }finally{
            lock.unlock();
        }
    }





    public boolean write(RouteDecode r, byte[] input, int offset, int length){
        int toi=r.toi;
        lock.lock();
        try{
            int index=mapGetBufferNumberFromTSI.get(r.tsi);
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
                        m.put(fileName, l);
                        t.remove(toi);
                        t.put(toi, fileName);
                        Log.d(TAG, "Wrote file: "+ fileName +" of size "+ l.contentLength + " to buffer: "+index + "which is connected to TSI: " + mapGetTSIFromBufferNumber.get(index));



                    }
                }else {
                    Log.e(TAG, "Attempt at buffer write overrun: "+l.fileName);
                }
            }
            return false;
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

    public byte[] MPDParse(String mpdData ){

        String[] result=mpdData.split("(?<=availabilityStartTime[\\s]?=[\\s]?\"[0-9\\-]{10}[\\s]?[\\s]?)");
        String[] result2=result[2].split("[\"]+",2);

//        for (int i=0; i<result.length; i++) {
//            Log.d("TEST_REGEX", result[i]);
//        }
//        for (int i=0; i<result2.length; i++) {
//            Log.d("TEST_REGEX2", result2[i]);
//        }


        String finalresult=result[0].trim().concat("T").concat(result2[0].concat("Z").concat("\"").concat(result2[1]));
//        Log.d("TEST_REGEX",finalresult + "  with length: "+finalresult.length());
        return finalresult.getBytes();
    }

    public boolean create(RouteDecode r) {
        lock.lock();
        try {
            short tsi=r.tsi;
            int index=mapGetBufferNumberFromTSI.get(tsi);

            HashMap<Integer,String> t=array_MapTOI_FileName.get(index);
            HashMap<String, ContentFileLocation> m=arrayMapFileLocations.get(index);
            firstAvailablePosition[index] = (firstAvailablePosition[index] + r.contentLength) > maxAvailablePosition[index] ? 0 : firstAvailablePosition[index];
            ContentFileLocation c = new ContentFileLocation(r.fileName.concat(".new"), r.efdt_toi, firstAvailablePosition[index], r.contentLength, System.currentTimeMillis(), System.currentTimeMillis() + MAX_FILE_RETENTION_MS);
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
            t.put(r.efdt_toi,c.fileName);

            firstAvailablePosition[index] += r.contentLength;

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
}

