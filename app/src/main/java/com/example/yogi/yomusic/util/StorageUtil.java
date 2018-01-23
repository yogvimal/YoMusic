package com.example.yogi.yomusic.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.yogi.yomusic.model.Audio;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Created by YOGI on 28-12-2017.
 */

public class StorageUtil {

    private final String STORAGE = "com.example.musicplayer.STORAGE";
    private SharedPreferences sharedPreferences;
    private Context context;

    public StorageUtil(Context context)
    {
        this.context = context;
    }

    public void storeAudio(ArrayList<Audio> audioList)
    {
        sharedPreferences = context.getSharedPreferences(STORAGE,Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();

        Gson gson = new Gson();
        String json = gson.toJson(audioList);
        editor.putString("audioList",json);
        editor.apply();

    }

    public ArrayList<Audio> loadAudio()
    {
        sharedPreferences = context.getSharedPreferences(STORAGE,Context.MODE_PRIVATE);

        Gson gson = new Gson();
        String json = sharedPreferences.getString("audioList",null);

        Type type = new TypeToken<ArrayList<Audio>>(){}.getType();
        return gson.fromJson(json,type);

    }

    public void storeAudioIndex(int index)
    {
        sharedPreferences = context.getSharedPreferences(STORAGE,Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("audioIndex",index);
        editor.apply();
    }

    public int loadAudioIndex()
    {
        sharedPreferences = context.getSharedPreferences(STORAGE,Context.MODE_PRIVATE);
        return sharedPreferences.getInt("audioIndex",-1);
    }

    public void clearCachedAudioPlayList()
    {
        sharedPreferences = context.getSharedPreferences(STORAGE,Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
    }
}
