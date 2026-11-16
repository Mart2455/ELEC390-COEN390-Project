package com.example.coen390.fridgefriends;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.coen390.fridgefriends.view.FridgeSetupActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isSelected;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;

@RunWith(AndroidJUnit4.class)
public class DeviceConfigurationUiTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences("DevicePrefs", Context.MODE_PRIVATE);
        prefs.edit().clear()
                .putBoolean("Device 1_added", true)
                .putBoolean("Device 1_track_freshness", false)
                .commit();
    }

    @After
    public void tearDown() {
        SharedPreferences prefs = context.getSharedPreferences("DevicePrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
    }

    @Test
    public void testSelectTrackingParameter() {
        Intent intent = new Intent(context, FridgeSetupActivity.class);
        intent.putExtra("device_name", "Device 1");

        try (ActivityScenario<FridgeSetupActivity> scenario = ActivityScenario.launch(intent)) {
            // Test Case 3.1
            // Pre-Condition: User is on the Fridge setup menu for a device.
            onView(withId(R.id.btnMoisture)).check(matches(isDisplayed()));
            
            // Steps: 1. Click on 'Moisture'
            onView(withId(R.id.btnMoisture)).perform(click());

            // Expected Results: 1. The Moisture button highlights to indicate it is selected.
            onView(withId(R.id.btnMoisture)).check(matches(isSelected()));
        }
    }
}
