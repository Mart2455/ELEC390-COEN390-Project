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
public class AddingDevicesUiTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences("DevicePrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
    }

    @After
    public void tearDown() {
        SharedPreferences prefs = context.getSharedPreferences("DevicePrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
    }

    @Test
    public void testOpenAddDeviceOverlay() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Test Case 2.1
            // Pre-Condition: User is on the empty Fridge menu (MainActivity with cleared prefs)

            // Steps: 1. Click on the '+' button at the bottom right.
            onView(withId(R.id.btnPlus)).perform(click());

            // Expected Results: 1. A dark overlay menu appears with the 'Add device' button.
            onView(withId(R.id.overlayAddMenu)).check(matches(isDisplayed()));
            onView(withId(R.id.btnAddDevice)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testAccessAvailableDevicesList() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Test Case 2.2
            // Pre-Condition: The 'Add device' overlay is visible.
            onView(withId(R.id.btnPlus)).perform(click());

            // Steps: 1. Click on 'Add device'.
            onView(withId(R.id.btnAddDevice)).perform(click());

            // Expected Results: 1. The "Available devices" menu opens showing a list of devices to add.
            // Check that the AddDeviceActivity is visible by checking for its title
            onView(withId(R.id.tvAvailableDevicesTitle)).check(matches(isDisplayed()));
        }
    }
}
