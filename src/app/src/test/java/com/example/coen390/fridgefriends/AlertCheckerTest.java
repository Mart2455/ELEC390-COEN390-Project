package com.example.coen390.fridgefriends;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.coen390.fridgefriends.alert.AlertChecker;
import com.example.coen390.fridgefriends.alert.AlertConfig;
import com.example.coen390.fridgefriends.alert.Sensor;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

//ALERT-1.2: checks the alert logic (2-min wait, one alert at a time, devices kept separate)
public class AlertCheckerTest {

    private static final long MINUTE = 60 * 1000L;
    private AlertChecker checker;

    //New checker before every test so the tests don't affect each other
    @Before
    public void setUp() {
        checker = new AlertChecker();
    }

    //SIM-1.3 TEMP-05: temperature goes to 6 °C at minute 10 -> alert at minute 12
    @Test
    public void sustainedWarmingAlertsAfterTwoMinutes() {
        checker.check("Device 1", Sensor.TEMPERATURE, 3.0, 9 * MINUTE, 9 * MINUTE);

        AlertChecker.Result result10 = checker.check("Device 1", Sensor.TEMPERATURE, 6.0, 10 * MINUTE, 10 * MINUTE);
        AlertChecker.Result result11 = checker.check("Device 1", Sensor.TEMPERATURE, 6.0, 11 * MINUTE, 11 * MINUTE);
        AlertChecker.Result result12 = checker.check("Device 1", Sensor.TEMPERATURE, 6.0, 12 * MINUTE, 12 * MINUTE);

        assertEquals(AlertChecker.Result.WAITING, result10);
        assertEquals(AlertChecker.Result.WAITING, result11);
        assertEquals(AlertChecker.Result.ALERT_STARTED, result12);
    }

    //SIM-1.3 TEMP-04: 8 °C for only 2 readings -> no alert
    @Test
    public void shortSpikeIsIgnored() {
        checker.check("Device 1", Sensor.TEMPERATURE, 3.0, 9 * MINUTE, 9 * MINUTE);
        checker.check("Device 1", Sensor.TEMPERATURE, 8.0, 10 * MINUTE, 10 * MINUTE);
        checker.check("Device 1", Sensor.TEMPERATURE, 8.0, 11 * MINUTE, 11 * MINUTE);
        checker.check("Device 1", Sensor.TEMPERATURE, 3.0, 12 * MINUTE, 12 * MINUTE);

        assertFalse(checker.isAlertActive("Device 1", Sensor.TEMPERATURE));
    }
    //Once started, the alert does not start again while it is still on
    @Test
    public void alertStartsOnlyOnce() {
        checker.check("Device 1", Sensor.TEMPERATURE, 6.0, 0, 0);
        checker.check("Device 1", Sensor.TEMPERATURE, 6.0, MINUTE, MINUTE);
        checker.check("Device 1", Sensor.TEMPERATURE, 6.0, 2 * MINUTE, 2 * MINUTE);

        AlertChecker.Result result = checker.check("Device 1", Sensor.TEMPERATURE, 6.0, 3 * MINUTE, 3 * MINUTE);

        assertEquals(AlertChecker.Result.ALERT_ACTIVE, result);
    }

    //Alert ends only at the clear value (3.5), not just below the limit (4.0)
    @Test
    public void alertClearsOnlyAtClearValue() {
        checker.check("Device 1", Sensor.TEMPERATURE, 6.0, 0, 0);
        checker.check("Device 1", Sensor.TEMPERATURE, 6.0, MINUTE, MINUTE);
        checker.check("Device 1", Sensor.TEMPERATURE, 6.0, 2 * MINUTE, 2 * MINUTE);

        AlertChecker.Result gapResult = checker.check("Device 1", Sensor.TEMPERATURE, 3.8, 3 * MINUTE, 3 * MINUTE);
        AlertChecker.Result clearResult = checker.check("Device 1", Sensor.TEMPERATURE, 3.4, 4 * MINUTE, 4 * MINUTE);

        assertEquals(AlertChecker.Result.ALERT_ACTIVE, gapResult);
        assertEquals(AlertChecker.Result.ALERT_CLEARED, clearResult);
    }

    //A missing reading restarts the 2-min wait
    @Test
    public void missingReadingRestartsWait() {
        checker.check("Device 1", Sensor.TEMPERATURE, 6.0, 0, 0);
        checker.check("Device 1", Sensor.TEMPERATURE, 6.0, MINUTE, MINUTE);
        checker.check("Device 1", Sensor.TEMPERATURE, Double.NaN, 2 * MINUTE, 2 * MINUTE);
        checker.check("Device 1", Sensor.TEMPERATURE, 6.0, 3 * MINUTE, 3 * MINUTE);

        assertFalse(checker.isAlertActive("Device 1", Sensor.TEMPERATURE));
    }

