package com.example.coen390.fridgefriends;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.coen390.fridgefriends.network.ESP32Receiver;
import com.example.coen390.fridgefriends.view.DeviceTrackerActivity;
import com.example.coen390.fridgefriends.view.MainActivity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

/** Tests inject completed requests: no ESP32 or network connection is needed. */
@RunWith(AndroidJUnit4.class)
public class MonitoringUiTest {
    private Context context;
    private Map<String, ?> savedPreferences;
    private MonitoringState.Snapshot savedState;
    private final List<MonitoringState.Listener> observers = new ArrayList<>();
    private final Map<String, Object> savedReadings = new java.util.HashMap<>();

    private void onMain(Runnable action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }

    @Before
    public void setUp() throws Exception
    {

        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        context.stopService(new Intent(context, SensorMonitoringService.class));

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();


        SharedPreferences prefs = context.getSharedPreferences("DevicePrefs", Context.MODE_PRIVATE);
        savedPreferences = prefs.getAll();
        // Prevent MainActivity from starting real HTTP polling during UI tests.
        assertTrue(prefs.edit().clear().commit());
        for (String name : new String[]{"humidity", "tvoc", "eco2", "mq3Raw"})
        {
            Field field = ESP32Receiver.class.getDeclaredField(name);
            field.setAccessible(true);
            savedReadings.put(name, field.get(null));
        }


        Field latest = MonitoringState.class.getDeclaredField("latest");
        latest.setAccessible(true);
        savedState = (MonitoringState.Snapshot) latest.get(null);
        Constructor<MonitoringState.Snapshot> constructor = MonitoringState.Snapshot.class
                .getDeclaredConstructor(boolean.class, boolean.class, boolean.class,
                        double.class, int.class, int.class, int.class);
        constructor.setAccessible(true);
        MonitoringState.Snapshot initial = constructor.newInstance(false, false, false,
                Double.NaN, -1, -1, -1);
        onMain(() -> {
            try { latest.set(null, initial); }
            catch (IllegalAccessException e) { throw new AssertionError(e); }
        });
    }

    @After
    public void tearDown() throws Exception {
        onMain(() -> {
            for (MonitoringState.Listener observer : observers) MonitoringState.removeObserver(observer);
        });
        Field latest = MonitoringState.class.getDeclaredField("latest");
        latest.setAccessible(true);
        onMain(() -> {
            try { latest.set(null, savedState); }
            catch (IllegalAccessException e) { throw new AssertionError(e); }
        });
        for (Map.Entry<String, Object> entry : savedReadings.entrySet()) {
            Field field = ESP32Receiver.class.getDeclaredField(entry.getKey());
            field.setAccessible(true);
            field.set(null, entry.getValue());
        }
        SharedPreferences.Editor editor = context.getSharedPreferences("DevicePrefs", Context.MODE_PRIVATE)
                .edit().clear();
        for (Map.Entry<String, ?> entry : savedPreferences.entrySet()) {
            Object value = entry.getValue();
            String key = entry.getKey();
            if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
            else if (value instanceof Integer) editor.putInt(key, (Integer) value);
            else if (value instanceof Long) editor.putLong(key, (Long) value);
            else if (value instanceof Float) editor.putFloat(key, (Float) value);
            else if (value instanceof String) editor.putString(key, (String) value);
            else if (value instanceof Set) editor.putStringSet(key, (Set<String>) value);
        }
        assertTrue(editor.commit());
    }

    private void success(double humidity, int tvoc) {
        onMain(() -> {
            try
            {
                String[] names = {"humidity", "tvoc", "eco2", "mq3Raw"};
                Object[] values = {humidity, tvoc, 800, 1500};

                for (int i = 0; i < names.length; i++)
                {
                    Field field = ESP32Receiver.class.getDeclaredField(names[i]);
                    field.setAccessible(true);
                    field.set(null, values[i]);
                }
                MonitoringState.publish(true);
            }
            catch (ReflectiveOperationException e)
            {
                throw new AssertionError(e);
            }
        });
    }

    private ActivityScenario<DeviceTrackerActivity> openMoistureTracker() {
        return ActivityScenario.launch(new Intent(context, DeviceTrackerActivity.class)
                .putExtra("device_name", "Device 1")
                .putExtra("track_moisture", true));
    }

