package com.example.coen390.fridgefriends;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.coen390.fridgefriends.view.AddDeviceActivity;
import com.example.coen390.fridgefriends.view.FridgeSetupActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class MultiDeviceThirdUiTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences("DevicePrefs", Context.MODE_PRIVATE);
        
        // Pre-Condition for Test 4.3: Device 1 & Device 2 are added. We are on Available Devices menu.
        prefs.edit().clear()
                .putBoolean("Device 1_added", true)
                .putInt("Device 1_icon", R.drawable.happy_apples)
                .putBoolean("Device 2_added", true)
                .putInt("Device 2_icon", R.drawable.cheese_icon)
                .commit();
    }

    @After
    public void tearDown() {
        SharedPreferences prefs = context.getSharedPreferences("DevicePrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
    }

    @Test
    public void testSelectThirdDevice() {
        // Test Case 4.3
        // Pre-Condition: User is on the Available devices menu.
        Intent intent = new Intent(context, AddDeviceActivity.class);
        
        try (ActivityScenario<AddDeviceActivity> scenario = ActivityScenario.launch(intent)) {
            // Steps: 1. Click on 'Device 3'
            onView(withId(R.id.btnDevice3)).perform(click());

            // Expected Results: 1. The Fridge setup menu opens for Device 3.
            onView(withId(R.id.btnSave)).check(matches(isDisplayed()));
            onView(withId(R.id.tvDeviceNameLabel)).check(matches(withText("Device 3")));
        }
    }

    @Test
    public void testSaveThirdDeviceConfiguration() {
        // Test Case 4.4
        // Pre-Condition: User is on the Fridge setup menu for Device 3.
        Intent intent = new Intent(context, FridgeSetupActivity.class);
        intent.putExtra("device_name", "Device 3");

        try (ActivityScenario<FridgeSetupActivity> scenario = ActivityScenario.launch(intent)) {
            // Steps: 1. Select tracking parameters and click save.
            onView(withId(R.id.btnFermentation)).perform(click());
            onView(withId(R.id.btnSave)).perform(scrollTo(), click());

            // Navigate back to the Main Activity from the Tracker Activity to check results
            onView(withId(R.id.navFridgeTracker)).perform(click());

            // Expected Results: 1. The Main fridge menu updates to display Device 1, Device 2, and Device 3.
            onView(withId(R.id.rvDevices)).check(matches(isDisplayed()));
            onView(withText("Device 1")).check(matches(isDisplayed()));
            onView(withText("Device 2")).check(matches(isDisplayed()));
            onView(withText("Device 3")).check(matches(isDisplayed()));
        }
    }
}
