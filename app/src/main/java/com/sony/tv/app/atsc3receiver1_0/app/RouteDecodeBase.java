package com.sony.tv.app.atsc3receiver1_0.app;

/**
 * Created by xhamc on 4/3/17.
 */

public interface RouteDecodeBase {


    public void fileName(String fileName);
    public int tsi();
    public int toi();
    public int arrayPosition();
    public String fileName();
    public int contentLength();
    public int efdt_toi();
    public boolean valid();

}
