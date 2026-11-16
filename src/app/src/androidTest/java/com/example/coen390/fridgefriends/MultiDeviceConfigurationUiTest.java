package com.example.coen390.fridgefriends;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.coen390.fridgefriends.view.MainActivity;

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
public class MultiDeviceConfigurationUiTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences("DevicePrefs", Context.MODE_PRIVATE);
        // Pre-Condition: User is on the Main fridge menu with Device 1 added.
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
    public void testAddSecondDevice() {
        // Test Case 4.1
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Steps: 1. Click on the '+' button and select 'Add device'.
            onView(withId(R.id.btnPlus)).perform(click());
            onView(withId(R.id.btnAddDevice)).perform(click());

            // 2. Click on 'Device 2'.
            onView(withId(R.id.btnDevice2)).perform(click());

            // Expected Results: 1. The setup menu for Device 2 opens.
            // Check that we are on the setup menu and it says "Device 2"
            onView(withId(R.id.btnSave)).check(matches(isDisplayed()));
            onView(withId(R.id.tvDeviceNameLabel)).check(matches(withText("Device 2")));
        }
    }
}
