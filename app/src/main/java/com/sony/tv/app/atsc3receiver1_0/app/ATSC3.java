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
    public static String manifest="ManifestUpdate_Dynamic.mpd";
    public static int NAB=1;
    public static int QUALCOMM=2;

    public interface CallBackInterface{
        void callBackSLTFound();
        void callBackSTFound();
        void callBackUSBDFound(FluteTaskManagerBase fluteTaskManagerBase);
        void callBackSTSIDFound(FluteTaskManagerBase fluteTaskManagerBase );
        void callBackManifestFound(FluteTaskManagerBase fluteTaskManagerBase);
        void callBackFluteStopped(FluteTaskManagerBase fluteTaskManagerBase);
    }


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

    public static boolean channelUp(final Activity activity){
        if (dataSourceIndex==1){
            dataSourceIndex=0;
            resetTimeStamp(dataSourceIndex);
            ATSCSample sample=getSampleFromIndex(dataSourceIndex);
            activity.startActivity(sample.buildIntent(activity));
            return true;
        }
        return false;

    }
    public static boolean channelDown(final Activity activity){

        if (dataSourceIndex==0){
            dataSourceIndex=1;
            resetTimeStamp(dataSourceIndex);
            ATSCSample sample=getSampleFromIndex(dataSourceIndex);
            activity.startActivity(sample.buildIntent(activity));
            return true;
        }
        return false;

    }

    private static ATSCSample getSampleFromIndex(int i){
        String host="239.255.8."+String.format("%d",i+1);
        String url=host;
        String title = LLSReceiver.getInstance().slt.mSLTData.mServices.get(i).shortServiceName;
        String port = LLSReceiver.getInstance().slt.mSLTData.mServices.get(i).broadcastServices.get(0).slsDestinationUdpPort;
        String name = manifest; /* TODO detect automatically from USBD*/
        ATSCSample s = new ATSCSample(title, null, null, null, false, url, port, name);
        return s;

    }

    private static void resetTimeStamp(int index){

        FluteReceiver.mFluteTaskManager[index].fileManager().resetTimeStamp();
    }

}
