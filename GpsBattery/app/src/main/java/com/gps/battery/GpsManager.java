package com.gps.battery;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

public class GpsManager {
    private static final long INTERVAL = 20000;// 20s
    private static final double TOLERANCE = 0.000015;// 0.00001° about  1.x meter
    private static final String TAG = "GpsManager";
    private static GpsManager gpsManager;
    private LocationListener locationListener;

    public static GpsManager getInstance(Context context) {
        if (gpsManager == null) {
            gpsManager = new GpsManager(context);
        }
        return gpsManager;
    }

    private final LocationManager lm;
    private String provider;
    private long updateTime;// gps update time
    private Location lastLocation;
    private Context context;

    public static interface AutoPauseListener {
        void onPause();
    }

    private AutoPauseListener autoPauseListener;

    private class PowerSaveTask implements Runnable {
        private Location lastLocation;

        public PowerSaveTask(Location lastLocation) {
            this.lastLocation = lastLocation;
        }

        @Override
        public void run() {
            Location currentLocation = getCurrentLocation();
            // pause gps if no location change
            if (isNear(lastLocation, currentLocation)) {
                Toast.makeText(context, R.string.tips_auto_pause, Toast.LENGTH_SHORT).show();
                pause();
                if (autoPauseListener != null) {
                    autoPauseListener.onPause();
                }
            }
        }
    }

    private PowerSaveTask powerSaveTask;

    private LocationListener listener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.i(TAG, "onLocationChanged:" + location);
            lastLocation = location;
            if (locationListener != null) {
                locationListener.onLocationChanged(location);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.i(TAG, "onStatusChanged provider:" + provider + ", status:" + status);
            if (locationListener != null) {
                locationListener.onStatusChanged(provider, status, extras);
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.i(TAG, "onProviderEnabled:" + provider);
            if (locationListener != null) {
                locationListener.onProviderEnabled(provider);
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.i(TAG, "onProviderDisabled:" + provider);
            if (locationListener != null) {
                locationListener.onProviderDisabled(provider);
            }
        }
    };


    public GpsManager(Context context) {
        lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.context = context.getApplicationContext();
    }


    @SuppressLint("MissingPermission")
    public void init() {
        List<String> list = lm.getProviders(true);
        // GPS
        if (list.contains(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
            setUpdateTime(2000);
        }
    }

    private boolean isInit() {
        return provider != null;
    }

    @SuppressLint("MissingPermission")
    public void setUpdateTime(long ms) {
        if (!isInit()) {
            return;
        }
        pause();
        updateTime = ms;
        lm.requestLocationUpdates(provider, ms, 0, listener);
        // start handler to check whether moved in 20s
        if (powerSaveTask != null) {
            HandlerUtils.getHandler().removeCallbacks(powerSaveTask);
        }
        Location lastLocation = getCurrentLocation();
        powerSaveTask = new PowerSaveTask(lastLocation);
        HandlerUtils.getHandler().postDelayed(powerSaveTask, INTERVAL);
    }

    private boolean isNear(Location lastLocation, Location currentLocation) {
        if (lastLocation == null && currentLocation == null) {
            return true;
        }
        if (lastLocation == null || currentLocation == null) {
            return false;
        }
        double diffLat = lastLocation.getLatitude() - currentLocation.getLatitude();
        double diffLon = lastLocation.getLongitude() - currentLocation.getLongitude();
        if (Math.abs(diffLat) < TOLERANCE && Math.abs(diffLon) < TOLERANCE) {
            return true;
        }
        return false;
    }

    public void pause() {
        lm.removeUpdates(listener);
    }

    @SuppressLint("MissingPermission")
    public Location getCurrentLocation() {
        if (lastLocation == null) {
            lastLocation = lm.getLastKnownLocation(provider);
        }
        return lastLocation;
    }

    /**
     * open or close gps
     *
     * @param context
     * @param open
     */
    public void openGPS(Context context, boolean open) {
        GpsUtils.openGPS(context, open);
    }

    public void setOnLocationChangeListener(LocationListener listener) {
        this.locationListener = listener;
    }

    public void removeLocationChangeListener() {
        this.locationListener = null;
    }

    public void setOnAutoPauseListener(AutoPauseListener listener) {
        this.autoPauseListener = listener;
    }

    public void removeAutoPauseListener() {
        this.autoPauseListener = null;
    }

    public void destroy() {
        pause();
        removeLocationChangeListener();
        removeAutoPauseListener();
        provider = null;
    }
}
