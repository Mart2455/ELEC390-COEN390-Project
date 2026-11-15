package com.example.coen390.fridgefriends.alert;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.coen390.fridgefriends.view.DeviceTrackerActivity;

import java.util.Locale;

public class AlertNotifier {

    //new channel id for notif
    private static final String CHANNEL_ID = "spoilage_alerts";

    private final Context context;

    public AlertNotifier(Context context) {
        this.context = context;
        createChannel();
    }

    //for android 8+, it needs a channel before showing a notification
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Spoilage alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Alerts when a sensor shows food might be going bad");

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    //join device name and sensor display name
    public static String buildTitle(String deviceName, Sensor sensor) {
        return deviceName + ": " + sensor.getSensorName() + " alert";
    }

    public static String buildText(Sensor sensor, double reading) {
        String direction;
        if (sensor.getAlertIfAbove())
            direction = "above";
        else
            direction = "below";

        return "Reading " + formatValue(reading) + " " + sensor.getUnit()
                + " is " + direction + " the limit of "
                + formatValue(sensor.getLimit()) + " " + sensor.getUnit();
    }

    public static String formatValue(double value) {
        if (value == Math.floor(value))
            return String.valueOf((long) value);
        return String.format(Locale.US, "%.1f", value);
    }

    //shows the notification for one device + sensor
    public void showAlert(String deviceName, Sensor sensor, double reading) {
        //Android 13+: do nothing if the user did not allow notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED)
            return;

        //different id for each device and sensor so notifs don't overwrite each other
        int notificationId = (deviceName + "_" + sensor.name()).hashCode();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(buildTitle(deviceName, sensor))
                .setContentText(buildText(sensor, reading))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openTracker(deviceName, notificationId))
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }

    //which device tracker is opened when clicking on notif
    private PendingIntent openTracker(String deviceName, int requestCode) {
        SharedPreferences prefs = context.getSharedPreferences("DevicePrefs", Context.MODE_PRIVATE);

        Intent intent = new Intent(context, DeviceTrackerActivity.class);
        intent.putExtra("device_name", deviceName);
        intent.putExtra("track_freshness", prefs.getBoolean(deviceName + "_track_freshness", false));
        intent.putExtra("track_moisture", prefs.getBoolean(deviceName + "_track_moisture", false));
        intent.putExtra("track_fermentation", prefs.getBoolean(deviceName + "_track_fermentation", false));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        return PendingIntent.getActivity(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }


}