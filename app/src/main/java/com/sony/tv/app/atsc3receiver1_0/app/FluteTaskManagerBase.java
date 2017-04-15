package com.sony.tv.app.atsc3receiver1_0.app;

/**
 * Interface to support Qualcomm and NAB flavors of FluteTaskManager
 * Created by xhamc on 4/2/17.
 */

public interface FluteTaskManagerBase{

    FluteFileManagerBase fileManager();
    void stop();
    boolean isManifestFound();
    boolean isUsbdFound();
    boolean isSTSIDFound();
    boolean isFirst();
    int index();

}
