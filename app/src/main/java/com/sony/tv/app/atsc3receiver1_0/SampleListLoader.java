package com.sony.tv.app.atsc3receiver1_0;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSourceInputStream;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.Loader;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Created by xhamc on 3/10/17.
 */
public final class SampleListLoader  {

    private List<SampleGroup> sampleGroups;
    private boolean sawError;
    private String TAG="SampleListLoader";

    public SampleListLoader(Context context, String[] uris){

        sawError=false;
        List<SampleGroup> result = new ArrayList<>();
        String userAgent = Util.getUserAgent(context, "ExoPlayerDemo");
        DataSource dataSource = new DefaultDataSource(context, null, userAgent, false);
        for (String uri : uris) {
            DataSpec dataSpec = new DataSpec(Uri.parse(uri));
            InputStream inputStream = new DataSourceInputStream(dataSource, dataSpec);
            try {
                readSampleGroups(new JsonReader(new InputStreamReader(inputStream, "UTF-8")), result);
            } catch (Exception e) {
                Log.e(TAG, "Error loading sample list: " + uri, e);
                sawError = true;
            } finally {
                Util.closeQuietly(dataSource);
            }
        }
        sampleGroups= result;

    }


    public List<SampleGroup> getSampleGroup(){
        return sampleGroups;

    }
    public boolean getError(){
        return sawError;
    }

    private void readSampleGroups(JsonReader reader, List<SampleGroup> groups) throws IOException {
        reader.beginArray();
        while (reader.hasNext()) {
            readSampleGroup(reader, groups);
        }
        reader.endArray();
    }

