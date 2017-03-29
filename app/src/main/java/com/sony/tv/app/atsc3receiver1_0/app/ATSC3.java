package com.sony.tv.app.atsc3receiver1_0.app;

import android.app.Application;
import android.content.Context;

import com.google.android.exoplayer2.upstream.DataSource;

import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;

import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;

import com.google.android.exoplayer2.util.Util;


/**
 * Created by xhamc on 3/10/17.
 */

public class ATSC3 extends Application {


    protected String userAgent;
    private String TAG="ATSC3";
    public static int dataSourceIndex;
    @Override
    public void onCreate() {
        super.onCreate();
        userAgent = Util.getUserAgent(this, "ExoPlayerDemo");

    }




    public DataSource.Factory buildDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        FluteReceiver sInstance=FluteReceiver.getInstance();
        sInstance.setListener(bandwidthMeter);
        return new FluteDataSourceFactory();
    }


    public HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultHttpDataSourceFactory(userAgent, bandwidthMeter);
    }



}
