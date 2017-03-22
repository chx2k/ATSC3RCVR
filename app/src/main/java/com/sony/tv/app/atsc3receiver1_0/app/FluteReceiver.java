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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Created by xhamc on 3/18/17.
 */

public class FluteReceiver implements DataSource {


    private TransferListener<? super DataSource> listener;


    public static final int SIGNALLING=0;
    public static final int AUDIO_CONTENT=1;
    public static final int VIDEO_CONTENT=2;

    private HashMap<String, Integer> mapFileContainsToTSI = new HashMap<>();                //retrieve the relevant TSI based on the name of the file

    private FluteTaskManager mFluteTaskManager;
    private FileManager mFileManager;
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
        this.mFileManager=FileManager.getInstance();
    }

    public void setListener(TransferListener<? super DataSource> listener ){
        this.listener=listener;
    }

    private DataSpec mExoPlayerUri;
    private String mExoPlayerOpenPath="";
    @Override
    public long open(DataSpec dataSpec) throws IOException {
        mExoPlayerOpenPath="";
        int mExoPlayerOpenPathContentLength=0;

        Log.d("TAG", "ExoPlayer trying to open :"+dataSpec.uri);
        mExoPlayerUri = dataSpec;
        String host = mExoPlayerUri.uri.getHost();
        int port = mExoPlayerUri.uri.getPort();
        if (mFluteTaskManager.dataSpec.uri.getHost().equals(host) && mFluteTaskManager.dataSpec.uri.getPort()==port){
            String path=dataSpec.uri.getPath();
            try {
                mExoPlayerOpenPathContentLength=mFileManager.open(path);
                mExoPlayerOpenPath=path;
                return mExoPlayerOpenPathContentLength;
            }catch (IOException e) {
                throw e;
            }
        } else{

            throw new IOException("Attempted to open a url that is not active: ".concat(mExoPlayerUri.toString()));
        }

    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        try {
            return mFileManager.read(buffer, offset, readLength);

        } catch (IOException e){
            throw e;
        }

    }

    @Override
    public Uri getUri() {
        return mExoPlayerUri.uri;
    }

    @Override
    public void close() throws IOException {
        mExoPlayerOpenPath="";

    }


    /**
     * start the task manager
     */
    public void start(DataSpec dataSpec) {

            mFluteTaskManager = new FluteTaskManager(dataSpec);
            new Thread(mFluteTaskManager).start();
    }

    /**
     * stop the task manager
     */
    public void stop(int type){
           mFluteTaskManager.stop();

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
        public DataSpec dataSpec;
        public FileManager fileManager;

        FluteTaskManager (DataSpec dataSpec ){
            stopRequest=false;
            this.fileManager=FileManager.getInstance();
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

        private void transferDataToFluteHandler(byte[] bytes, int packetSize ){

            sInstance.handleTaskState(this, FOUND_FLUTE_PACKET);
            String fileName;
            RouteDecode routeDecode=new RouteDecode(bytes, packetSize);

            if (routeDecode.toi==0 && routeDecode.tsi==0){
                fileName=routeDecode.fileName;
                if (fileName.toLowerCase().contains(".mpd") || fileName.toLowerCase().contains("usbd.xml") || fileName.toLowerCase().contains("s-tsid.xml")) {
                    Log.d(TAG,"Found file: "+fileName);

                    fileManager.create(routeDecode);
                }else{
                    Log.e(TAG, "Unrecognized fileName: "+fileName );
                }
            }else if (routeDecode.toi==0) {
                fileManager.create(routeDecode);
            }
            else {
                    fileManager.write(routeDecode, bytes, RouteDecode.PAYLOAD_START_POSITION, packetSize-RouteDecode.PAYLOAD_START_POSITION);
            }

        }


        public void reportError(){
            stopRequest=true;
            sInstance.handleTaskState(this, TASK_ERROR);
        }


    }







}
