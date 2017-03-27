package com.sony.tv.app.atsc3receiver1_0.app;

import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.UdpDataSource;
import com.sony.tv.app.atsc3receiver1_0.BuildConfig;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;

import static com.sony.tv.app.atsc3receiver1_0.app.FluteFileManager.MAX_AUDIO_BUFFERSIZE;
import static com.sony.tv.app.atsc3receiver1_0.app.FluteFileManager.MAX_VIDEO_BUFFERSIZE;

/**
 * Created by xhamc on 3/16/17.
 */

public class FluteDataSource implements DataSource {


//    public Receiver Receiver;
    private static final String TAG="FluteDataSource";
    private DataSpec mExoPlayerUri;
    private FluteReceiver fluteReceiver;
    private FluteFileManager fileManager;
    private int thread;

    public FluteDataSource(int thread){
        fluteReceiver=FluteReceiver.getInstance();
        fileManager=FluteFileManager.getInstance();
        this.thread=thread;
        Log.d(TAG, "Created new FluteDataSource at thread: "+thread);

    }
    @Override
    public long open(DataSpec dataSpec) throws IOException {
        mExoPlayerUri=dataSpec;

        return fileManager.open(dataSpec, thread);

    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        return fileManager.read(buffer, offset, readLength, thread );

    }

    @Override
    public Uri getUri() {
        return mExoPlayerUri.uri;
    }

    @Override
    public void close() throws IOException {

    }

}
