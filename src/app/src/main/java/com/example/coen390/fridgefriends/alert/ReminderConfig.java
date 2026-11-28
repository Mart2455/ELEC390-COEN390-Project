package com.example.coen390.fridgefriends.alert;

//Consumption reminder rules (Owned items with an expiry date only)
//Reminders: 7 days before and 1 day before expiry
public class ReminderConfig {

    private ReminderConfig() {}

    //first reminder is sent a week before the expiry date
    public static final int FIRST_REMINDER_DAYS = 7;
    //second reminder is sent one day before expiry date
    public static final int SECOND_REMINDER_DAYS = 1;
    //Time of the daily check
    public static final int CHECK_HOUR = 9;

    //the two kinds of reminders
    public enum ReminderType {
        FIRST,   //7 days before
        SECOND   //1 day before
    }

    //which reminder is due for an item, based on its days left
    //returns null when no reminder is due
    public static ReminderType getDueReminder(int daysLeft) {
        if (daysLeft == SECOND_REMINDER_DAYS)
            return ReminderType.SECOND;
        if (daysLeft > SECOND_REMINDER_DAYS && daysLeft <= FIRST_REMINDER_DAYS)
            return ReminderType.FIRST;
        return null;
    }
}
