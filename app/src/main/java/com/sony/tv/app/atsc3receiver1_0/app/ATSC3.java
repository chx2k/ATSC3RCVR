package com.sony.tv.app.atsc3receiver1_0.app;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.google.android.exoplayer2.upstream.DataSource;

import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;

import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;

import com.google.android.exoplayer2.util.Util;
import com.sony.tv.app.atsc3receiver1_0.PlayerActivity;
import com.sony.tv.app.atsc3receiver1_0.SampleChooserFragment;


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

    public static boolean channelUp(Activity activity){
        if (dataSourceIndex==1){
            ATSCSample sample=getSampleFromIndex(dataSourceIndex);
            activity.startActivity(sample.buildIntent(activity));
            dataSourceIndex=0;

            return true;
        }
        return false;

    }
    public static boolean channelDown(final Activity activity){

        if (dataSourceIndex==0){
            final ATSCSample sample=getSampleFromIndex(dataSourceIndex);
            ((PlayerActivity) activity).releasePlayer();

            new Thread(){
                public void run(){
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    dataSourceIndex=1;

                    activity.startActivity(sample.buildIntent(activity));


                }}.start();
//            activity.startActivity(sample.buildIntent(activity));
            return true;
        }
        return false;

    }

    private static ATSCSample getSampleFromIndex(int i){
        String host="239.255.8."+String.format("%d",i+1);
        String url=host;
        String title = LLSReceiver.getInstance().slt.mSLTData.mServices.get(i).shortServiceName;
        String port = LLSReceiver.getInstance().slt.mSLTData.mServices.get(i).broadcastServices.get(0).slsDestinationUdpPort;
        String name = "ManifestUpdate_Dynamic.mpd"; /* TODO detect automatically from USBD*/
        ATSCSample s = new ATSCSample(title, null, null, null, false, url, port, name);
        return s;
    }

}
