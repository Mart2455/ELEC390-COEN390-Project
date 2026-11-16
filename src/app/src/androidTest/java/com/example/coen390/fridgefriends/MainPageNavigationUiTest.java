package com.example.coen390.fridgefriends;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.matcher.IntentMatchers;
import androidx.test.espresso.contrib.RecyclerViewActions;

import com.example.coen390.fridgefriends.view.MainActivity;
import com.example.coen390.fridgefriends.view.DeviceTrackerActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;

@RunWith(AndroidJUnit4.class)
public class MainPageNavigationUiTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences("DevicePrefs", Context.MODE_PRIVATE);
        
        // Pre-Condition: The Main fridge menu is populated with devices.
        prefs.edit().clear()
                .putBoolean("Device 1_added", true)
                .putInt("Device 1_icon", R.drawable.happy_apples)
                .putBoolean("Device 1_track_freshness", true)
                .commit();
        
        Intents.init();
    }

    @After
    public void tearDown() {
        SharedPreferences prefs = context.getSharedPreferences("DevicePrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        Intents.release();
    }

    @Test
    public void testNavigateToDeviceTrackerMenu() {
        // Test Case 6.1
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Check that the RecyclerView is visible and the Empty layout is gone
            onView(withId(R.id.rvDevices)).check(matches(isDisplayed()));

            // Steps: 1. Click on 'Device 1'
            // We find the RecyclerView item that contains the text 'Device 1' and click it
            onView(withId(R.id.rvDevices))
                .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText("Device 1")), click()));

            // Expected Results: 1. The screen expands to the specific device tracker menu.
            Intents.intended(IntentMatchers.hasComponent(DeviceTrackerActivity.class.getName()));
            Intents.intended(IntentMatchers.hasExtra("device_name", "Device 1"));
        }
    }
}
