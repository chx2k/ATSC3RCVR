package com.sony.tv.app.atsc3receiver1_0.app;

import android.util.Log;

import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.upstream.UdpDataSource;

/**
 * Created by xhamc on 3/22/17.
 */

public class FluteTaskManager implements Runnable{

    private static final String TAG="FluteTaskManager";
    public String error;
    private int packetSize;
    private boolean running;
    Boolean stopRequest;
    public DataSpec dataSpec;
//    public FluteFileManager fileManager=FluteFileManager.getInstance();
    public FluteFileManager fileManager;

    private UdpDataSource udpDataSource;
    private byte[] bytes;
    private static int MAX_SOCKET_TIMEOUT=20*1000;


    private FluteReceiver sInstance=FluteReceiver.getInstance();

    public FluteTaskManager (DataSpec dataSpec){
        stopRequest=false;
//        this.fileManager=FluteFileManager.getInstance();
        this.dataSpec=dataSpec;
//        sInstance=FluteReceiver.getInstance();
        fileManager=new FluteFileManager(dataSpec);

    }

    @Override
    public void run(){
        Log.d(TAG, "New thread running FluteTaskManager created");
        fileManager.reset();

        udpDataSource = new UdpDataSource(new TransferListener<UdpDataSource>() {
            @Override
            public void onTransferStart(UdpDataSource source, DataSpec dataSpec) {
                running=true;
            }

            @Override
            public void onBytesTransferred(UdpDataSource source, int bytesTransferred) {
                packetSize=bytesTransferred;
            }
            @Override
            public void onTransferEnd(UdpDataSource source) {
                running=false;
            }
            },
            UdpDataSource.DEFAULT_MAX_PACKET_SIZE,
            MAX_SOCKET_TIMEOUT
            );


        try {
            udpDataSource.open(dataSpec);
        } catch (UdpDataSource.UdpDataSourceException e) {
            e.printStackTrace();
            return;
        }
        mainloop:while (!stopRequest && running) {

            int len;
            int offset = 0;
            bytes = new byte[UdpDataSource.DEFAULT_MAX_PACKET_SIZE];
            do {
                try {
                    len = udpDataSource.read(bytes, offset, UdpDataSource.DEFAULT_MAX_PACKET_SIZE);
                    offset += len;
                } catch (UdpDataSource.UdpDataSourceException e) {
                    e.printStackTrace();
//                        reportError();
                    break mainloop;
                }
            } while (offset < packetSize);

            transferDataToFluteHandler(bytes, packetSize);

        }
        udpDataSource.close();
    }

    public void stop(){
        stopRequest=true;
        udpDataSource.close();
        sInstance.handleTaskState(this, FluteReceiver.TASK_STOPPED);
    }

    private void transferDataToFluteHandler(byte[] bytes, int packetSize ){

        sInstance.handleTaskState(this, FluteReceiver.FOUND_FLUTE_PACKET);
        String fileName;
        RouteDecode routeDecode=new RouteDecode(bytes, packetSize);

        if (routeDecode.toi==0 && routeDecode.tsi==0){
            fileName=routeDecode.fileName;
            if (fileName.toLowerCase().contains(".mpd") || fileName.toLowerCase().contains("usbd.xml") || fileName.toLowerCase().contains("s-tsid.xml")) {
                Log.d(TAG,"Found file: "+fileName);

                fileManager.create(routeDecode);
            }else{
                Log.e(TAG, "Unrecognized fileName: "+fileName );
            }
        }else if (routeDecode.toi==0) {
            fileManager.create(routeDecode);
        }
        else {
            fileManager.write(routeDecode, bytes, RouteDecode.PAYLOAD_START_POSITION, packetSize-RouteDecode.PAYLOAD_START_POSITION);
        }

    }


    public void reportError(){
        stopRequest=true;
        sInstance.handleTaskState(this, FluteReceiver.TASK_ERROR);
    }




}
