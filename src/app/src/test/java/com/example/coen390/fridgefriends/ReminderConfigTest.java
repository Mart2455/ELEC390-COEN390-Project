package com.example.coen390.fridgefriends;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.coen390.fridgefriends.alert.ReminderConfig;

import org.junit.Test;

//ALERT-2.1: checks which reminder is due based on the days left
public class ReminderConfigTest {

    //first reminder
    @Test
    public void firstReminderAtSevenDays() {
        assertEquals(ReminderConfig.ReminderType.FIRST, ReminderConfig.getDueReminder(7));
    }

    //item added late (3 days left): first reminder right away
    @Test
    public void firstReminderWhenAddedLate() {
        assertEquals(ReminderConfig.ReminderType.FIRST, ReminderConfig.getDueReminder(3));
    }

    //1 day left: second reminder
    @Test
    public void secondReminderOneDayBefore() {
        assertEquals(ReminderConfig.ReminderType.SECOND, ReminderConfig.getDueReminder(1));
    }

    //more than 7 days left: too early
    @Test
    public void noReminderTooEarly() {
        assertNull(ReminderConfig.getDueReminder(8));
    }

    //expires today or already expired: no reminder
    @Test
    public void noReminderTodayOrExpired() {
        assertNull(ReminderConfig.getDueReminder(0));
        assertNull(ReminderConfig.getDueReminder(-2));
    }

    //no expiry date: no reminder
    @Test
    public void noReminderWithoutDate() {
        assertNull(ReminderConfig.getDueReminder(9999));
    }
}