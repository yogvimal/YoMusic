package com.example.yogi.yomusic;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.yogi.yomusic.service.MediaPlayerService;

/**
 * Created by YOGI on 24-01-2018.
 */

public class MyApplication extends Application {
    private static MyApplication instance;
    private static SharedPreferences pref;
    private static MediaPlayerService service;

    //To check if Application is in the foreground
    private static boolean isAppVisible;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("YOGI","Application Created");
        instance = this;
        pref = PreferenceManager.getDefaultSharedPreferences(this);
    }

    public static MyApplication getInstance()
    {
        return instance;
    }

    public static Context getContext()
    {
        return instance;
    }

    public static SharedPreferences getPref()
    {
        return pref;
    }

    public static MediaPlayerService getService() {
        return service;
    }

    public static void setService(MediaPlayerService service) {
        MyApplication.service = service;
    }

    public static boolean isIsAppVisible() {
        return isAppVisible;
    }

    public static void setIsAppVisible(boolean isAppVisible) {
        MyApplication.isAppVisible = isAppVisible;
    }
}
