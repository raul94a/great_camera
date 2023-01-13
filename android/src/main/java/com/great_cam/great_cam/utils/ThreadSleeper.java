package com.great_cam.great_cam.utils;

import android.util.Log;

public class ThreadSleeper {

    public static void sleep(int millis){
        try{
            Thread.sleep(millis);
        }
        catch (InterruptedException ex){
            Log.e("ThreadSleeper", ex.toString());
        }
    }
}
