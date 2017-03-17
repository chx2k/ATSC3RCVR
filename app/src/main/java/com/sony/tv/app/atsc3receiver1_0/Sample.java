package com.sony.tv.app.atsc3receiver1_0;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.UUID;

/**
 * Created by xhamc on 3/16/17.
 */


public abstract class Sample {

    public final String name;
    public final boolean preferExtensionDecoders;
    public final UUID drmSchemeUuid;
    public final String drmLicenseUrl;
    public final String[] drmKeyRequestProperties;

    public Sample(String name, UUID drmSchemeUuid, String drmLicenseUrl,
                  String[] drmKeyRequestProperties, boolean preferExtensionDecoders) {
        this.name = name;
        this.drmSchemeUuid = drmSchemeUuid;
        this.drmLicenseUrl = drmLicenseUrl;
        this.drmKeyRequestProperties = drmKeyRequestProperties;
        this.preferExtensionDecoders = preferExtensionDecoders;
    }

    public Intent buildIntent(Context context) {
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putExtra(PlayerActivity.PREFER_EXTENSION_DECODERS, preferExtensionDecoders);
        if (drmSchemeUuid != null) {
            intent.putExtra(PlayerActivity.DRM_SCHEME_UUID_EXTRA, drmSchemeUuid.toString());
            intent.putExtra(PlayerActivity.DRM_LICENSE_URL, drmLicenseUrl);
            intent.putExtra(PlayerActivity.DRM_KEY_REQUEST_PROPERTIES, drmKeyRequestProperties);
        }
        return intent;
    }

}


