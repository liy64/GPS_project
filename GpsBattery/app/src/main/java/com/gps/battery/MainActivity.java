package com.gps.battery;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_LOCATION = 1;
    private static final String TAG = "MainActivity";
    private String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    private GpsManager gpsManager;
    private TextView tvLocationInfo;
    private TextView tvTime;
    private EditText etUpdateTime;
    private Button btnUpdateTime;
    private TextView tvBatteryInfo;
    private TextView tvBatteryPercent;
    private TextView tvGpsStatus;
    private Switch sGps;
    private TextView tvErrorTips;

    private LocationListener listener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            tvLocationInfo.setText(getString(R.string.location_info, location.getLatitude(), location.getLongitude()));
            tvTime.setText(getString(R.string.update_time, TimeUtils.format(System.currentTimeMillis())));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
            boolean enable = LocationManager.GPS_PROVIDER.equals(provider);
            updateGpsStatus(enable);
        }

        @Override
        public void onProviderDisabled(String provider) {
            boolean enable = !LocationManager.GPS_PROVIDER.equals(provider);
            updateGpsStatus(enable);
        }
    };
    private GpsManager.AutoPauseListener autoPauseListener = new GpsManager.AutoPauseListener() {
        @Override
        public void onPause() {
            tvErrorTips.setText(R.string.tips_auto_pause);
            openGps(false);
        }
    };
    private BroadcastReceiver batteryReceiver = new PowerConnectionReceiver();

    private void updateGpsStatus(boolean enable) {
        tvGpsStatus.setText(getString(R.string.gps_status, enable ? "open" : "close"));
        if (sGps.isChecked() != enable) {
            sGps.setChecked(enable);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initEvent();
        ActivityCompat.requestPermissions(this, permissions, REQ_LOCATION);
        initBatteryInfo();
    }

    private void initBatteryInfo() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = registerReceiver(batteryReceiver, ifilter);
        getBatteryInfo(intent);
    }

    private void initEvent() {
        btnUpdateTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadData();
            }
        });
        sGps.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                openGps(isChecked);
            }
        });
    }

    private void openGps(boolean open) {
        GpsUtils.openGPS(this, open);
        if (open) {
            initGpsManager();
            loadData();
        }
        updateGpsStatus(open);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isInit()) {
            gpsManager.destroy();
        }
        unregisterReceiver(batteryReceiver);
    }

    private void initView() {
        tvLocationInfo = findViewById(R.id.tv_location_info);
        tvTime = findViewById(R.id.tv_time);
        etUpdateTime = findViewById(R.id.et_update_time);
        btnUpdateTime = findViewById(R.id.btn_update_time);
        tvBatteryInfo = findViewById(R.id.tv_battery_info);
        tvBatteryPercent = findViewById(R.id.tv_battery_percent);
        tvGpsStatus = findViewById(R.id.tv_gps_status);
        sGps = findViewById(R.id.s_gps);
        tvErrorTips = findViewById(R.id.tv_error_tips);

        tvLocationInfo.setText(getString(R.string.location_info, 0., 0.));
        tvTime.setText(getString(R.string.update_time, TimeUtils.format(System.currentTimeMillis())));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_LOCATION) {
            return;
        }

        if (grantResults == null || grantResults.length != 2) {
            tvErrorTips.setText(R.string.tips_no_gps_permission);
            return;
        }

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            onLocationPermissionGranted();
        }
    }

    private void onLocationPermissionGranted() {
        Log.i(TAG, "onLocationPermissionGranted");

        // check is open
        boolean isGpsOpen = GpsUtils.isOpen(this);
        updateGpsStatus(isGpsOpen);
        if (!isGpsOpen) {
            Toast.makeText(this, R.string.tips_gps_disabled, Toast.LENGTH_LONG).show();
            tvErrorTips.setText(R.string.tips_gps_disabled);
            return;
        }

        initGpsManager();
    }

    private void initGpsManager() {
        if (isInit()) {
            return;
        }
        gpsManager = GpsManager.getInstance(this);
        gpsManager.init();
        gpsManager.setOnLocationChangeListener(listener);
        gpsManager.setOnAutoPauseListener(autoPauseListener);
        loadData();
    }


    private boolean isInit() {
        return gpsManager != null;
    }

    private void loadData() {
        if (!isInit()) {
            Toast.makeText(this, R.string.tips_no_gps_permission, Toast.LENGTH_SHORT).show();
            return;
        }
        int updateTime = getInputUpdateTime();
        if (updateTime <= 0) {
            return;
        }
        tvErrorTips.setText("");
        gpsManager.setUpdateTime(updateTime);
        Location location = gpsManager.getCurrentLocation();
        if (location != null) {
            listener.onLocationChanged(location);
        }
    }

    private int getInputUpdateTime() {
        String updateTimeStr = etUpdateTime.getText().toString();
        int updateTime = 0;
        try {
            updateTime = Integer.parseInt(updateTimeStr);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (updateTime <= 0) {
            Toast.makeText(this, R.string.tips_invalid_input_update_time, Toast.LENGTH_SHORT).show();
        }
        return updateTime;
    }

    public class PowerConnectionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            getBatteryInfo(intent);
        }
    }

    private void getBatteryInfo(Intent intent) {
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPercent = level / (float) scale;
        Log.i(TAG, "isCharging:" + isCharging + ", chargePlug:" + chargePlug + ", batteryPct:" + batteryPercent);
        updateBatteryInfo(isCharging, chargePlug, batteryPercent);
    }

    private void updateBatteryInfo(boolean isCharging, int chargePlug, float batteryPercent) {
        String chargingInfo;
        if (isCharging) {
            chargingInfo = "Charging by ";
            switch (chargePlug) {
                case BatteryManager.BATTERY_PLUGGED_AC:
                    chargingInfo += "AC";
                    break;
                case BatteryManager.BATTERY_PLUGGED_USB:
                    chargingInfo += "USB";
                    break;
                case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                    chargingInfo += "wireless";
                    break;
                default:
                    chargingInfo = "Not charged";
                    break;
            }
        } else {
            chargingInfo = "Not charged";
        }
        tvBatteryInfo.setText(getString(R.string.battery_info, chargingInfo));
        tvBatteryPercent.setText(getString(R.string.battery_percent, String.format("%.0f%%", batteryPercent * 100)));
    }
}
