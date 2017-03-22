package com.sony.tv.app.atsc3receiver1_0.app;

import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.UdpDataSource;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by xhamc on 3/16/17.
 */

public class FluteDataSource implements DataSource {


//    public Receiver Receiver;
    private static final String TAG="FluteDataSource";
    private DataSpec mExoPlayerUri;
    private String mExoPlayerOpenPath="";
    FluteReceiver fluteReceiver;
    FluteFileManager fileManager;
    private int thread;

    public FluteDataSource(int thread){
        fluteReceiver=FluteReceiver.getInstance();
        fileManager=FluteFileManager.getInstance();
        this.thread=thread;
        Log.d(TAG, "Created new FluteDataSource at thread: "+thread);

    }
    @Override
    public long open(DataSpec dataSpec) throws IOException {
        mExoPlayerOpenPath="";
        int mExoPlayerOpenPathContentLength=0;

        Log.d("TAG", "ExoPlayer trying to open :"+dataSpec.uri);
        mExoPlayerUri = dataSpec;
        String host = mExoPlayerUri.uri.getHost();
        int port = mExoPlayerUri.uri.getPort();
        if ( fluteReceiver.mFluteTaskManager.dataSpec.uri.getHost().equals(host) && fluteReceiver.mFluteTaskManager.dataSpec.uri.getPort()==port){
            String path=dataSpec.uri.getPath();
            try {
                mExoPlayerOpenPathContentLength=fileManager.open(path,thread);
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
            return fileManager.read(buffer, offset, readLength, thread);

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


}
