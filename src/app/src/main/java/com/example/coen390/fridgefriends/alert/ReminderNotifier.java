package com.example.coen390.fridgefriends.alert;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.coen390.fridgefriends.model.DateParser;
import com.example.coen390.fridgefriends.model.GroceryItem;
import com.example.coen390.fridgefriends.view.NewItemActivity;

//send reminder notification when food is about to expire
public class ReminderNotifier {

    //separate channel from the spoilage alerts
    private static final String CHANNEL_ID = "consumption_reminders";

    private Context context;

    public ReminderNotifier(Context context) {
        this.context = context;
        createChannel();
    }

    //android 8+ needs a channel before showing a notification
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Consumption reminders",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Reminders when food should be eaten soon");

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    public static String buildTitle(String foodName, ReminderConfig.ReminderType type, int daysLeft) {
        if (type == ReminderConfig.ReminderType.SECOND)
            return foodName + " expires tomorrow";
        return foodName + " expires in " + daysLeft + " days";
    }

    //notification txt
    public static String buildText(String expiryDate) {
        return "Expiry date: " + DateParser.formatDisplayDate(expiryDate);
    }

    //shows the reminder notif for one food item
    public void showReminder(GroceryItem item, int index, ReminderConfig.ReminderType type, int daysLeft) {
        //for android 13+ it does nothing if the user did not allow notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED)
            return;

        //one id per food item, so the "tomorrow" notif replaces the "in X days" one
        int notifId = ("reminder_" + item.getName()).hashCode();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(buildTitle(item.getName(), type, daysLeft))
                .setContentText(buildText(item.getExpiryDate()))
                .setContentIntent(openFoodItem(item, index, notifId))
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(notifId, builder.build());
    }

    //opens the food item screen when the notif is tapped and sends the same info the grocery list sends when you tap an item to edit it
    private PendingIntent openFoodItem(GroceryItem item, int index, int requestCode) {
        Intent intent = new Intent(context, NewItemActivity.class);
        intent.putExtra(NewItemActivity.EXTRA_IS_EDIT_MODE, true);
        intent.putExtra(NewItemActivity.EXTRA_ITEM_INDEX, index);
        intent.putExtra(NewItemActivity.EXTRA_ITEM_NAME, item.getName());
        intent.putExtra(NewItemActivity.EXTRA_ITEM_EXPIRY, item.getExpiryDate());
        intent.putExtra(NewItemActivity.EXTRA_ITEM_IMAGE_URL, item.getImageUrl());
        intent.putExtra(NewItemActivity.EXTRA_ITEM_CATEGORY, item.getCategory());
        intent.putExtra(NewItemActivity.EXTRA_ITEM_QUANTITY, item.getQuantity());
        intent.putExtra(NewItemActivity.EXTRA_ITEM_UNIT, item.getUnit());
        intent.putExtra(NewItemActivity.EXTRA_ITEM_MEMO, item.getMemo());
        intent.putExtra(NewItemActivity.EXTRA_ITEM_DEVICE_ID, item.getDeviceId());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        return PendingIntent.getActivity(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}