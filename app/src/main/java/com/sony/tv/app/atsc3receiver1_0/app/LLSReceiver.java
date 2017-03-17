package com.sony.tv.app.atsc3receiver1_0.app;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.Loader;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.upstream.UdpDataSource;
import com.sony.tv.app.atsc3receiver1_0.MainActivity;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.R.attr.type;


/**
 * Created by xhamc on 3/13/17.
 */

public class LLSReceiver {

    public LLSData slt;
    public LLSData systemTime;
    private static DataSpec dataSpec;
    private ATSCUdpDataSource udpDataSource;
    private TransferListener<ATSCUdpDataSource> listener;
    private byte[] bytes;
    private Handler mHandler;
    public static final String SLTTAG="SLT";
    public static final String SYSTEMTIMETAG="SystemTime";

    private final static byte SLT=1;
    private final static byte ST=3;
    private static final int TASK_COMPLETE=1;
    private static final int TASK_ERROR=-1;

    private static final int TASK_STARTED=2;
    private static final int TASK_STOPPED=3;
    private static final int FOUND_SLT=4;
    private static final int FOUND_ST=5;



    private static final String TAG="LLS";

    private static LLSReceiver sInstance=new LLSReceiver();
    private static Activity activityContext;

    private LLSTaskManager mLLSTaskManager;

    boolean first=true;

    /**
     * LLSReceiver is singleton
     */
    private LLSReceiver() {

        dataSpec = new DataSpec(Uri.parse("udp://224.0.23.60:4937"));
        udpDataSource = new ATSCUdpDataSource();
        mHandler=new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                LLSTaskManager llstask= (LLSTaskManager) inputMessage.obj;

                switch (inputMessage.what) {
                    // The decoding is done
                    case TASK_COMPLETE:
                        Log.d(TAG,"FOUND LLS MESSAGES");

                        break;
                    case TASK_STARTED:
                        Log.d(TAG,"STARTED");
                        break;
                    case FOUND_SLT:
                        Log.d(TAG,"FOUND SLT");
                        if (slt!=null) first=false;
                        slt= new ATSCXmlParse(llstask.mSLTData, "SLT").LLSParse();
                        saveLLSData(slt);
                        if (first) ((MainActivity) activityContext).callBackSLTFound(true);
                        break;
                    case FOUND_ST:
                        Log.d(TAG,"FOUND ST");
//                        long t= System.currentTimeMillis();
                        systemTime= new ATSCXmlParse(llstask.mSTData, SYSTEMTIMETAG).LLSParse();
                        saveLLSData(systemTime);
//                        Log.d(TAG,"LLS xml parse time in ms: "+(System.currentTimeMillis()-t));
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
         mLLSTaskManager=new LLSTaskManager();

    }

    /**
     * getInstance()
     * @return reference to singleton instance
     */
    public static LLSReceiver getInstance(){
        return sInstance;
    }

    /**
     * start the task manager
     */
    public void start(Activity m){
        this.activityContext=m;
        first=true;
        if ( m!=null)  mLLSTaskManager.start();
    }

    /**
     * stop the task manager
     */
    public void stop(){
        mLLSTaskManager.stop();
    }

    /**
     * handleTaskState is passes LLS data to UI thread
     * @param task  The LLSTaskManager holding the data
     * @param state The state of the task manager (error, completed ...)
     */
    public void handleTaskState(LLSTaskManager task, int state){
        Log.d(TAG,"Message to send: "+state);

        (mHandler.obtainMessage(state, task)).sendToTarget();
    }

    public boolean saveLLSData(LLSData data){

        String fileName="/LLS/"+data.type;
        try {
            FileOutputStream f=activityContext.openFileOutput(fileName, Activity.MODE_PRIVATE);
            f.write(data.xmlString.getBytes());
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally{
            return true;

        }

    }

    /**
     * Class to decode the LLS message headers from  bytes receiver
     * from LLSFetchManager, convert to string, and send to UI
     */
    private class LLSTaskManager {

        public String mSLTData;
        public String mSTData;
        public String error;
        Thread SLTThread;
        Runnable SLTRunnable;
        Runnable STRunnable;
        Boolean stopRequest=false;

        LLSTaskManager (){

//            STThread=new Thread (new LLSFetch(this,ST));

         }
        public void stop(){
            stopRequest=true;
            sInstance.handleTaskState(this, TASK_STOPPED);
        }

        public void start(){
           /*TODO need to correctly scan all the LLS messages based on the group id and group count*/

            stopRequest=false;
            STRunnable=new LLSFetchManager(this,ST);
            SLTRunnable=new LLSFetchManager(this,SLT);
            SLTThread= new Thread (SLTRunnable);
            SLTThread.start();
        }

        public void fetchTaskData(int type, byte[] data, int len){

            if (type==SLT ) {

                sInstance.handleTaskState(this, FOUND_SLT);
                mSLTData=new String (data,4,len-4);
                STRunnable.run();
            }else if (type==ST){
                sInstance.handleTaskState(this, FOUND_ST);
                mSTData=new String (data,4,len-4);
                SLTRunnable.run();
            }


        }
        public void reportError(String error){
            this.error=error;
            sInstance.handleTaskState(this, TASK_ERROR);
        }


    }

    /**
     * Class to fetch byte data from udp LLS messages and send to Task Manager
     */
    private class LLSFetchManager implements Runnable{
        private LLSTaskManager mTask;
        private byte mType;
        private byte[] bytes;

        public LLSFetchManager(LLSTaskManager task, byte type){
            mTask=task;
            mType=type;
        }
        @Override
        public void run() {
            if (!mTask.stopRequest) {
                int tries = 0;
                int len;
                do {
                    bytes = new byte[ATSCUdpDataSource.DEFAULT_MAX_PACKET_SIZE];

                    try {
                        udpDataSource.open(dataSpec);
                    } catch (UdpDataSource.UdpDataSourceException e) {
                        e.printStackTrace();
                        return;
                    } finally {
                        try {
                            len = udpDataSource.read(bytes, 0, ATSCUdpDataSource.DEFAULT_MAX_PACKET_SIZE);
                            udpDataSource.close();
                        } catch (UdpDataSource.UdpDataSourceException e) {
                            e.printStackTrace();
                            return;
                        }
                    }

                } while (mType != bytes[0] && tries < 100);         //bytes[0] should contain the type of LLS message. Wait till we see relevant one.
                /*TODO better efficency to receive any type of message*/
                if (tries < 100) {
                    mTask.fetchTaskData(bytes[0], bytes, len);
                }else {
                    mTask.reportError("Too many tries");
                }
            }
        }
    }
}
