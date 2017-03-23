package com.sony.tv.app.atsc3receiver1_0.app;

import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.UdpDataSource;

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
    FluteReceiver fluteReceiver;
    FluteFileManager fileManager;
    private int thread;
    private int bytesToSkip;
    private int bytesToRead;
    private int bytesSkipped;
    private int bytesRead;


    private byte[] fileCacheRead=new byte[MAX_VIDEO_BUFFERSIZE/10];

    public FluteDataSource(int thread){
        fluteReceiver=FluteReceiver.getInstance();
        fileManager=FluteFileManager.getInstance();
        this.thread=thread;
        Log.d(TAG, "Created new FluteDataSource at thread: "+thread);

    }
    @Override
    public long open(DataSpec dataSpec) throws IOException {

        bytesToRead=0;
        bytesToSkip=0;
        bytesRead=0;

        Log.d("TAG", "ExoPlayer trying to open :"+dataSpec.uri);
        mExoPlayerUri = dataSpec;
        String host = mExoPlayerUri.uri.getHost();
        int port = mExoPlayerUri.uri.getPort();
        if ( fluteReceiver.mFluteTaskManager.dataSpec.uri.getHost().equals(host) && fluteReceiver.mFluteTaskManager.dataSpec.uri.getPort()==port){
            String path=dataSpec.uri.getPath();
            try {
                bytesToSkip=(int) dataSpec.position;
                bytesToRead=fileManager.open(path, bytesToSkip , fileCacheRead, MAX_VIDEO_BUFFERSIZE/10);
                return bytesToRead;
            }catch (IOException e) {
                throw e;
            }
        } else{

            throw new IOException("Attempted to open a url that is not active: ".concat(mExoPlayerUri.toString()));
        }

    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        if (readLength == 0) {
            return 0;
        }
        if (bytesToRead != C.LENGTH_UNSET) {
            long bytesRemaining = bytesToRead - bytesRead;
            if (bytesRemaining == 0) {
                return C.RESULT_END_OF_INPUT;
            }
            readLength = (int) Math.min(readLength, bytesRemaining);
        }
        if (readLength<0){
            Log.d(TAG,"BytesToRead: "+bytesToRead+ "  bytesRead: "+ bytesRead+ " readLength:  "+readLength);
            throw new IOException("Read Length is less than 0");

        }
        System.arraycopy(fileCacheRead,bytesRead,buffer,offset,readLength);
        bytesRead += readLength;
//        if (listener != null) {
//            listener.onBytesTransferred(this, read);
//        }
        return readLength;


    }

    @Override
    public Uri getUri() {
        return mExoPlayerUri.uri;
    }

    @Override
    public void close() throws IOException {

    }

}
