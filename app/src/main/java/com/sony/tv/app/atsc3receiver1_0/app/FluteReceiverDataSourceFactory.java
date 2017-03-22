package com.sony.tv.app.atsc3receiver1_0.app;

import com.google.android.exoplayer2.upstream.DataSource;

/**
 * Created by xhamc on 3/21/17.
 */

public class FluteReceiverDataSourceFactory implements DataSource.Factory{

    @Override
    public DataSource createDataSource() {
        return FluteReceiver.getInstance();
    }


}
