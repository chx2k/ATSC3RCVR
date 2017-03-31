package com.sony.tv.app.atsc3receiver1_0;

import android.test.InstrumentationTestCase;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;

/**
 * Created by xhamc on 3/30/17.
 */

public class LLSGZipTest extends InstrumentationTestCase {



    public void testinflateSLTTest() throws IOException, DataFormatException{

        InputStream open = getInstrumentation().getContext().getResources().getAssets().open("SLT.bin");
        byte[] source=new byte[1000];
        byte[] dataout=new byte[5000];

        int len=open.read(source,0,1000);
        byte[] datain=new byte[len+1];      //extra dummy byte for gzip
//        assertEquals((byte) 0x1f, datain[0]);
//        assertEquals((byte) 0x8b, datain[1]);

        GZIPInputStream gzipInputStream;
        if (source.length > 4)
        {
            gzipInputStream = new GZIPInputStream(
                    new ByteArrayInputStream(source, 4,
                            source.length - 4));

            len = gzipInputStream.read(dataout,0,1000);

            assertTrue("stream fits in buffer: "+len,len<5000);

            gzipInputStream.close();

        }

        String mSLTData=new String (dataout,0,dataout.length);
        Log.d("TEST",mSLTData);
        assertTrue(mSLTData,mSLTData.length()==0);

    }

    public void testinflateSTTest() throws IOException, DataFormatException{

        InputStream open = getInstrumentation().getContext().getResources().getAssets().open("ST.bin");
        byte[] source=new byte[1000];
        byte[] dataout=new byte[5000];

        int len=open.read(source,0,1000);
        byte[] datain=new byte[len+1];      //extra dummy byte for gzip
//        assertEquals((byte) 0x1f, datain[0]);
//        assertEquals((byte) 0x8b, datain[1]);

        GZIPInputStream gzipInputStream;
        if (source.length > 4)
        {
            gzipInputStream = new GZIPInputStream(
                    new ByteArrayInputStream(source, 4,
                            source.length - 4));

            len = gzipInputStream.read(dataout,0,1000);

            assertTrue("stream fits in buffer: "+len,len<5000);

            gzipInputStream.close();

        }

        String mSTData=new String (dataout,0,dataout.length);
        assertTrue(mSTData,mSTData.length()==0);

    }
}
