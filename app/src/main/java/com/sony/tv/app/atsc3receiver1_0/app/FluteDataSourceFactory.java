package com.sony.tv.app.atsc3receiver1_0.app;

import android.util.Log;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.FileDataSource;

/**
 * Created by xhamc on 3/21/17.
 */

public class FluteDataSourceFactory implements DataSource.Factory{
    public static int thread=-1;
    @Override
    public DataSource createDataSource() {
        thread++;
        return new FluteDataSource(thread);

    }


}
