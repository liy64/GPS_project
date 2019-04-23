package com.gps.battery;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtils {
    private static final DateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static String format(long ts) {
        return dataFormat.format(new Date(ts));
    }
}
