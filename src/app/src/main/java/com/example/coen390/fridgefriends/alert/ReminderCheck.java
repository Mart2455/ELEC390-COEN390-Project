package com.example.coen390.fridgefriends.alert;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.coen390.fridgefriends.model.DateParser;
import com.example.coen390.fridgefriends.model.GroceryItem;

import org.json.JSONArray;

//daily check that sends reminders for food about to expire
public class ReminderCheck extends Worker {

    public ReminderCheck(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    //runs once a day and goes through the food items and sends the reminders that are due
    @NonNull
    @Override
    public Result doWork() {
        ReminderNotifier notifier = new ReminderNotifier(getApplicationContext());

        SharedPreferences prefs = getApplicationContext().getSharedPreferences("GroceryPrefs", Context.MODE_PRIVATE);
        String savedList = prefs.getString("grocery_list", "[]");

        //try to read the saved list and fail if the data is corrupted
        try {
            JSONArray items = new JSONArray(savedList);
            for (int i = 0; i < items.length(); i++) {
                GroceryItem item = GroceryItem.fromJSON(items.getJSONObject(i));

                //continue only if the food is owned by the user and skips if its an item in the shopping list
                if (!item.isOwned())
                    continue;

                int daysLeft = DateParser.getDaysUntilExpiry(item.getExpiryDate());
                ReminderConfig.ReminderType type = ReminderConfig.getDueReminder(daysLeft);

                if (type != null)
                    notifier.showReminder(item, i, type, daysLeft);
            }
        } catch (Exception e) {
            return Result.failure();
        }
        return Result.success();
    }
}