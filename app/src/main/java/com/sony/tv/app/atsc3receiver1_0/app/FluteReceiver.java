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


    public  FileManager signalingFiles=new FileManager(MAX_SIGNALING_BUFFERSIZE);
    public  FileManager mVideoPacketData=new FileManager(MAX_VIDEO_BUFFERSIZE);
    public  FileManager mAudioPacketData=new FileManager(MAX_AUDIO_BUFFERSIZE);


    public boolean running=false;
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

    private FluteTaskManager mSignalingFluteTaskManager;
    private FluteTaskManager mAudioContentFluteTaskManager;
    private FluteTaskManager mVideoContentFluteTaskManager;


    boolean first=true;

    /**
     * LLSReceiver is singleton
     */
    private FluteReceiver() {

//        dataSpec = new DataSpec(Uri.parse("udp://224.0.23.60:4937"));
        mHandler=new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                FluteTaskManager llstask= (FluteTaskManager) inputMessage.obj;

                switch (inputMessage.what) {
                    // The decoding is done
                    case TASK_COMPLETE:
                        Log.d(TAG,"FOUND LLS MESSAGES");
                        break;
                    case TASK_STARTED:
                        Log.d(TAG,"STARTED");
                        break;
                    case FOUND_FLUTE_PACKET:
                        Log.d(TAG,"FOUND FLUTE PACKET");
                        break;
                    case FOUND_FLUTE_INSTANCE:
                        Log.d(TAG,"FOUND FLUTE INSTANCE");

                        break;
                    case FOUND_FLUTE_FILE:
                        break;
                    case TASK_ERROR:
                        Log.d(TAG,"FOUND ERROR: "+llstask.error);
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

        if (type == FluteDataSource.SIGNALLING) {
            mSignalingFluteTaskManager = new FluteTaskManager(dataSpec, signalingFiles);
            new Thread(mSignalingFluteTaskManager).start();
        } else if (type == FluteDataSource.AUDIO_CONTENT) {
            mAudioContentFluteTaskManager = new FluteTaskManager(dataSpec, mAudioPacketData);
            new Thread(mAudioContentFluteTaskManager).start();
        } else if (type == FluteDataSource.VIDEO_CONTENT) {
            mVideoContentFluteTaskManager = new FluteTaskManager(dataSpec, mVideoPacketData);
            new Thread(mVideoContentFluteTaskManager).start();
        }
    }

    /**
     * stop the task manager
     */
    public void stop(int type){
        if (type == FluteDataSource.SIGNALLING) {
            mSignalingFluteTaskManager.stop();
        }else if (type==FluteDataSource.AUDIO_CONTENT) {
            mAudioContentFluteTaskManager.stop();
        }else if (type==FluteDataSource.VIDEO_CONTENT) {
            mVideoContentFluteTaskManager.stop();
        }
    }

    /**
     * handleTaskState is passes Flute Datagram packets to UI thread
     * @param task  The FluteTaskManager holding the data
     * @param state The state of the task manager (error, completed ...)
     */
    public void handleTaskState(FluteTaskManager task, int state){
        Log.d(TAG,"Message to send: "+state);

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

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
//                    reportError();
                    break;
                }
            }
            udpDataSource.close();
        }

        public void stop(){
            stopRequest=true;
            sInstance.handleTaskState(this, TASK_STOPPED);
        }


        private void transferDataToFluteHandler(byte[] bytes, int packetSize ){

            sInstance.handleTaskState(this, FOUND_FLUTE_PACKET);
            String fileName="";
            RouteDecode routeDecode=new RouteDecode(bytes, packetSize);

            if (routeDecode.toi==0){
                fileName=routeDecode.fileName;
                if (fileName.toLowerCase().contains(".mpd") || fileName.toLowerCase().contains("usbd.xml") || fileName.toLowerCase().contains("s-tsid.xml")) {
                    Log.d(TAG,"Found file: "+fileName);
                    fileManager.create(routeDecode);
                }else{
                    Log.d(TAG, "Unrecognized fileName: "+fileName );
                }
            }else{
                fileManager.write(routeDecode.toi, bytes, RouteDecode.PAYLOAD_START_POSITION, packetSize-RouteDecode.PAYLOAD_START_POSITION);
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

                toi = (short) (data[TOI_POSITION] << 24 | data[TOI_POSITION + 1] << 16 | data[TOI_POSITION + 2] << 8 | data[TOI_POSITION + 3]);
                tsi = (short) (data[TSI_POSITION] << 24 | data[TSI_POSITION + 1] << 16 + data[TSI_POSITION + 2] << 8 | data[TSI_POSITION + 3]);
                byte length = data[HEADER_LENGTH_POSITION];
                if (length == 4) {
                    arrayPosition = (short) (data[ARRAY_POSITION] * 0x1000 + data[ARRAY_POSITION + 1] * 0x100 + data[ARRAY_POSITION + 2] * 0x10 + data[ARRAY_POSITION + 3]);
                    valid = true;
                } else if (length==9 && toi==0) {
                    if (data[HEADER_EXTENSIONS] == EXFT_PREAMBLE[0] && (data[HEADER_EXTENSIONS+1] & 0xF0) == EXFT_PREAMBLE[1]) {
                        instanceId=(short) ( (data[INSTANCE_ID_POSITION] & 0x0F) << 16 + data[INSTANCE_ID_POSITION + 1] << 8 + data[INSTANCE_ID_POSITION + 2]);
                        expiry=System.currentTimeMillis()+(data[EXPIRY_POSITION] << 24 + data[EXPIRY_POSITION + 1] << 16 + data[EXPIRY_POSITION + 2] <<8 + data[EXPIRY_POSITION + 3])*1000;
                        maxObjectSize=(short) (data[MAX_OBJECT_SIZE_POSITION] << 24 + data[MAX_OBJECT_SIZE_POSITION + 1] << 16 + data[MAX_OBJECT_SIZE_POSITION + 2] << 8 + data[MAX_OBJECT_SIZE_POSITION + 3]);
                        String s=new String (data, EFDT_CONTENT_START_POSITION, packetSize-EFDT_CONTENT_START_POSITION);
                        EFDT_DATA e=(new ATSCXmlParse(s, ATSCXmlParse.EFDT_INSTANCE_TAG)).EFDTParse();
                        fileName=e.location;
                        contentLength=e.contentlength;
                        efdt_toi=e.toi;
                        efdt=true;
                    }
                }
            }
        }
    }

    public static class ContentFileLocations{
        public long time;
        public long expiry;
        public int start;
        public int length;
        public int nextWritePosition;
        public ContentFileLocations(int start, int length, long time, long expiry){
            this.time=time;
            this.expiry=expiry;
            this.start=start;
            this.length=length;
            this.nextWritePosition=0;
        }

    }

    public class FileManager{
        HashMap<String, ContentFileLocations> locations;
        HashMap<Integer,String> toiToFileMap =new HashMap<>();

        int firstAvailablePosition=0;
        int maxAvailablePosition=0;
        byte[] bytes;

        ReentrantLock lock = new ReentrantLock();


        public FileManager(int size){
            locations = new HashMap<>();
            bytes=new byte[size];
            maxAvailablePosition=size-1;
        }

        public int read(String fileName, byte[] output, int offset,  int length){
            lock.lock();
            try {
                ContentFileLocations f = locations.get(fileName);
                if (f != null) {
                    int bytesToFetch = Math.min(length, f.length - offset);
                    bytesToFetch = (bytesToFetch < 0) ? 0 : bytesToFetch;
                    int startPosition = f.start + offset;
                    System.arraycopy(bytes, startPosition, output, 0, bytesToFetch);
                    return bytesToFetch;
                }
                return 0;
            }finally{
                lock.unlock();
            }
        }
        public boolean write(int toi, byte[] input, int offset, int length){
            lock.lock();
            try{
                String fileName;
                if (toiToFileMap.containsKey(toi)) {

                    fileName=toiToFileMap.get(toi).concat(".new");
                    if (locations.containsKey(fileName)){
                        ContentFileLocations l = locations.get(fileName);
                        if (l.length - l.nextWritePosition>=length){
                            System.arraycopy(input, offset, bytes, l.start + l.nextWritePosition, length);
                            l.nextWritePosition+=length;
                            if (l.nextWritePosition==l.length){         //Finished so copy object to not ".new"
                                locations.put(toiToFileMap.get(toi) , locations.get(fileName));
                                locations.remove(fileName);

                            }
                        }

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
        public boolean create(RouteDecode r){
            toiToFileMap.put(r.efdt_toi, r.fileName);           /*TODO memory leak will occur eventually, clean up expired */

//            if (locations.containsKey(fileName)){
//                locations.remove(fileName);
//            }
            firstAvailablePosition = (firstAvailablePosition+r.contentLength)>maxAvailablePosition?0:firstAvailablePosition;
            ContentFileLocations c= new ContentFileLocations(firstAvailablePosition, r.contentLength, System.currentTimeMillis(), System.currentTimeMillis()+MAX_FILE_RETENTION_MS);
            firstAvailablePosition+=r.contentLength;
            for (Map.Entry<String, ContentFileLocations> entry : locations.entrySet()) {
                if (entry.getValue().start<firstAvailablePosition){             //overwrite an existing entry so delete it;

                    Log.d (TAG, "Deleted key at buffer position: "+entry.getValue().start+"  for key value: "+entry.getKey());
                    locations.remove(entry.getKey());
                }
            }
            locations.put(r.fileName.concat(".new"),c);

            return true;
        }
    }


}
