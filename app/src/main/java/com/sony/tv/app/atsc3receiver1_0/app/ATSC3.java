package com.sony.tv.app.atsc3receiver1_0.app;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;


/**
 * Created by xhamc on 3/10/17.
 */

public class ATSC3 extends Application {


    public final static boolean FAKEUDPSOURCE=false;
    public final static boolean FAKEMANIFEST=false;
    public final static boolean FAKEPERIODINJECT=false;
    public static AtomicLong adPrimaryKey;
    public static boolean ADS_ENABLED=true;                   //Enable ad replacements for xlinked periods
    public static boolean GZIP=false;

    public static String userAgent;
    private String TAG="ATSC3";
    public static int dataSourceIndex;
    public static String manifest="ManifestUpdate_Dynamic.mpd";
    public static String periodToInject="";
    public static String manifestContents;
    public static int NAB=1;
    public static int QUALCOMM=2;

    private static Context context;

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
        userAgent = Util.getUserAgent(this, "ATSC3Demo");
        context=this;
        try {
            byte[] buffer=new byte[10000];
            InputStream is=getAssets().open("Period");

            int len=is.read(buffer,0,10000);
            periodToInject=new String(buffer,0,len);

        } catch (IOException e) {
            e.printStackTrace();
        }
        initRealm();
    }

    private void initRealm() {
        Realm.init(this);
        RealmConfiguration configuration  = new RealmConfiguration.Builder()
                .name("atsc3demo")
                .schemaVersion(1)
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(configuration);
        Realm realm = Realm.getInstance(configuration);

        try {
            adPrimaryKey = new AtomicLong(realm.where(AdContent.class).max("id").longValue() + 1);
        } catch (Exception e) {
            realm.beginTransaction();
            AdContent task = realm.createObject(AdContent.class, 0);
            adPrimaryKey = new AtomicLong(realm.where(AdContent.class).max("id").longValue() + 1);
            RealmResults<AdContent> results = realm.where(AdContent.class).equalTo("id", 0).findAll();
            results.deleteAllFromRealm();
            realm.commitTransaction();
        }finally {
            realm.close();
        }


    }

    public static Context getContext(){
        return context;
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
