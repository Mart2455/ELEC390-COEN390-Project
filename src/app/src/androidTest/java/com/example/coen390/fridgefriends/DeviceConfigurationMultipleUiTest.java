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
import static androidx.test.espresso.matcher.ViewMatchers.isNotSelected;
import static androidx.test.espresso.matcher.ViewMatchers.isSelected;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class DeviceConfigurationMultipleUiTest {

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
    public void testSelectMultipleTrackingParameters() {
        // Test Case 3.2
        Intent intent = new Intent(context, FridgeSetupActivity.class);
        intent.putExtra("device_name", "Device 1");

        try (ActivityScenario<FridgeSetupActivity> scenario = ActivityScenario.launch(intent)) {
            // Check pre-condition: User is on the Fridge setup menu.
            onView(withId(R.id.btnSave)).check(matches(isDisplayed()));

            // Step 1: Click on 'Moisture'
            onView(withId(R.id.btnMoisture)).perform(click());

            // Step 2: Click on 'Fermentation'
            onView(withId(R.id.btnFermentation)).perform(click());

            // Expected Result 1: Both Moisture and Fermentation buttons highlight
            onView(withId(R.id.btnMoisture)).check(matches(isSelected()));
            onView(withId(R.id.btnFermentation)).check(matches(isSelected()));
        }
    }

    @Test
    public void testToggleTrackingParameterOff() {
        // Test Case 3.3
        Intent intent = new Intent(context, FridgeSetupActivity.class);
        intent.putExtra("device_name", "Device 1");

        try (ActivityScenario<FridgeSetupActivity> scenario = ActivityScenario.launch(intent)) {
            // Freshness is selected by default in new device setup
            onView(withId(R.id.btnFreshness)).check(matches(isSelected()));

            // Step 1: Click on 'Freshness' again.
            onView(withId(R.id.btnFreshness)).perform(click());

            // Expected Result 1: The Freshness button returns to a grey/unselected state
            onView(withId(R.id.btnFreshness)).check(matches(isNotSelected()));
        }
    }
}
