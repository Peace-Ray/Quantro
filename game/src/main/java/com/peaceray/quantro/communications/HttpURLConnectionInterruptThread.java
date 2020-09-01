package com.peaceray.quantro.communications;

import java.net.HttpURLConnection;

public class HttpURLConnectionInterruptThread extends Thread {

    HttpURLConnection con;
    int timeout ;
    public HttpURLConnectionInterruptThread(HttpURLConnection con, int timeoutInMillis) {
        this.con = con;
        this.timeout = timeoutInMillis ;
    }

    public void run() {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {

        }
        ((HttpURLConnection)con).disconnect();
    }
    
}
