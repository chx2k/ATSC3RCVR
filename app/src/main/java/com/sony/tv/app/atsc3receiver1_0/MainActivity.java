/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.sony.tv.app.atsc3receiver1_0;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.exoplayer2.upstream.DataSpec;
import com.sony.tv.app.atsc3receiver1_0.app.ATSC3;
import com.sony.tv.app.atsc3receiver1_0.app.ATSC3.*;

import com.sony.tv.app.atsc3receiver1_0.app.ATSCXmlParse;
import com.sony.tv.app.atsc3receiver1_0.app.Ads;
import com.sony.tv.app.atsc3receiver1_0.app.FluteReceiver;
import com.sony.tv.app.atsc3receiver1_0.app.FluteTaskManager;
import com.sony.tv.app.atsc3receiver1_0.app.FluteTaskManagerBase;
import com.sony.tv.app.atsc3receiver1_0.app.LLSData;
import com.sony.tv.app.atsc3receiver1_0.app.LLSReceiver;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


import static java.lang.Thread.sleep;

/*
 * MainActivity class that loads MainFragment
 */
public class MainActivity extends Activity {
    /**
     * Called when the activity is first created.
     */

    SampleChooserFragment sampleChooserFragment;
    LLSReceiver mLLSReceiver;
    FluteReceiver mFluteReceiver;
    private static final String TAG="MainActivity";
    private static boolean fragmentsInitialized=false;
    public long timeOffset=0;
    private static boolean sltComplete=false;
    private static boolean stComplete=false;
    private static boolean firstLLS =true;
    public static boolean ExoPlayerStarted=false;
    public static int exoPlayerDataSourceIndex=0;
    private Ads ads;

    CallBackInterface callBackInterface;

