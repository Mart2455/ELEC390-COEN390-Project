package com.example.coen390.fridgefriends;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.coen390.fridgefriends.view.AddDeviceActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class AvailableDevicesUiTest {

    @Test
    public void testSelectDeviceFromList() {
        // Test Case 2.3
        // Pre-Condition: User is on the Available devices menu (AddDeviceActivity)
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, AddDeviceActivity.class);
        
        try (ActivityScenario<AddDeviceActivity> scenario = ActivityScenario.launch(intent)) {
            // Check that we are on the right screen
            onView(withId(R.id.tvAvailableDevicesTitle)).check(matches(isDisplayed()));

            // Steps: 1. Click on 'Device 1'. (Assuming it's the first button under Available Devices based on XML)
            onView(withId(R.id.btnDevice1)).perform(click());

            // Expected Results: 1. The Fridge setup menu opens for Device 1
            // Check for a View specific to the setup activity, like the "Save Changes" button or title
            onView(withId(R.id.btnSave)).check(matches(isDisplayed()));
            onView(withId(R.id.tvDeviceNameLabel)).check(matches(withText("Device 1")));
        }
    }
}
