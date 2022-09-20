package com.hufi.internetspeedmeter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!CheckConnection.haveNetworkConnection(getApplicationContext())) {
            CheckConnection.ShowToast_Short(getApplicationContext(), "No internet connection.");
            //stopService(new Intent(this, InternetSpeedMeter.class));
            //mHandler.removeCallbacks(mRunnable);
            //finish();
        }
        else {
            if (!isMyServiceRunning(InternetSpeedMeter.class))
            {
                startService(new Intent(this, InternetSpeedMeter.class));
            }
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            long txBytes = intent.getLongExtra("txBytes", 0);
            long rxBytes = intent.getLongExtra("rxBytes", 0);

            TextView RX = (TextView) findViewById(R.id.txtDownloadSpeed);
            TextView TX = (TextView) findViewById(R.id.txtUploadSpeed);

            String downloadSpeed, downloadUnit, uploadSpeed, uploadUnit;
            String contentDownload, contentUpload;

            //rxBytes = (TrafficStats.getTotalRxBytes() - mStartRX)/1024;        //KBps
            if (rxBytes < 1000) {
                downloadSpeed = Long.toString(rxBytes);
                downloadUnit = "KB/s";
            }
            else {
                downloadSpeed = Double.toString((double)Math.round((double)rxBytes / 1000 * 10) / 10);
                downloadUnit = "MB/s";
            }
            contentDownload = "Download: " + downloadSpeed + " " + downloadUnit;
            RX.setText(contentDownload);

            //txBytes = (TrafficStats.getTotalTxBytes() - mStartTX)/1024;           //KBps
            if (txBytes < 1000) {
                uploadSpeed = Long.toString(txBytes);
                uploadUnit = "KB/s";
            }
            else {
                uploadSpeed = Double.toString((double)Math.round((double)txBytes / 1000 * 10) / 10);
                uploadUnit = "MB/s";
            }
            contentUpload = "Upload: " + uploadSpeed + " " + uploadUnit;
            TX.setText(contentUpload);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        if (!isMyServiceRunning(InternetSpeedMeter.class))
        {
            startService(new Intent(this, InternetSpeedMeter.class));
        }

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(messageReceiver, new IntentFilter("internet-speed"));
    }

    @Override
    protected void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        super.onPause();
    }
}