    @Test
    public void failurePreservesReadingsAndRecoveryReplacesThem()
    {
        List<MonitoringState.Snapshot> results = new ArrayList<>();

        MonitoringState.Listener listener = results::add;

        observers.add(listener);

        onMain(() -> MonitoringState.observe(listener));

        assertFalse(results.get(0).attempted);

        onMain(() -> MonitoringState.publish(false));

        assertFalse(results.get(1).hasReadings);

        success(65.5, 120);

        onMain(() -> MonitoringState.publish(false));

        MonitoringState.Snapshot failure = results.get(3);

        assertFalse(failure.connected);

        assertTrue(failure.hasReadings);

        assertEquals(65.5, failure.humidity, 0.001);

        success(42.0, 80);

        assertTrue(results.get(4).connected);

        assertEquals(42.0, results.get(4).humidity, 0.001);
    }

    @Test
    public void removedObserverStopsReceivingUpdatesAndNewObserverGetsLatest()
    {
        List<MonitoringState.Snapshot> first = new ArrayList<>();

        MonitoringState.Listener listener = first::add;

        onMain(() -> { MonitoringState.observe(listener); MonitoringState.removeObserver(listener); });
        success(42.0, 80);
        assertEquals(1, first.size());
        List<MonitoringState.Snapshot> second = new ArrayList<>();
        MonitoringState.Listener next = second::add;
        observers.add(next);
        onMain(() -> MonitoringState.observe(next));
        assertEquals(42.0, second.get(0).humidity, 0.001);
    }

    @Test
    public void trackerShowsFirstFailureWithoutDefaultZeroReadings() {
        try (ActivityScenario<DeviceTrackerActivity> scenario = openMoistureTracker()) {
            onView(withId(R.id.tvStatusLabel)).check(matches(withText("Connecting...")));
            onMain(() -> MonitoringState.publish(false));
            onView(withId(R.id.tvSensorValue)).check(matches(withText("No readings available")));
            onView(withId(R.id.tvStatusLabel)).check(matches(
                    withText("Unable to connect — no readings available")));
            onView(withId(R.id.tvStatusLabel)).check((view, error) -> {
                if (error != null) throw error;
                assertEquals(Color.RED, ((TextView) view).getCurrentTextColor());
            });
        }
    }

    @Test
    public void trackerKeepsStaleReadingsThenClearsWarningOnRecovery() {
        try (ActivityScenario<DeviceTrackerActivity> scenario = openMoistureTracker()) {
            success(65.5, 120);
            onMain(() -> MonitoringState.publish(false));
            onView(withId(R.id.tvSensorValue)).check(matches(withText("65.5 %")));
            onView(withId(R.id.tvStatusLabel)).check(matches(withText("Connection lost — readings outdated")));
            success(42.0, 80);
            onView(withId(R.id.tvSensorValue)).check(matches(withText("42.0 %")));
            onView(withId(R.id.tvStatusLabel)).check(matches(withText("Connected")));
            scenario.recreate();
            onView(withId(R.id.tvSensorValue)).check(matches(withText("42.0 %")));
        }
    }

    @Test
    public void dashboardShowsFailureAndRecoveryWithoutChangingTrackingPreference()
    {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class))
        {
            SharedPreferences prefs = context.getSharedPreferences("DevicePrefs", Context.MODE_PRIVATE);


            assertTrue(prefs.edit().putBoolean("Device 1_added", true)
                    .putBoolean("Device 1_track_moisture", true).commit());


            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);


            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);


            success(65.5, 120);
            onMain(() -> MonitoringState.publish(false));
            onView(withId(R.id.tvTrackingMain)).check(matches(withText("Tracking: Moisture")));
            onView(withId(R.id.tvConnectionStatusMain)).check(matches(
                    withText("Connection lost — readings outdated")));
            success(42.0, 80);
            onView(withId(R.id.tvConnectionStatusMain)).check(matches(withText("Connected")));
        }
    }

    @Test
    public void missingSensorReadingWarnsOnlyForSelectedMeasurement() {
        try (ActivityScenario<DeviceTrackerActivity> scenario = openMoistureTracker()) {
            success(Double.NaN, 120);
            onView(withId(R.id.tvSensorValue)).check(matches(withText("Reading unavailable")));
            onView(withId(R.id.tvStatusLabel)).check(matches(withText("Sensor reading unavailable")));
            // Missing TVOC must not mark a valid moisture reading as disconnected.
            success(42.0, -1);
            onView(withId(R.id.tvStatusLabel)).check(matches(withText("Connected")));
        }
    }
}
