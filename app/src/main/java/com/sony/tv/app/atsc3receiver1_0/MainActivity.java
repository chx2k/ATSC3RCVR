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
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.exoplayer2.upstream.DataSpec;
import com.sony.tv.app.atsc3receiver1_0.app.ATSC3;
import com.sony.tv.app.atsc3receiver1_0.app.ATSCXmlParse;
import com.sony.tv.app.atsc3receiver1_0.app.FluteReceiver;
import com.sony.tv.app.atsc3receiver1_0.app.LLSData;
import com.sony.tv.app.atsc3receiver1_0.app.LLSReceiver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    private static boolean first=true;
    public static boolean ExoPlayerStarted=false;

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
            first = true;
            sltComplete = false;
            stComplete = false;
            mLLSReceiver.start(this);
        }
    }

//    public void refreshFragments(){
//        sampleChooserFragment.refreshFragments();
//    }

    public void callBackSLTFound(){
        sltComplete=true;
        if (stComplete && first) {
            startFluteSession(FluteReceiver.SIGNALLING);
            first=false;
            if (!fragmentsInitialized)
                initFragments();
        }
    }

    public void callBackSTFound(long time) {

        Date now=Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();
        stComplete = true;
        long nowms=now.getTime();
        timeOffset = nowms - time/1000;

        if (sltComplete && first) {
            startFluteSession(FluteReceiver.SIGNALLING);
            first = false;
            if (!fragmentsInitialized)
                initFragments();
        }
    }



    public void callBackUSBDFound(String manifest){

    }

    public void callBackSTSIDFound(int audioTSI, int videoTSI){

    }

    public void startFluteSession(int type){
//        if (mLLSReceiver.slt!=null){
//            for (int i=0; i<mLLSReceiver.slt.mSLTData.mServices.size(); i++){
//                Uri uri=Uri.parse((mLLSReceiver.slt.mSLTData.mServices.get(i).broadcastServices.get(0).slsDestinationIpAddress + ":" +
//                        mLLSReceiver.slt.mSLTData.mServices.get(i).broadcastServices.get(0).slsDestinationUdpPort));
//                String port;
//                if (i==0) {port=":4005";}else {port=":4006";}
//                String uriString="udp://"+
//                        (mLLSReceiver.slt.mSLTData.mServices.get(i).broadcastServices.get(0).slsDestinationIpAddress.concat(port));
                // stopLLSReceiver();
//                String uriString="udp://239.255.8.1:4005";
//                String uriString="udp://".concat(mLLSReceiver.slt.mSLTData.mServices.get(0).broadcastServices.get(0).slsDestinationIpAddress).concat(":").concat(
//                        mLLSReceiver.slt.mSLTData.mServices.get(0).broadcastServices.get(0).slsDestinationUdpPort);
                String uriString="udp://".concat(mLLSReceiver.slt.mSLTData.mServices.get(1).broadcastServices.get(0).slsDestinationIpAddress).concat(":").concat(
                mLLSReceiver.slt.mSLTData.mServices.get(1).broadcastServices.get(0).slsDestinationUdpPort);

//                Uri uri=Uri.parse((mLLSReceiver.slt.mSLTData.mServices.get(0).broadcastServices.get(0).slsDestinationIpAddress + ":" +

                Log.d(TAG,"Opening: "+uriString);
                Uri uri=Uri.parse(uriString);
                DataSpec d=new DataSpec(uri);
                mFluteReceiver.start(d, timeOffset);
//                Log.d(TAG, "Started Flute Signalling receiver: "+i);
//            }
//        }
    }
    public void stopFluteSession(int type){

    }

}
