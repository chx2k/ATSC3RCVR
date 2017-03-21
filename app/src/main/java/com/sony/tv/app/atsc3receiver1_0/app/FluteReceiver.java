package com.sony.tv.app.atsc3receiver1_0.app;

import android.app.Activity;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.upstream.UdpDataSource;
import com.google.android.exoplayer2.util.SystemClock;
import com.sony.tv.app.atsc3receiver1_0.MainActivity;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import static com.sony.tv.app.atsc3receiver1_0.app.FluteDataSource.AUDIO_CONTENT;
import static com.sony.tv.app.atsc3receiver1_0.app.FluteDataSource.SIGNALLING;
import static com.sony.tv.app.atsc3receiver1_0.app.FluteDataSource.VIDEO_CONTENT;


/**
 * Created by xhamc on 3/18/17.
 */

public class FluteReceiver {

    private static final int MAX_SIGNALING_BUFFERSIZE=10000;
//    private static  final int MAX_USBD_BUFFERSIZE=1000;
//    private static final int MAX_STSID_BUFFERSIZE=1000;
    private static final int MAX_VIDEOINIT_BUFFERSIZE=1000;
    private static final int MAX_AUDIOINIT_BUFFERSIZE=1000;
    private static final int MAX_VIDEO_BUFFERSIZE=10000000;
    private static final int MAX_AUDIO_BUFFERSIZE=10000000;
    private static final int MAX_FILE_RETENTION_MS=10000;


    private  FileManager signalingFiles=new FileManager(MAX_SIGNALING_BUFFERSIZE, SIGNALLING);
    private FileManager mVideoPacketData=new FileManager(MAX_VIDEO_BUFFERSIZE, VIDEO_CONTENT);
    private FileManager mAudioPacketData=new FileManager(MAX_AUDIO_BUFFERSIZE, AUDIO_CONTENT);
    private FluteTaskManager mSignalingFluteTaskManager;
    private FluteTaskManager mAudioContentFluteTaskManager;
    private FluteTaskManager mVideoContentFluteTaskManager;

    private static DataSpec dataSpec;
    private UdpDataSource udpDataSource;
    private byte[] bytes;
    private Handler mHandler;
    private static final int TASK_ERROR=-1;
    private static final int TASK_COMPLETE=1;
    private static final int TASK_STARTED=2;
    private static final int TASK_STOPPED=3;
    private static final int FOUND_FLUTE_PACKET=4;
    private static final int FOUND_FLUTE_INSTANCE=5;
    private static final int FOUND_FLUTE_FILE=6;
    private static final String TAG="FLUTE";
    private static FluteReceiver sInstance=new FluteReceiver();



