package com.sony.tv.app.atsc3receiver1_0.app;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.upstream.UdpDataSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Created by xhamc on 3/18/17.
 */

public class FluteReceiver  {


    private static final String TAG="FluteReceiver";


    private TransferListener<? super DataSource> listener;


    public static final int SIGNALLING=0;
    public static final int AUDIO_CONTENT=1;
    public static final int VIDEO_CONTENT=2;

    private HashMap<String, Integer> mapFileContainsToTSI = new HashMap<>();                //retrieve the relevant TSI based on the name of the file

    public static FluteTaskManager[] mFluteTaskManager=new FluteTaskManager[2];
//    private FluteFileManager mFileManager;
    private FluteTaskManager mAudioContentFluteTaskManager;
    private FluteTaskManager mVideoContentFluteTaskManager;

    private Handler mHandler;
    public static final int TASK_ERROR=-1;
    public final int TASK_COMPLETE=1;
    public static final int TASK_STARTED=2;
    public static final int TASK_STOPPED=3;
    public static final int FOUND_FLUTE_PACKET=4;
    public final int FOUND_FLUTE_INSTANCE=5;
    public static final int FOUND_FLUTE_FILE=6;

    public long timeOffset=0;

    private static FluteReceiver sInstance=new FluteReceiver();
    public long getTimeOffset(){ return timeOffset;}


    /**
     *
     * @return instance of this singleton FluteReceiver
     */
    public static FluteReceiver getInstance(){
        return sInstance;
    }


    /**
     * FluteReceiver is singleton
     */
    private FluteReceiver() {

        this.listener=null;
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
//        this.mFileManager=FluteFileManager.getInstance();
    }

    public void setListener(TransferListener<? super DataSource> listener ){
        this.listener=listener;
    }


    /**
     * start the task manager
     */
    public void start(DataSpec dataSpec, int index) {


            mFluteTaskManager[index] = new FluteTaskManager(dataSpec);
            new Thread(mFluteTaskManager[index]).start();

    }

    /**
     * stop the task manager
     */
    public void stop(){
        if (null!=mFluteTaskManager[0])
           mFluteTaskManager[0].stop();
        if (null!=mFluteTaskManager[1])
            mFluteTaskManager[1].stop();
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







}
