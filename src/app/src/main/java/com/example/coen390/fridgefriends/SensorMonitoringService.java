package com.example.coen390.fridgefriends;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.coen390.fridgefriends.network.ESP32Receiver;

public class SensorMonitoringService extends Service
{


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {

        Log.d("SensorMonitoring", "Service started");
        Notification notification = new NotificationCompat.Builder(this, "sensor_monitoring")
                .setContentTitle("Sensor monitoring")
                .setContentText("Monitoring your selected device")
                .setSmallIcon(R.drawable.baseline_add_24)
                .setOngoing(true)
                .build();


        startForeground(1, notification);


        ESP32Receiver.receiveData(() -> {
            double humidity = ESP32Receiver.getHumidity();
            int tvoc = ESP32Receiver.getTvoc();
            int eco2 = ESP32Receiver.getEco2();
            int mq3Raw = ESP32Receiver.getMq3Raw();

            Log.d("SensorMonitoring",
                    "Humidity: " + humidity
                            + ", TVOC: " + tvoc
                            + ", eCO2: " + eco2
                            + ", MQ3: " + mq3Raw);
        });
        return START_NOT_STICKY;
    }



    @Override
    public void onCreate()
    {
        // When the device is setup, we launch on create that picks up the sensor readings
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(
                    "sensor_monitoring",
                    "Sensor monitoring",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        // Cancel scheduled polling and release resources.
        super.onDestroy();
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}