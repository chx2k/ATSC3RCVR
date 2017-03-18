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
import java.util.Map;

/**
 * Created by xhamc on 3/18/17.
 */

public class FluteReceiver {

    private static final int MAX_MPD_BUFFERSIZE=5000;
    private static  final int MAX_USBD_BUFFERSIZE=1000;
    private static final int MAX_STSID_BUFFERSIZE=1000;
    private static final int MAX_VIDEOINIT_BUFFERSIZE=1000;
    private static final int MAX_AUDIOINIT_BUFFERSIZE=1000;
    private static final int MAX_VIDEO_BUFFERSIZE=10000000;
    private static final int MAX_AUDIO_BUFFERSIZE=10000000;
    private static final int MAX_FILE_RETENTION_MS=10000;


    public  FileManager mMPD=new FileManager(MAX_MPD_BUFFERSIZE);
    public  FileManager mUSBD=new FileManager(MAX_USBD_BUFFERSIZE);
    public  FileManager mSTSID=new FileManager(MAX_STSID_BUFFERSIZE);
    public  FileManager mVideoInit=new FileManager(MAX_VIDEOINIT_BUFFERSIZE);
    public  FileManager mAudioInit=new FileManager(MAX_AUDIOINIT_BUFFERSIZE);
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
    private static final String TAG="LLS";
    private static FluteReceiver sInstance=new FluteReceiver();

    private FluteTaskManager mSignalingFluteTaskManager;
    private FluteTaskManager mContentFluteTaskManager;

    boolean first=true;

    /**
     * LLSReceiver is singleton
     */
    private FluteReceiver() {

        dataSpec = new DataSpec(Uri.parse("udp://224.0.23.60:4937"));
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
                        break;
                    case FOUND_FLUTE_INSTANCE:
                        break;
                    case FOUND_FLUTE_FILE:
                        break;
//                    case FOUND_SLT:
//                        Log.d(TAG,"FOUND SLT");
//                        long t= System.currentTimeMillis();
//                        slt= new ATSCXmlParse(llstask.mSLTData, "SLT").LLSParse();
//                        Log.d(TAG,"LLS xml parse time in ms: "+(System.currentTimeMillis()-t));
//
//                        saveLLSData(slt);
//                        if (first) {
//                            ((MainActivity) activityContext).callBackSLTFound(true);
//                            first = false;
//                        }
//                        break;
//                    case FOUND_ST:
//                        Log.d(TAG,"FOUND ST");
//                        systemTime= new ATSCXmlParse(llstask.mSTData, SYSTEMTIMETAG).LLSParse();
//                        saveLLSData(systemTime);
//                        break;
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
            mSignalingFluteTaskManager = new FluteTaskManager(dataSpec, mMPD, mUSBD, mSTSID);
            new Thread(mSignalingFluteTaskManager).start();
        } else if (type == FluteDataSource.CONTENT) {
            mContentFluteTaskManager = new FluteTaskManager(dataSpec, mMPD, mUSBD, mSTSID);
            new Thread(mContentFluteTaskManager).start();
        }
    }

    /**
     * stop the task manager
     */
    public void stop(int type){
        if (type == FluteDataSource.SIGNALLING) {
            mSignalingFluteTaskManager.stop();
        }else if (type==FluteDataSource.CONTENT){
            mContentFluteTaskManager.stop();
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

//    public boolean saveFluteData(FluteData data){
//
//        String fileName="/LLS/"+data.type;
//        try {
//            FileOutputStream f=activityContext.openFileOutput(fileName, Activity.MODE_PRIVATE);
//            f.write(data.xmlString.getBytes());
//            f.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//            return false;
//        } finally{
//            return true;
//
//        }
//
//    }

    /**
     * Class to decode the LLS message headers from  bytes receiver
     * from LLSFetchManager, convert to string, and send to UI
     */
    private class FluteTaskManager implements Runnable{

        public String error;
        private int packetSize;
        private boolean running;
        Boolean stopRequest;
        DataSpec dataSpec;
        FileManager mpd;

        FluteTaskManager (DataSpec dataSpec, FileManager mpd, FileManager usbd, FileManager stsid ){
            stopRequest=false;
            this.dataSpec=dataSpec;
        }

        FluteTaskManager (DataSpec dataSpec, FileManager mVideoPacketData, FileManager mAudioPacketData,
                          FileManager mVideoInit, FileManager mAudioInit,
                          Map<Integer, ArrayList<ContentFileLocations>> mapVideoFileLocations,
                          Map<Integer, ArrayList<ContentFileLocations>> mapAudioFileLocations) {
            stopRequest=false;
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
                        reportError();
                        break mainloop;
                    }
                } while (offset < packetSize);

                transferDataToFluteHandler(bytes, packetSize);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    reportError();
                    break;
                }
            }
            udpDataSource.close();
        }

        public void stop(){
            stopRequest=true;
            sInstance.handleTaskState(this, TASK_STOPPED);
        }

        private class FluteObject{
            String fileName;
            int toi;
            int length;
            int overallLength;
            public FluteObject(String fileName,int toi,int length, int overallLength){
                this.fileName=fileName;
                this.toi=toi;
                this.length=length;
                this.overallLength=overallLength;
            }
        }

        private void transferDataToFluteHandler(byte[] bytes, int packetSize ){

            sInstance.handleTaskState(this, FOUND_FLUTE_PACKET);
            String fileName="";
            FluteObject fo=getEFDTInstance(bytes);
            if (fo.toi==0){
                if (fileName.toLowerCase().contains(".mpd")) {
                    mMPD.create(fileName, fo.overallLength);
                }else if (fileName.toLowerCase().equals("usbd.xml")){
                    mUSBD.create(fileName, fo.overallLength);

                }else if (fileName.toLowerCase().equals("s-tsid.xml")){
                    mSTSID.create(fileName,fo.overallLength);
                }
            }
        }

        public FluteObject getEFDTInstance(byte[] bytes){
             return new FluteObject("",0,0,0);

        }

