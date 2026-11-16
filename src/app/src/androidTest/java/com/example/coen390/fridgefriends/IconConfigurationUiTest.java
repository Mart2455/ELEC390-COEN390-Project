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
public class IconConfigurationUiTest {

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
    public void testOpenIconSelectionMenu() {
        // Test Case 5.1
        // Pre-Condition: User is on the specific device tracker menu (Setup Menu in this case).
        Intent intent = new Intent(context, FridgeSetupActivity.class);
        intent.putExtra("device_name", "Device 1");

        try (ActivityScenario<FridgeSetupActivity> scenario = ActivityScenario.launch(intent)) {
            // Steps: 1. Click on 'Edit Icon'.
            onView(withId(R.id.tvEditIcon)).perform(click());

            // Expected Results: 1. The Icon selection menu opens showing Available icons.
            onView(withId(R.id.tvAvailableIconsTitle)).check(matches(isDisplayed()));
            onView(withText("Available icons")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testAssignFruitsIcon() {
        // Test Case 5.2
        // Pre-Condition: User is on the specific device tracker menu and clicks 'Edit Icon' to go to Icon selection menu.
        Intent intent = new Intent(context, FridgeSetupActivity.class);
        intent.putExtra("device_name", "Device 1");

        try (ActivityScenario<FridgeSetupActivity> scenario = ActivityScenario.launch(intent)) {
            // Navigate to Icon Selection Menu
            onView(withId(R.id.tvEditIcon)).perform(click());
            onView(withId(R.id.tvAvailableIconsTitle)).check(matches(isDisplayed()));

            // Steps: 1. Click on the 'Fruits' icon.
            onView(withId(R.id.optionFruits)).perform(click());

            // Expected Results: 1. The app returns to the tracker menu (setup menu) and updates the device's icon to Fruits.
            // Check that we returned to the Setup screen by verifying the Edit Icon button is visible again
            onView(withId(R.id.tvEditIcon)).check(matches(isDisplayed()));
            // At this point, the icon would be updated (which we could verify if we tested the ImageView drawable, but checking visibility of Setup elements confirms we returned)
            onView(withId(R.id.btnSave)).check(matches(isDisplayed()));
        }
    }
}
