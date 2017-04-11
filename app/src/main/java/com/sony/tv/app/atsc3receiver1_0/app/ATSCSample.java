package com.sony.tv.app.atsc3receiver1_0.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.sony.tv.app.atsc3receiver1_0.PlayerActivity;
import com.sony.tv.app.atsc3receiver1_0.Sample;

import java.util.UUID;

/**
 * Created by xhamc on 3/29/17.
 */

public final class ATSCSample extends Sample {

    public final String uri;
    //        public final String extension;

    public ATSCSample(String name, UUID drmSchemeUuid, String drmLicenseUrl,
                      String[] drmKeyRequestProperties, boolean preferExtensionDecoders, String uri, String port, String fileName) {
        super(name, drmSchemeUuid, drmLicenseUrl, drmKeyRequestProperties, preferExtensionDecoders);
        this.uri = "flute://"+uri+":"+port+"/"+fileName;
        //            this.extension = extension;
    }

    @Override
    public Intent buildIntent(Context context) {
        return super.buildIntent(context)
                .setData(Uri.parse(uri))
                //                    .putExtra(PlayerActivity.EXTENSION_EXTRA, extension)
                .setAction(PlayerActivity.ACTION_VIEW);
    }

}