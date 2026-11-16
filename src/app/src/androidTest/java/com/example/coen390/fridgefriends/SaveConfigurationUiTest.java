package com.example.coen390.fridgefriends;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.matcher.IntentMatchers;

import com.example.coen390.fridgefriends.view.FridgeSetupActivity;
import com.example.coen390.fridgefriends.view.DeviceTrackerActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class SaveConfigurationUiTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences("DevicePrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        Intents.init();
    }

    @After
    public void tearDown() {
        SharedPreferences prefs = context.getSharedPreferences("DevicePrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        Intents.release();
    }

    @Test
    public void testSaveDeviceConfiguration() {
        // Test Case 3.4
        // Pre-Condition: User has selected tracking parameters for Device 1. (on Fridge setup menu)
        Intent intent = new Intent(context, FridgeSetupActivity.class);
        intent.putExtra("device_name", "Device 1");

        try (ActivityScenario<FridgeSetupActivity> scenario = ActivityScenario.launch(intent)) {
            // Step 1: Click the save button at the bottom.
            onView(withId(R.id.btnSave)).perform(click());

            // Expected Result 1: The configuration saves and the UI updates
            // According to code, saving navigates to DeviceTrackerActivity
            Intents.intended(IntentMatchers.hasComponent(DeviceTrackerActivity.class.getName()));
        }
    }
}
