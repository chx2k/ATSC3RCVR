package com.sony.tv.app.atsc3receiver1_0.app;

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by xhamc on 3/14/17.
 * This class parses the LLS message body into SLT object and ST Object
 * Uses Reflection (invoke) to match the xml attribute to the relevant class method
 */

public class ATSCXmlParse {
    private XmlPullParserFactory factory;
    private XmlPullParser xpp;
    private static final String TAG="XML";
    LLSData llsData;
    String type;
    String data;

    public  ATSCXmlParse(final String data, String type){
        llsData =new LLSData(type);
        this.type=type;
        this.data=data;

    }
    public void LLSParse(){
        int tagLevel=-1;
        int[] xmlItems=new int[10];
        boolean foundStartTag =false;

        try {
            factory = XmlPullParserFactory.newInstance();
            xpp = factory.newPullParser();
            StringReader s=new StringReader(data);
            xpp.setInput(s);
            int eventType = xpp.getEventType();
            while (eventType!=XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_DOCUMENT) {
//                    Log.d(TAG,"Start document");
                } else if(eventType == XmlPullParser.START_TAG) {
                    tagLevel++;
                    xmlItems[tagLevel]++;

                    if (type.equals(xpp.getName())){
                        foundStartTag=true;
                        for (int i=0; i<xpp.getAttributeCount(); i++){
                            Log.d(TAG,"attribute: "+xpp.getAttributeName(i)+"="+xpp.getAttributeValue(i));
                            try {

                                llsData.hashMap.get(xpp.getAttributeName(i)).invoke(llsData, xpp.getAttributeValue(i));
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            } catch (InvocationTargetException e) {
                                e.printStackTrace();
                            }
                            if (xpp.getAttributeName(i).equals("ptpPrepend")) {
                                long senderTime = Long.parseLong(xpp.getAttributeValue(i)) / 1000;
                                long receiverTime = System.currentTimeMillis();
                                Log.d(TAG, "sTime: " + senderTime + "rTime: " + receiverTime + "diffTime: " + (senderTime - receiverTime));
                                long diffTime=(senderTime-receiverTime);
                                llsData.hashMap.get("diffTime").invoke(llsData, diffTime);
                            }

                        }
                    }
                    if (foundStartTag) {
                        try {
                            if (tagLevel == 1) {
                                for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                    Log.d(TAG,"attribute: "+xpp.getAttributeName(i)+"="+xpp.getAttributeValue(i));

                                    llsData.hashMap.get(xpp.getAttributeName(i)).invoke(llsData, xmlItems[1], xpp.getAttributeValue(i));
                                }
                            } else if (tagLevel == 2) { //*****currently allowing for XML two levels only
                                for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                    Log.d(TAG,"attribute: "+xpp.getAttributeName(i)+"="+xpp.getAttributeValue(i));

                                    llsData.hashMap.get(xpp.getAttributeName(i)).invoke(llsData, xmlItems[1], xmlItems[2], xpp.getAttributeValue(i));
                                }
                            }
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                } else if(eventType == XmlPullParser.END_TAG) {
//                    Log.d(TAG,"End tag "+xpp.getName());
                    xmlItems[tagLevel]=0;
                    tagLevel--;
//                    Log.d(TAG,"Tag level "+tagLevel);


                } else if(eventType == XmlPullParser.TEXT) {
                    if (!
                            xpp.getText().trim().equals("")) {
                        Log.d(TAG, "Text " + xpp.getText());
                    }
                }else{


                }
                eventType = xpp.next();
            }

//
        }catch (Exception e){
            e.printStackTrace();
        }
    }


}