    public static Activity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"Initializing ATSC MainActivity");
        setContentView(R.layout.activity_main);
        ExoPlayerStarted=false;
        mLLSReceiver=LLSReceiver.getInstance();
        mFluteReceiver=FluteReceiver.getInstance();
        mFluteReceiver.stop();
        mLLSReceiver.stop();
        sltComplete=false;
        stComplete=false;
        ExoPlayerStarted=false;
        fragmentsInitialized=false;
        activity=this;

        Intent intent=getIntent();
        if (null!=intent){
            if (null!=intent.getExtras()){
                if (intent.getExtras().containsKey("gzip")){
                    boolean value=Boolean.parseBoolean(intent.getStringExtra("gzip"));
                    ATSC3.GZIP=value;
                }


            }
        }
        Ads ads=new Ads(this);
        try {
            String[] dirlist=activity.getApplicationContext().getAssets().list("ADS");
            for (int i=0; i<dirlist.length; i++) {
                String[] adList = activity.getApplicationContext().getAssets().list("ADS/".concat(dirlist[i]));
                for (int j=0; j<adList.length;j++) {

                    if (adList[j].endsWith(".mpd")) {
                        ads.addAd(Ads.SCHEME_ASSET.concat(":///ADS/").concat(dirlist[i]).concat("/").concat(adList[j]), true);

                    }

                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

//        for (int i=0; i<Ads.getAds(false).size();i++){
//
//            if (Ads.getAds(false).get(i).title.startsWith("Gold")||Ads.getAds(false).get(i).title.startsWith("PS4")){
//                Ads.getAds(false).get(i).enabled=true;
//
//            }else{
//                Ads.getAds(false).get(i).enabled=false;
//            }
//        }

//
//        for (int i=0; i<Ads.getAds(false).size();i++){
//
//            if (Ads.getAds(false).get(i).title.startsWith("Better")){
//                Ads.getAds(false).get(i).enabled=true;
//
//            }else{
//                Ads.getAds(false).get(i).enabled=false;
//            }
//        }

        callBackInterface=new CallBackInterface() {
            @Override
            public void callBackSLTFound() {
                sltComplete=true;
                if (stComplete && firstLLS ) {
                    int type;
                    if (LLSReceiver.getInstance().systemTime.getPtpPrepend()!=0){
                        type=ATSC3.QUALCOMM;
                    }else{
                        type=ATSC3.NAB;
                    }
                    startSignalingFluteSession(type);
                    firstLLS =false;
                    if (!fragmentsInitialized)
                        initFragments();
                }
            }

            @Override
            public void callBackSTFound() {

                if (sltComplete && firstLLS ) {
                    int type;
                    if (LLSReceiver.getInstance().systemTime.getPtpPrepend()!=0){
                        type=ATSC3.QUALCOMM;
                    }else{
                        type=ATSC3.NAB;
                    }
                    startSignalingFluteSession(type);
                    firstLLS  = false;
                    if (!fragmentsInitialized)
                        initFragments();
                }
            }

            @Override
            public void callBackUSBDFound(FluteTaskManagerBase fluteTaskManager) {
                if (fluteTaskManager.isFirst() &&
                        fluteTaskManager.isManifestFound() &&
                        fluteTaskManager.isSTSIDFound()){
                        fluteTaskManager.stop();
                }
            }

            @Override
            public void callBackSTSIDFound(FluteTaskManagerBase fluteTaskManager) {
                if (fluteTaskManager.isFirst() &&
                        fluteTaskManager.isManifestFound() &&
                        fluteTaskManager.isUsbdFound()){
                        fluteTaskManager.stop();
                }
            }

            @Override
            public void callBackManifestFound(FluteTaskManagerBase fluteTaskManager) {
                if (fluteTaskManager.isFirst() &&
                        fluteTaskManager.isUsbdFound() &&
                        fluteTaskManager.isSTSIDFound()){
                        fluteTaskManager.stop();
                }
            }

            @Override
            public void callBackFluteStopped(FluteTaskManagerBase fluteTaskManager){
                if (fluteTaskManager.isFirst() &&
                        fluteTaskManager.isUsbdFound() &&
                        fluteTaskManager.isSTSIDFound() &&
                        fluteTaskManager.isManifestFound()){
                    int type;
                    if (LLSReceiver.getInstance().systemTime.getPtpPrepend()!=0){
                        type=ATSC3.QUALCOMM;
                    }else{
                        type=ATSC3.NAB;
                    }
                    startCompleteFluteSession(type, fluteTaskManager);
                }
            }
        };

        initLLSReceiver();

//        initFragments();
    }


    @Override
    public void onStop(){
        super.onStop();
        if (!ExoPlayerStarted) {
            FluteReceiver.getInstance().stop();
            LLSReceiver.getInstance().stop();
        }
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        FluteReceiver.getInstance().stop();
        LLSReceiver.getInstance().stop();
    }

    @Override
    public void onStart(){
        super.onStart();
        mFluteReceiver.resetTimeStamp();
//        LLSReceiver.getInstance().start(this);
    }
    /**
     * Initialize fragments.
     * <p/>
     * Note that this also can be called when Fragments have automatically been restored by Android.
     * In this case we need to attach and configure existing Fragments instead of making new ones.
     */
    private void initFragments() {
        Log.d(TAG,"Initializing Fragment");
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // video playback fragment
        sampleChooserFragment = (SampleChooserFragment) fragmentManager.findFragmentByTag(SampleChooserFragment.TAG);
        if (sampleChooserFragment == null) {
            // create a new fragment and add it
            sampleChooserFragment = new SampleChooserFragment();
            transaction.add(R.id.videoFrame, sampleChooserFragment, SampleChooserFragment.TAG);
        }
        transaction.show(sampleChooserFragment);
        transaction.commit();

        fragmentsInitialized=true;
    }

    private void initLLSReceiver(){
        startLLSReceiver();


    }
    public void stopLLSReceiver(){
        mLLSReceiver.stop();

    }
    public void startLLSReceiver(){
        if (!mLLSReceiver.running) {
            firstLLS  = true;
            sltComplete = false;
            stComplete = false;
            mLLSReceiver.start(this, callBackInterface);
        }
    }




    public void startSignalingFluteSession(int type){
//            int i=0;

            for (int i=0; i<mLLSReceiver.slt.mSLTData.mServices.size(); i++){

                String host=mLLSReceiver.slt.mSLTData.mServices.get(i).broadcastServices.get(0).slsDestinationIpAddress;
                String uriString="udp://".concat(host).concat(":").concat(
                mLLSReceiver.slt.mSLTData.mServices.get(i).broadcastServices.get(0).slsDestinationUdpPort);
                Log.d(TAG,"Opening: "+uriString);
                Uri uri=Uri.parse(uriString);
                DataSpec d=new DataSpec(uri);
                mFluteReceiver.start(d, null, i, type, callBackInterface);
        }
    }


    public void startCompleteFluteSession(int type, FluteTaskManagerBase fluteTaskManager){

            int i=fluteTaskManager.index();
            String host=mLLSReceiver.slt.mSLTData.mServices.get(i).broadcastServices.get(0).slsDestinationIpAddress;
            String uriString="udp://".concat(host).concat(":").concat(
                    mLLSReceiver.slt.mSLTData.mServices.get(i).broadcastServices.get(0).slsDestinationUdpPort);
            Log.d(TAG,"Opening: "+uriString);
            Uri uri=Uri.parse(uriString);
            DataSpec d=new DataSpec(uri);
            Log.d(TAG,"Opening: "+uriString);
//            mFluteReceiver.start(d, d2, i, type, callBackInterface);
            mFluteReceiver.start(d, d, i, type, callBackInterface);

    }



}