//        public void transferDataToUIThread(int type, byte[] data, int len){
//
//            if (type==SLT ) {
//                sInstance.handleTaskState(this, FOUND_SLT);
//                mSLTData=new String (data,4,len-4);
//            }else if (type==ST){
//                sInstance.handleTaskState(this, FOUND_ST);
//                mSTData=new String (data,4,len-4);
//            }
//        }

        public void reportError(){
            stopRequest=true;
            sInstance.handleTaskState(this, TASK_ERROR);
        }


    }

    public static class ContentFileLocations{
        public long time;
        public long expiry;
        public int start;
        public int length;
        public ContentFileLocations(int start, int length, long time, long expiry){
            this.time=time;
            this.expiry=expiry;
            this.start=start;
            this.length=length;
        }

    }
    public static class FileManager{
        HashMap<String, ContentFileLocations> locations;
        int firstAvailablePosition=0;
        int maxAvailablePosition=0;
        byte[] bytes;
        public FileManager(int size){
            locations = new HashMap<>();
            bytes=new byte[size];
            maxAvailablePosition=size-1;
        }
        public int read(String fileName, byte[] output, int offset,  int length){
            ContentFileLocations f=locations.get(fileName);
            if (f!=null){
                int bytesToFetch=Math.min(length,f.length-offset);
                bytesToFetch=(bytesToFetch<0)?0:bytesToFetch;
                int startPosition = f.start + offset;
                System.arraycopy(bytes, startPosition, output, 0, bytesToFetch);
                return bytesToFetch;
            }
            return 0;
        }
        public boolean write(String fileName, byte[] input, int offset,  int length){
            if (locations.containsKey(fileName)) {
                ContentFileLocations l=locations.get(fileName);
                if (l.length<=offset+length){
                    System.arraycopy(input,length,bytes,l.start+offset,length);
                    return true;
                }
            }
            return false;

        }
        public boolean delete(String fileName) {
            if (locations.containsKey(fileName)) {
                locations.remove(fileName);
                return true;
            }
            return false;
        }
        public boolean create(String fileName, int length){
            if (locations.containsKey(fileName)){
                locations.remove(fileName);
            }
            int startPosition = (firstAvailablePosition+length)>maxAvailablePosition?0:firstAvailablePosition;
            ContentFileLocations c= new ContentFileLocations(startPosition, length, System.currentTimeMillis(), System.currentTimeMillis()+MAX_FILE_RETENTION_MS);
            locations.put(fileName,c);
            firstAvailablePosition+=length;
            for ( Map.Entry<String, ContentFileLocations> key: locations.entrySet()){
                if (locations.get(key).start< firstAvailablePosition){
                    locations.remove(key);
                }
            }
            return true;
        }
    }


}
