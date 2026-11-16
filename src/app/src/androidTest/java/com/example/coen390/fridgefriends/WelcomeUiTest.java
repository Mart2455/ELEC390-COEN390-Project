package com.example.coen390.fridgefriends;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.coen390.fridgefriends.view.WelcomeActivity;

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
public class WelcomeUiTest {

    private Context context;

    @Before
    public void setUp() {
        // Pre-Condition: Application launched for the first time.
        // We ensure preferences are cleared so the app behaves as a fresh installation.
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
    public void testWelcomeScreenNavigation() {
        // Launch WelcomeActivity
        try (ActivityScenario<WelcomeActivity> scenario = ActivityScenario.launch(WelcomeActivity.class)) {
            // Steps: 1. Click on the 'YES!' button on the Welcome screen.
            onView(withId(R.id.btnYes)).perform(click());

            // Expected Results: 1. The Welcome screen closes and the empty Fridge menu opens displaying "Please connect your device"
            onView(withText(R.string.please_connect_your_device)).check(matches(isDisplayed()));
        }
    }
}
