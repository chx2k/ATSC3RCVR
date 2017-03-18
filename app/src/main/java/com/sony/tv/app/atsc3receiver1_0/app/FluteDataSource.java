package com.sony.tv.app.atsc3receiver1_0.app;

import android.net.Uri;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.UdpDataSource;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by xhamc on 3/16/17.
 */

public class FluteDataSource implements DataSource{
    public static final int SIGNALLING=0;
    public static final int CONTENT=1;
    public Receiver Receiver;

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        return 0;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        return 0;
    }

    @Override
    public Uri getUri() {
        return null;
    }

    @Override
    public void close() throws IOException {

    }

    public Receiver createReceiver(DataSpec dataSpec, int type, int fileType){
        if (type==SIGNALLING)
            Receiver = new SignalingReceiver(dataSpec, fileType);
        else
            Receiver= new ContentReceiver(dataSpec);
        return Receiver;
    }

    private abstract class Receiver{
        public DataSpec dataSpec;
        public byte[] fluteOutputBuffer;
        private UdpDataSource udpDataSource;
        private static final int USBD=0;
        private static final int STSID=1;
        private static final int MPD=2;
        public int receiverType;

    }

    private class SignalingReceiver extends Receiver{
        public int fileType;
        public SignalingReceiver(DataSpec dataSpec, int type){
            this.dataSpec=dataSpec;
            this.receiverType=SIGNALLING;
            this.fileType=type;
        }

    }
    private class ContentReceiver extends Receiver{
        public ContentReceiver(DataSpec dataSpec){
            this.receiverType=CONTENT;
            this.dataSpec=dataSpec;
        }
    }


}
