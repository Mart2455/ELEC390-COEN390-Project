package com.example.coen390.fridgefriends.alert;
import java.util.HashMap;
import java.util.Map;
public class AlertChecker {

    //What happened with the reading that was just checked
    public enum Result {
        //Missing, Invalid or too old
        IGNORED,
        NORMAL,
        //abnormal, but not for 2 min yet
        WAITING,
        //abnormal for 2 min, the alert just started
        ALERT_STARTED,
        ALERT_ACTIVE,
        //alert ended
        ALERT_CLEARED
    }

    //State of a sensor on a device
    private static class SensorState {
        boolean alertActive = false;
        //tracks time of the first abnormal reading
        long abnormalSince = -1;
    }

    //Key = device name + sensor
    private final Map<String, SensorState> states = new HashMap<>();

    public Result check(String deviceName, Sensor sensor, double reading, long readingTime, long now){
     SensorState state = getState(deviceName, sensor);

        //if a reading is missing, invalid or too old, ignore it and restart the wait
        boolean tooOld = now - readingTime > AlertConfig.MAX_READING_AGE_MS;
        if (!sensor.isValid(reading) || tooOld) {
            state.abnormalSince = -1;
            return Result.IGNORED;
        }

        //if an is active, clear alert if reading is back to normal
        if (state.alertActive) {
            if (sensor.isBackToNormal(reading)) {
                state.alertActive = false;
                state.abnormalSince = -1;
                return Result.ALERT_CLEARED;
            }
            return Result.ALERT_ACTIVE;
        }

        //if a reading is normal, restart the wait
        if (!sensor.isAbnormal(reading)) {
            state.abnormalSince = -1;
            return Result.NORMAL;
        }

        //if abnormal, start the count and check time state
        if (state.abnormalSince == -1)
            state.abnormalSince = readingTime;
        if (readingTime - state.abnormalSince >= AlertConfig.WAIT_TIME_MS) {
            state.alertActive = true;
            return Result.ALERT_STARTED;
        }
        return Result.WAITING;
    }

    //check if an alert is already active for a sensor
    public boolean isAlertActive(String deviceName, Sensor sensor) {
        return getState(deviceName, sensor).alertActive;
    }

    //delete memory of a device when its removed
    public void removeDevice(String deviceName) {
        for (Sensor sensor : Sensor.values())
            states.remove(deviceName + "_" + sensor.name());
    }

    //Finds the state for this device/sensors and create it
    private SensorState getState(String deviceName, Sensor sensor) {
        String key = deviceName + "_" + sensor.name();
        SensorState state = states.get(key);
        if (state == null) {
            state = new SensorState();
            states.put(key, state);
        }
        return state;
    }
}
