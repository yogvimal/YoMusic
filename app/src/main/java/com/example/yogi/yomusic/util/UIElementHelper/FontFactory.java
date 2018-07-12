package com.example.yogi.yomusic.util.UIElementHelper;

import android.graphics.Typeface;

import com.example.yogi.yomusic.MyApplication;
import com.example.yogi.yomusic.R;
import com.example.yogi.yomusic.model.Constants;

/**
 * Created by YOGI on 29-01-2018.
 */

public class FontFactory {
    public static Typeface getFont()
    {
        Typeface t = Typeface.MONOSPACE;
        switch (MyApplication.getPref()
                .getInt(MyApplication.getContext().getString(R.string.pref_text_font), Constants.FONTS.MONOSPACE))
        {
            case Constants.FONTS.NORMAL:
                t = Typeface.DEFAULT;
                break;
            case Constants.FONTS.SANS_SERIF:
                t = Typeface.SANS_SERIF;
                break;
            case Constants.FONTS.SERIF:
                t = Typeface.SERIF;
                break;
        };
        return t;
    }
}
