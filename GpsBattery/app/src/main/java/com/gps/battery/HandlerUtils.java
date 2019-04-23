package com.gps.battery;

import android.os.Handler;
import android.os.Looper;

public class HandlerUtils {
    private static Handler handler = new MainHandler(Looper.getMainLooper());

    public static Handler getHandler() {
        return handler;
    }
}
