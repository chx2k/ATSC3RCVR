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

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.ZipException;

import static android.R.attr.type;


/**
 * Created by xhamc on 3/13/17.
 */

public class LLSReceiver {

    public LLSData slt;
    public LLSData systemTime;
    public boolean running=false;
    private static DataSpec dataSpec;
    private UdpDataSource udpDataSource;
    private byte[] bytes;
    private Handler mHandler;


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

    boolean firstSLT=true;
    boolean firstST=true;

    /**
     * LLSReceiver is singleton
     */
    private LLSReceiver() {

        dataSpec = new DataSpec(Uri.parse("udp://224.0.23.60:4937"));
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
                        slt= new ATSCXmlParse(llstask.mSLTData, "SLT").LLSParse();

                        saveLLSData(slt);
                        ((MainActivity) activityContext).callBackSLTFound();

                        break;
                    case FOUND_ST:
                        Log.d(TAG,"FOUND ST");
                        systemTime= new ATSCXmlParse(llstask.mSTData, ATSCXmlParse.SYSTEMTIMETAG).LLSParse();
                        saveLLSData(systemTime);
                        ((MainActivity) activityContext).callBackSTFound(systemTime.getPtpPrepend());

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
        mLLSTaskManager=new LLSTaskManager();
        firstSLT=true;
        firstST=true;
        if ( m!=null) {
            new Thread(mLLSTaskManager).start();
            running=true;
        }
    }

    /**
     * stop the task manager
     */
    public void stop(){
        if (null!=mLLSTaskManager)
            mLLSTaskManager.stop();
        running=false;
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
    private class LLSTaskManager implements Runnable{

        public String mSLTData;
        public String mSTData;
        public String error;
        private int packetSize;
        private boolean running;
        Boolean stopRequest;
        private byte[] buffer=new byte[10000];


        LLSTaskManager (){
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
                        len = udpDataSource.read(bytes, offset, 500);
                        offset += len;
                    } catch (UdpDataSource.UdpDataSourceException e) {
                        e.printStackTrace();
                        reportError();
                        break mainloop;
                    }
                } while (offset < packetSize);

                transferDataToUIThread(bytes[0], bytes, packetSize);

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
            udpDataSource.close();
            sInstance.handleTaskState(this, TASK_STOPPED);
        }

        public void transferDataToUIThread(int type, byte[] data, int len){

            if (type==SLT ) {
                try{
                GZIPInputStream gzipInputStream;
                    gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(data, 4, data.length - 4));
                    int unziplen = gzipInputStream.read(buffer,0,buffer.length);
                    gzipInputStream.close();
                    mSLTData=new String (buffer,0,unziplen);
                    sInstance.handleTaskState(this, FOUND_SLT);

                }
                catch(ZipException e){
                    mSLTData=new String (data,4,len-4);
                    sInstance.handleTaskState(this, FOUND_SLT);

                }
                catch(IOException e2){
                    e2.printStackTrace();
                }

            }else if (type==ST){
                try{
                    GZIPInputStream gzipInputStream;
                    gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(data, 4, data.length - 4));
                    int unziplen = gzipInputStream.read(buffer,0,buffer.length);
                    gzipInputStream.close();
                    mSTData=new String (buffer,0,unziplen);
                    sInstance.handleTaskState(this, FOUND_ST);

                }
                catch(ZipException e){
                    mSTData=new String (data,4,len-4);
                    sInstance.handleTaskState(this, FOUND_ST);

                }
                catch(IOException e2){
                    e2.printStackTrace();
                }

            }
        }

        public void reportError(){
            stopRequest=true;
            sInstance.handleTaskState(this, TASK_ERROR);
        }


    }

}