    //A missing reading does not end an active alert
    @Test
    public void missingReadingKeepsAlertOn() {
        checker.check("Device 1", Sensor.TEMPERATURE, 6.0, 0, 0);
        checker.check("Device 1", Sensor.TEMPERATURE, 6.0, MINUTE, MINUTE);
        checker.check("Device 1", Sensor.TEMPERATURE, 6.0, 2 * MINUTE, 2 * MINUTE);
        checker.check("Device 1", Sensor.TEMPERATURE, Double.NaN, 3 * MINUTE, 3 * MINUTE);

        assertTrue(checker.isAlertActive("Device 1", Sensor.TEMPERATURE));
    }

    //A reading older than 2 min is ignored
    @Test
    public void outdatedReadingIsIgnored() {
        AlertChecker.Result result = checker.check("Device 1", Sensor.TEMPERATURE, 6.0, 0, 5 * MINUTE);

        assertEquals(AlertChecker.Result.IGNORED, result);
    }

    //Device 1 alerting does not affect Device 2
    @Test
    public void devicesAreSeparate() {
        for (int m = 0; m <= 2; m++) {
            checker.check("Device 1", Sensor.ALCOHOL, 1500, m * MINUTE, m * MINUTE);
            checker.check("Device 2", Sensor.ALCOHOL, 1200, m * MINUTE, m * MINUTE);
        }

        assertTrue(checker.isAlertActive("Device 1", Sensor.ALCOHOL));
        assertFalse(checker.isAlertActive("Device 2", Sensor.ALCOHOL));
    }

    //Removing a device forgets its alerts
    @Test
    public void removeDeviceForgetsAlerts() {
        checker.check("Device 1", Sensor.TEMPERATURE, 6.0, 0, 0);
        checker.check("Device 1", Sensor.TEMPERATURE, 6.0, MINUTE, MINUTE);
        checker.check("Device 1", Sensor.TEMPERATURE, 6.0, 2 * MINUTE, 2 * MINUTE);

        checker.removeDevice("Device 1");

        assertFalse(checker.isAlertActive("Device 1", Sensor.TEMPERATURE));
    }

    //Sensors checked match what the device tracks, temperature always included
    @Test
    public void sensorsToCheckMatchTracking() {
        assertEquals(Arrays.asList(Sensor.TEMPERATURE),
                AlertConfig.getSensorsToCheck(false, false, false));
        assertEquals(Arrays.asList(Sensor.TEMPERATURE, Sensor.HUMIDITY),
                AlertConfig.getSensorsToCheck(false, true, false));
    }

    //ALERT-1.4: after going back to normal, a new problem sends a new alert
    @Test
    public void newAlertAfterBackToNormal() {
        //first problem: alert starts at minute 2
        checker.check("Device 1", Sensor.TEMPERATURE, 6.0, 0, 0);
        checker.check("Device 1", Sensor.TEMPERATURE, 6.0, MINUTE, MINUTE);
        AlertChecker.Result first = checker.check("Device 1", Sensor.TEMPERATURE, 6.0, 2 * MINUTE, 2 * MINUTE);

        //back to normal
        AlertChecker.Result cleared = checker.check("Device 1", Sensor.TEMPERATURE, 3.0, 3 * MINUTE, 3 * MINUTE);

        //second problem: needs 2 new minutes, then alerts again
        AlertChecker.Result waiting = checker.check("Device 1", Sensor.TEMPERATURE, 6.0, 4 * MINUTE, 4 * MINUTE);
        checker.check("Device 1", Sensor.TEMPERATURE, 6.0, 5 * MINUTE, 5 * MINUTE);
        AlertChecker.Result second = checker.check("Device 1", Sensor.TEMPERATURE, 6.0, 6 * MINUTE, 6 * MINUTE);

        assertEquals(AlertChecker.Result.ALERT_STARTED, first);
        assertEquals(AlertChecker.Result.ALERT_CLEARED, cleared);
        assertEquals(AlertChecker.Result.WAITING, waiting);
        assertEquals(AlertChecker.Result.ALERT_STARTED, second);
    }

    //ALERT-1.4: staying in the gap zone (between 3.5 and 4.0) does not allow a new alert
    @Test
    public void noNewAlertWithoutGoingBackToNormal() {
        checker.check("Device 1", Sensor.TEMPERATURE, 6.0, 0, 0);
        checker.check("Device 1", Sensor.TEMPERATURE, 6.0, MINUTE, MINUTE);
        checker.check("Device 1", Sensor.TEMPERATURE, 6.0, 2 * MINUTE, 2 * MINUTE);

        //dips to 3.8 (not back to normal), then goes bad again
        checker.check("Device 1", Sensor.TEMPERATURE, 3.8, 3 * MINUTE, 3 * MINUTE);
        AlertChecker.Result result4 = checker.check("Device 1", Sensor.TEMPERATURE, 6.0, 4 * MINUTE, 4 * MINUTE);
        AlertChecker.Result result6 = checker.check("Device 1", Sensor.TEMPERATURE, 6.0, 6 * MINUTE, 6 * MINUTE);

        assertEquals(AlertChecker.Result.ALERT_ACTIVE, result4);
        assertEquals(AlertChecker.Result.ALERT_ACTIVE, result6);
    }
}