    private void readSampleGroup(JsonReader reader, List<SampleGroup> groups) throws IOException {
        String groupName = "";
        ArrayList<Sample> samples = new ArrayList<>();

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case "name":
                    groupName = reader.nextString();
                    break;
                case "samples":
                    reader.beginArray();
                    while (reader.hasNext()) {
                        samples.add(readEntry(reader, false));
                    }
                    reader.endArray();
                    break;
                case "_comment":
                    reader.nextString(); // Ignore.
                    break;
                default:
                    throw new ParserException("Unsupported name: " + name);
            }
        }
        reader.endObject();

        SampleGroup group = getGroup(groupName, groups);
        group.samples.addAll(samples);
    }

    private Sample readEntry(JsonReader reader, boolean insidePlaylist) throws IOException {
        String sampleName = null;
        String uri = null;
        String extension = null;
        UUID drmUuid = null;
        String drmLicenseUrl = null;
        String[] drmKeyRequestProperties = null;
        boolean preferExtensionDecoders = false;
        ArrayList<UriSample> playlistSamples = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case "name":
                    sampleName = reader.nextString();
                    break;
                case "uri":
                    uri = reader.nextString();
                    break;
                case "extension":
                    extension = reader.nextString();
                    break;
                case "drm_scheme":
                    Assertions.checkState(!insidePlaylist, "Invalid attribute on nested item: drm_scheme");
                    drmUuid = getDrmUuid(reader.nextString());
                    break;
                case "drm_license_url":
                    Assertions.checkState(!insidePlaylist,
                            "Invalid attribute on nested item: drm_license_url");
                    drmLicenseUrl = reader.nextString();
                    break;
                case "drm_key_request_properties":
                    Assertions.checkState(!insidePlaylist,
                            "Invalid attribute on nested item: drm_key_request_properties");
                    ArrayList<String> drmKeyRequestPropertiesList = new ArrayList<>();
                    reader.beginObject();
                    while (reader.hasNext()) {
                        drmKeyRequestPropertiesList.add(reader.nextName());
                        drmKeyRequestPropertiesList.add(reader.nextString());
                    }
                    reader.endObject();
                    drmKeyRequestProperties = drmKeyRequestPropertiesList.toArray(new String[0]);
                    break;
                case "prefer_extension_decoders":
                    Assertions.checkState(!insidePlaylist,
                            "Invalid attribute on nested item: prefer_extension_decoders");
                    preferExtensionDecoders = reader.nextBoolean();
                    break;
                case "playlist":
                    Assertions.checkState(!insidePlaylist, "Invalid nesting of playlists");
                    playlistSamples = new ArrayList<>();
                    reader.beginArray();
                    while (reader.hasNext()) {
                        playlistSamples.add((UriSample) readEntry(reader, true));
                    }
                    reader.endArray();
                    break;
                default:
                    throw new ParserException("Unsupported attribute name: " + name);
            }
        }
        reader.endObject();

        if (playlistSamples != null) {
            UriSample[] playlistSamplesArray = playlistSamples.toArray(
                    new UriSample[playlistSamples.size()]);
            return new PlaylistSample(sampleName, drmUuid, drmLicenseUrl, drmKeyRequestProperties,
                    preferExtensionDecoders, playlistSamplesArray);
        } else {
            return new UriSample(sampleName, drmUuid, drmLicenseUrl, drmKeyRequestProperties,
                    preferExtensionDecoders, uri, extension);
        }
    }

    private SampleGroup getGroup(String groupName, List<SampleGroup> groups) {
        for (int i = 0; i < groups.size(); i++) {
            if (Util.areEqual(groupName, groups.get(i).title)) {
                return groups.get(i);
            }
        }
        SampleGroup group = new SampleGroup(groupName);
        groups.add(group);
        return group;
    }

    private UUID getDrmUuid(String typeString) throws ParserException {
        switch (typeString.toLowerCase()) {
            case "widevine":
                return C.WIDEVINE_UUID;
            case "playready":
                return C.PLAYREADY_UUID;
            default:
                try {
                    return UUID.fromString(typeString);
                } catch (RuntimeException e) {
                    throw new ParserException("Unsupported drm type: " + typeString);
                }
        }
    }


    public static class SampleGroup {

        public final String title;
        public final List<Sample> samples;

        public SampleGroup(String title) {
            this.title = title;
            this.samples = new ArrayList<>();
        }

    }

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

    public final class UriSample extends Sample {

        public final String uri;
        public final String extension;

        public UriSample(String name, UUID drmSchemeUuid, String drmLicenseUrl,
                         String[] drmKeyRequestProperties, boolean preferExtensionDecoders, String uri,
                         String extension) {
            super(name, drmSchemeUuid, drmLicenseUrl, drmKeyRequestProperties, preferExtensionDecoders);
            this.uri = uri;
            this.extension = extension;
        }

        @Override
        public Intent buildIntent(Context context) {
            return super.buildIntent(context)
                    .setData(Uri.parse(uri))
                    .putExtra(PlayerActivity.EXTENSION_EXTRA, extension)
                    .setAction(PlayerActivity.ACTION_VIEW);
        }

    }

    public class PlaylistSample extends Sample {

        public final UriSample[] children;

        public PlaylistSample(String name, UUID drmSchemeUuid, String drmLicenseUrl,
                              String[] drmKeyRequestProperties, boolean preferExtensionDecoders,
                              UriSample... children) {
            super(name, drmSchemeUuid, drmLicenseUrl, drmKeyRequestProperties, preferExtensionDecoders);
            this.children = children;
        }

        @Override
        public Intent buildIntent(Context context) {
            String[] uris = new String[children.length];
            String[] extensions = new String[children.length];
            for (int i = 0; i < children.length; i++) {
                uris[i] = children[i].uri;
                extensions[i] = children[i].extension;
            }
            return super.buildIntent(context)
                    .putExtra(PlayerActivity.URI_LIST_EXTRA, uris)
                    .putExtra(PlayerActivity.EXTENSION_LIST_EXTRA, extensions)
                    .setAction(PlayerActivity.ACTION_VIEW_LIST);
        }


    }


}