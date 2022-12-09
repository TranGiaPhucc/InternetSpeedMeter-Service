package com.hufi.internetspeedmeter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Timer;
import java.util.TimerTask;

public class InternetSpeedMeter extends Service {
    private String connectionType = "";
    private Handler mHandler = new Handler();
    private long mStartRX = 0;
    private long mStartTX = 0;
    public static long txBytes = 0;
    public static long rxBytes = 0;

    public InternetSpeedMeter() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Timer timer = new Timer();
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

                //For 3G check
                boolean is3g = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                        .isConnectedOrConnecting();
                //For WiFi Check
                boolean isWifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                        .isConnectedOrConnecting();


                if (!is3g && !isWifi)
                {
                    connectionType = "No internet connection";
                }
                else
                {
                    if (is3g)
                        connectionType = "MOBILE";
                    if (isWifi)
                        connectionType = "WIFI";
                }
            }
        }, 0, 1000);

        //onDestroy();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            start();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void start()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("My notification", "My notification", NotificationManager.IMPORTANCE_HIGH);
            channel.setVibrationPattern(new long[]{ 0 });
            channel.enableVibration(true);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        mStartRX = TrafficStats.getTotalRxBytes();
        mStartTX = TrafficStats.getTotalTxBytes();

        if (mStartRX == TrafficStats.UNSUPPORTED || mStartTX == TrafficStats.UNSUPPORTED) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Uh Oh!");
            alert.setMessage("Your device does not support traffic stat monitoring.");
            alert.show();
        } else {
            mHandler.postDelayed(mRunnable, 1000);
        }
    }

    private final Runnable mRunnable = new Runnable() {
        public void run() {
            rxBytes = (TrafficStats.getTotalRxBytes() - mStartRX)/1024;        //KBps
            txBytes = (TrafficStats.getTotalTxBytes() - mStartTX)/1024;           //KBps

            showNotification();

            mStartRX = TrafficStats.getTotalRxBytes();
            mStartTX = TrafficStats.getTotalTxBytes();

            sendMessage();

            mHandler.postDelayed(mRunnable, 1000);
        }
    };

    private void sendMessage() {
        // The string "my-message" will be used to filer the intent
        Intent intent = new Intent("internet-speed");
        // Adding some data
        intent.putExtra("txBytes", txBytes);
        intent.putExtra("rxBytes", rxBytes);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @SuppressLint("RestrictedApi")
    private void showNotification() {
        // TODO Auto-generated method stub

        String bmSpeed, bmUnit;
        String uploadSpeed, downloadSpeed, uploadUnit, downloadUnit;
        String contentText;

        if (txBytes < 1000) {
            uploadSpeed = Long.toString(txBytes);
            uploadUnit = "KB/s";
        }
        else {
            uploadSpeed = Double.toString((double)Math.round((double)txBytes / 1000 * 10) / 10);
            uploadUnit = "MB/s";
        }

        if (rxBytes < 1000) {
            bmSpeed = Long.toString(rxBytes);
            bmUnit = "KB/s";
            downloadSpeed = Long.toString(rxBytes);
            downloadUnit = "KB/s";
        }
        else {
            bmSpeed = Double.toString((double)Math.round((double)rxBytes / 1000 * 10) / 10);
            bmUnit = "MB/s";
            downloadSpeed = Double.toString((double)Math.round((double)rxBytes / 1000 * 10) / 10);
            downloadUnit = "MB/s";
        }

        contentText = "Upload: " + uploadSpeed + " " + uploadUnit + "        Download: " + downloadSpeed + " " + downloadUnit;
        //contentText = "Upload: " + Double.toString((double)Math.round((double)txBytes / 1000 * 10) / 10) + " MBps        Download: " + Double.toString((double)Math.round(rxBytes / 1000 * 10) / 10) + " MBps";

        Bitmap bitmap = createBitmapFromString(bmSpeed, bmUnit);
        Icon icon = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            icon = Icon.createWithBitmap(bitmap);
        }

        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        //For 3G check
        boolean is3g = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                .isConnectedOrConnecting();
        //For WiFi Check
        boolean isWifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                .isConnectedOrConnecting();
        if (is3g || isWifi) {
            //NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "My notification");
            startForeground(1, new NotificationCompat.Builder(this, "My notification")
                    //.setContentTitle("Internet Speed Meter" + "     " + connectionType)
                    .setContentTitle("Connection type: " + connectionType)
                    .setContentText(contentText)
                    //builder.setSmallIcon(R.mipmap.ic_launcher_round);
                    .setSmallIcon(IconCompat.createFromIcon(icon))
                    .setAutoCancel(false)
                    .setOnlyAlertOnce(true)
                    .build());
        }
        else {
            stopForeground(true);

            NotificationManager notificationManager = (NotificationManager)
                    getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
        }
        /*NotificationManagerCompat managerCompat = NotificationManagerCompat.from(this);
        managerCompat.notify(1, builder.build());*/
    }

    private Bitmap createBitmapFromString(String speed, String units) {

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextSize(55);
        paint.setTextAlign(Paint.Align.CENTER);

        Paint unitsPaint = new Paint();
        unitsPaint.setAntiAlias(true);
        unitsPaint.setTextSize(40); // size is in pixels
        unitsPaint.setTextAlign(Paint.Align.CENTER);

        Rect textBounds = new Rect();
        paint.getTextBounds(speed, 0, speed.length(), textBounds);

        Rect unitsTextBounds = new Rect();
        unitsPaint.getTextBounds(units, 0, units.length(), unitsTextBounds);

        int width = (textBounds.width() > unitsTextBounds.width()) ? textBounds.width() : unitsTextBounds.width();

        Bitmap bitmap = Bitmap.createBitmap(width + 10, 90,
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        canvas.drawText(speed, width / 2 + 5, 50, paint);
        canvas.drawText(units, width / 2, 90, unitsPaint);

        return bitmap;
    }
}