    /**
     * FluteReceiver is singleton
     */
    private FluteReceiver() {

//        dataSpec = new DataSpec(Uri.parse("udp://224.0.23.60:4937"));
        mHandler=new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                FluteTaskManager flutetask= (FluteTaskManager) inputMessage.obj;


                switch (inputMessage.what) {
                    // The decoding is done
//                    case TASK_COMPLETE:
//                        Log.d(TAG,"FOUND LLS MESSAGES");
//                        break;
                    case TASK_STARTED:
                        Log.d(TAG,"STARTED");
                        break;
                    case FOUND_FLUTE_PACKET:
//                        Log.d(TAG,"FOUND FLUTE PACKET: current size of locations: "+flutetask.fileManager.locations.size())
                        ;
                        break;
                    case FOUND_FLUTE_INSTANCE:
                        Log.d(TAG,"FOUND FLUTE INSTANCE");

                        break;
                    case FOUND_FLUTE_FILE:
                        break;
                    case TASK_ERROR:
                        Log.d(TAG,"FOUND ERROR: "+flutetask.error);
                        break;
                    case TASK_STOPPED:
                        Log.d(TAG,"STOP REQUEST ISSUED");

                        break;
                    default:
                        super.handleMessage(inputMessage);

                }
            }
        };
    }


    /**
     *
     * @return instance of this singleton FluteReceiver
     */
    public static FluteReceiver getInstance(){
        return sInstance;
    }

    /**
     * start the task manager
     */
    public void start(int type, DataSpec dataSpec) {

        if (type == SIGNALLING) {
            mSignalingFluteTaskManager = new FluteTaskManager(dataSpec, signalingFiles);
            new Thread(mSignalingFluteTaskManager).start();
        } else if (type == AUDIO_CONTENT) {
            mAudioContentFluteTaskManager = new FluteTaskManager(dataSpec, mAudioPacketData);
            new Thread(mAudioContentFluteTaskManager).start();
        } else if (type == VIDEO_CONTENT) {
            mVideoContentFluteTaskManager = new FluteTaskManager(dataSpec, mVideoPacketData);
            new Thread(mVideoContentFluteTaskManager).start();
        }
    }

    /**
     * stop the task manager
     */
    public void stop(int type){
        if (type == SIGNALLING) {
            mSignalingFluteTaskManager.stop();
        }else if (type== AUDIO_CONTENT) {
            mAudioContentFluteTaskManager.stop();
        }else if (type== VIDEO_CONTENT) {
            mVideoContentFluteTaskManager.stop();
        }
    }

    /**
     * handleTaskState is passes Flute Datagram packets to UI thread
     * @param task  The FluteTaskManager holding the data
     * @param state The state of the task manager (error, completed ...)
     */
    public void handleTaskState(FluteTaskManager task, int state){
//        Log.d(TAG,"Message to send: "+state);

        (mHandler.obtainMessage(state, task)).sendToTarget();
    }

    /**
     * Class to handle getting udp flute data and decoding it
     */
    private class FluteTaskManager implements Runnable{

        public String error;
        private int packetSize;
        private boolean running;
        Boolean stopRequest;
        DataSpec dataSpec;
        FileManager fileManager;

        FluteTaskManager (DataSpec dataSpec, FileManager f ){
            stopRequest=false;
            this.fileManager=f;
            this.dataSpec=dataSpec;
        }

        @Override
        public void run(){
            Log.d(TAG, "New thread running FluteTaskManager created");

            udpDataSource = new UdpDataSource(new TransferListener<UdpDataSource>() {
                @Override
                public void onTransferStart(UdpDataSource source, DataSpec dataSpec) {
                    running=true;
                }

                @Override
                public void onBytesTransferred(UdpDataSource source, int bytesTransferred) {
                    packetSize=bytesTransferred;
                }
                @Override
                public void onTransferEnd(UdpDataSource source) {
                    running=false;
                }
            });

            try {
                udpDataSource.open(dataSpec);
            } catch (UdpDataSource.UdpDataSourceException e) {
                e.printStackTrace();
                return;
            }
            mainloop:while (!stopRequest && running) {

                int len;
                int offset = 0;
                bytes = new byte[UdpDataSource.DEFAULT_MAX_PACKET_SIZE];
                do {
                    try {
                        len = udpDataSource.read(bytes, offset, UdpDataSource.DEFAULT_MAX_PACKET_SIZE);
                        offset += len;
                    } catch (UdpDataSource.UdpDataSourceException e) {
                        e.printStackTrace();
//                        reportError();
                        break mainloop;
                    }
                } while (offset < packetSize);

                transferDataToFluteHandler(bytes, packetSize);

            }
            udpDataSource.close();
        }

        public void stop(){
            stopRequest=true;
            sInstance.handleTaskState(this, TASK_STOPPED);
        }

        private short maxObjectSize=0;
        private int contentLength=0;
        private boolean packetStillInProgress=false;
        private int bytesReceived=0;
        private int latestToi;
        private int trap_a_Toi_Out_Of_Order=-1;
        private void transferDataToFluteHandler(byte[] bytes, int packetSize ){

            sInstance.handleTaskState(this, FOUND_FLUTE_PACKET);
            String fileName;
            RouteDecode routeDecode=new RouteDecode(bytes, packetSize);
//
            if (routeDecode.efdt) {
                if (packetStillInProgress) {
                    Log.e(TAG, "Route decode PACKET still in progress, contentLength: " + contentLength + "  maxObject Size: " + maxObjectSize +"  bytes received: "+bytesReceived);
                }
                if (routeDecode.efdt_toi==trap_a_Toi_Out_Of_Order){
                    Log.e(TAG, "Found a toi out of order :"+trap_a_Toi_Out_Of_Order);
                    trap_a_Toi_Out_Of_Order=-1;
                }
                maxObjectSize=routeDecode.maxObjectSize;
                contentLength=routeDecode.contentLength;
                bytesReceived=0;
                latestToi=routeDecode.efdt_toi;
            }else{

                int adjustedPacketSize=packetSize-RouteDecode.PAYLOAD_START_POSITION;
                bytesReceived+=adjustedPacketSize;
                if (contentLength>maxObjectSize){
                    if (adjustedPacketSize >=maxObjectSize){
                        if (adjustedPacketSize>maxObjectSize){
                            Log.e(TAG,"Route decode PACKET is too big: "+adjustedPacketSize);
                        }else{
                            if (bytesReceived<contentLength){
                                packetStillInProgress = true;
                            }else if (bytesReceived==contentLength){
                                packetStillInProgress=false;
                            }else{
                                Log.e(TAG,"Too many bytes received v1: "+bytesReceived);
                            }
                        }
                    }else{
                        if (bytesReceived<contentLength){
                            packetStillInProgress = true;
                        }else if (bytesReceived==contentLength){
                            packetStillInProgress=false;
                        }else{
                            Log.e(TAG,"Too many bytes received v2: "+bytesReceived);
                        }
                    }
                }else{

                    if (bytesReceived<contentLength){
                        packetStillInProgress = true;
                    }else if (bytesReceived==contentLength){
                        packetStillInProgress=false;
                    }else{
                        trap_a_Toi_Out_Of_Order=routeDecode.toi;
                        Log.e(TAG,"Too many bytes received v3: "+bytesReceived +" latest TOI in efdt header: "+latestToi);
                    }
                }
            }
            if (routeDecode.toi==0 && routeDecode.tsi==0){
                fileName=routeDecode.fileName;
                if (fileName.toLowerCase().contains(".mpd") || fileName.toLowerCase().contains("usbd.xml") || fileName.toLowerCase().contains("s-tsid.xml")) {
                    Log.d(TAG,"Found file: "+fileName);
                    fileManager.create(routeDecode);
                }else{
                    Log.e(TAG, "Unrecognized fileName: "+fileName );
                }
            }else{
                fileManager.write(routeDecode, bytes, RouteDecode.PAYLOAD_START_POSITION, packetSize-RouteDecode.PAYLOAD_START_POSITION);
            }
        }


        public void reportError(){
            stopRequest=true;
            sInstance.handleTaskState(this, TASK_ERROR);
        }


    }

    private class RouteDecode{
        private final int HEADER_LENGTH_POSITION=2;
        private final int TSI_POSITION=8;
        private final int TOI_POSITION=12;
        private final int INSTANCE_ID_POSITION=17;
        private final int HEADER_EXTENSIONS =16;
        private final int ARRAY_POSITION=16;
        private final int EXPIRY_POSITION=24;
        private final int MAX_OBJECT_SIZE_POSITION=28;
        private final int EFDT_CONTENT_START_POSITION=40;
        public final static int PAYLOAD_START_POSITION=20;
        private final byte[] PREAMBLE=new byte[]{(byte) 0x10, (byte) 0xA0};
        private final byte[] EXFT_PREAMBLE=new byte[]{(byte) 0xC0, (byte) 0x10}; // mask last byte with 0xf0

        public boolean valid=false;
        public boolean efdt=false;
        public short tsi;
        public short toi;
        public short instanceId;
        public short maxObjectSize;
        public short arrayPosition;
        public long expiry;
        public String fileName;
        public int contentLength;
        public int efdt_toi;

        public RouteDecode(byte[] data, int packetSize) {
            if (data.length < 0x20) return;
            if (data[0] == PREAMBLE[0] && data[1] == PREAMBLE[1]) {

                toi = (short) ((data[TOI_POSITION] &0xff) <<24 | (data[TOI_POSITION + 1] &0xff)<<16 | (data[TOI_POSITION + 2] &0xff) <<8 | (data[TOI_POSITION + 3]&0xff));
                tsi = (short) ((data[TSI_POSITION] &0xff) <<24 | (data[TSI_POSITION + 1] &0xff)<<16 | (data[TSI_POSITION + 2] &0xff) <<8 | (data[TSI_POSITION + 3]&0xff));
                byte length = data[HEADER_LENGTH_POSITION];
                if (length == 4) {
                    arrayPosition = (short) ((data[ARRAY_POSITION]&0xff) <<24 | (data[ARRAY_POSITION + 1] &0xff)<<16 | (data[ARRAY_POSITION + 2] &0xff) <<8 | (data[ARRAY_POSITION + 3] & 0xff));
                    valid = true;
                } else if (length==9 && toi==0) {
                    if (data[HEADER_EXTENSIONS] == EXFT_PREAMBLE[0] && (data[HEADER_EXTENSIONS+1] & 0xF0) == EXFT_PREAMBLE[1]) {
                        instanceId=(short) ((data[INSTANCE_ID_POSITION] & 0x0F)<<16 | (data[INSTANCE_ID_POSITION + 1] &0xff)<<8 | (data[INSTANCE_ID_POSITION + 2]*0xff));
                        expiry=System.currentTimeMillis()+((data[EXPIRY_POSITION] &0xff) <<24 | (data[EXPIRY_POSITION + 1] &0xff)<<16 | (data[EXPIRY_POSITION + 2] & 0xff) <<8 | (data[EXPIRY_POSITION + 3]&0xff))*1000;
                        maxObjectSize= (short) ((data[MAX_OBJECT_SIZE_POSITION] &0xff) <<24 | (data[MAX_OBJECT_SIZE_POSITION + 1] &0xff) <<16 |(data[MAX_OBJECT_SIZE_POSITION + 2] &0xff) <<8 | (data[MAX_OBJECT_SIZE_POSITION + 3] &0xff));
                        String s=new String (data, EFDT_CONTENT_START_POSITION, packetSize-EFDT_CONTENT_START_POSITION);
                        EFDT_DATA e=(new ATSCXmlParse(s, ATSCXmlParse.EFDT_INSTANCE_TAG)).EFDTParse();
                        fileName=e.location;
                        contentLength=e.contentlength;
                        efdt_toi=e.toi;
                        efdt=true;
                    }else {
                        Log.e(TAG, "Unknown Route preamble: " + data[HEADER_EXTENSIONS] + "  " + (data[HEADER_EXTENSIONS + 1] & 0xF0));
                    }
                }else {
                    Log.e(TAG,"Unknown Route header length: "+length);
                }
            }
        }
    }

    public static class ContentFileLocation{
        public String fileName;
        public int toi;
        public long time;
        public long expiry;
        public int start;
        public int contentLength;
        public int nextWritePosition;
//        public int toi;
        public ContentFileLocation(String fileName, int toi, int start, int length, long time, long expiry){
            this.fileName=fileName;
            this.toi=toi;
            this.time=time;
            this.expiry=expiry;
            this.start=start;
            this.contentLength=length;
            this.nextWritePosition=0;
        }

    }

    public class FileManager{
        HashMap<String, ContentFileLocation> mapContentLocations;
        HashMap<String, ContentFileLocation> mapSignalingLocations;

        HashMap<Integer,String> signaling_TOI_FileName_Map =new HashMap<>();

        int firstAvailablePosition=0;
        int maxAvailablePosition=0;
        byte[] storage;

        ReentrantLock lock = new ReentrantLock();


        public FileManager(int size, int type){
            if (type==SIGNALLING){
                mapSignalingLocations = new HashMap<>();
            }else{
                mapContentLocations=new HashMap<>();
            }


            storage=new byte[size];
            maxAvailablePosition=size-1;
        }

        public int read(String fileName, byte[] output, int offset,  int length){
            lock.lock();
            try {
                    ContentFileLocation f = mapContentLocations.get(fileName);
                    if (f != null) {
                        int bytesToFetch = Math.min(length, f.contentLength - offset);
                        bytesToFetch = (bytesToFetch < 0) ? 0 : bytesToFetch;
                        int startPosition = f.start + offset;
                        System.arraycopy(storage, startPosition, output, 0, bytesToFetch);
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
            int toi=r.toi; int tsi=r.tsi;
            lock.lock();
            try{
                if (signaling_TOI_FileName_Map.containsKey(toi) && mapSignalingLocations.containsKey(signaling_TOI_FileName_Map.get(toi))){
                    ContentFileLocation l=mapSignalingLocations.get(signaling_TOI_FileName_Map.get(toi));
                    if (length<=(l.contentLength - l.nextWritePosition)){
                        System.arraycopy(input, offset, storage, l.start + l.nextWritePosition, length);
                        l.nextWritePosition+=length;
                        if (l.nextWritePosition==l.contentLength){


                            String fileName=l.fileName.substring(0,l.fileName.length()-4);    //Finished so copy object to not ".new"
                            Iterator<Map.Entry<Integer, String>> it=signaling_TOI_FileName_Map.entrySet().iterator();
                            while (it.hasNext()){
                                Map.Entry<Integer, String> entry=it.next();
                                if (entry.getValue().equals(fileName)){
                                    it.remove();
                                }
                            }
                            mapSignalingLocations.remove(l.fileName);
                            mapSignalingLocations.put(fileName, l);
                            signaling_TOI_FileName_Map.remove(toi);
                            signaling_TOI_FileName_Map.put(toi, fileName);

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
        public boolean create(RouteDecode r) {
            lock.lock();
            try {

                firstAvailablePosition = (firstAvailablePosition + r.contentLength) > maxAvailablePosition ? 0 : firstAvailablePosition;
                ContentFileLocation c = new ContentFileLocation(r.fileName.concat(".new"), r.efdt_toi, firstAvailablePosition, r.contentLength, System.currentTimeMillis(), System.currentTimeMillis() + MAX_FILE_RETENTION_MS);
                Iterator<Map.Entry<String,ContentFileLocation>> iterator= mapSignalingLocations.entrySet().iterator();
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

                        signaling_TOI_FileName_Map.remove(it.getValue().toi);
                        iterator.remove();

                    }
                }

                mapSignalingLocations.put(c.fileName, c);
                signaling_TOI_FileName_Map.put(r.efdt_toi,c.fileName);

                firstAvailablePosition += r.contentLength;

                snapshot_of_Filemanager();
                return true;

            } finally {
                lock.unlock();
            }
        }

        public void snapshot_of_Filemanager(){
//
            for (Map.Entry<Integer,String> entry : signaling_TOI_FileName_Map.entrySet() ){
                Log.d(TAG,"Toi mapping to filenames:   size: "+signaling_TOI_FileName_Map.size()+"TOI: "+entry.getKey()+"   FileName:  "+entry.getValue());
            }
            for (Map.Entry<String,ContentFileLocation> entry : mapSignalingLocations.entrySet() ){
                Log.d(TAG,"Filename mapping to ContentLocations:  size:  "+mapSignalingLocations.size()+"FileName: "+entry.getKey() +"   TOI:  "+entry.getValue().toi);
            }

        }
    }


}
