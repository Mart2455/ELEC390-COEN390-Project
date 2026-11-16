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
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class MultiDeviceSecondUiTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences("DevicePrefs", Context.MODE_PRIVATE);
        // Pre-Condition: Device 1 is already added.
        prefs.edit().clear()
                .putBoolean("Device 1_added", true)
                .putInt("Device 1_icon", R.drawable.happy_apples)
                .commit();
    }

    @After
    public void tearDown() {
        SharedPreferences prefs = context.getSharedPreferences("DevicePrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
    }

    @Test
    public void testSaveSecondDeviceConfiguration() {
        // Test Case 4.2
        // Pre-Condition: User is on the Fridge setup menu for Device 2.
        Intent intent = new Intent(context, FridgeSetupActivity.class);
        intent.putExtra("device_name", "Device 2");

        try (ActivityScenario<FridgeSetupActivity> scenario = ActivityScenario.launch(intent)) {
            // Steps: 1. Select tracking parameters and click save.
            onView(withId(R.id.btnMoisture)).perform(click()); // Select something
            onView(withId(R.id.btnSave)).perform(click());

            // Expected Results: 1. The Main fridge menu updates to display both Device 1 and Device 2
            // Because saving navigates to DeviceTrackerActivity, we need to click the back button or home button
            // to return to MainActivity to check this.
            onView(withId(R.id.navFridgeTracker)).perform(click());

            // We are now on MainActivity. The layoutEmptyState should be gone, and rvDevices should be visible.
            onView(withId(R.id.rvDevices)).check(matches(isDisplayed()));

            // Note: In Espresso, verifying specific items in a RecyclerView is complex without custom matchers,
            // but we can verify that the list exists and the empty placeholder does not.
            onView(withText("Device 1")).check(matches(isDisplayed()));
            onView(withText("Device 2")).check(matches(isDisplayed()));
        }
    }
}
