package com.example.coen390.fridgefriends.alert;
import java.util.ArrayList;
import java.util.List;

//Threshold values and checks are in Sensor (one line per sensor)
public final class AlertConfig {

    private AlertConfig() {}

    //How long a reading must stay abnormal before sending an alert
    public static final long WAIT_TIME_MS = 2 * 60 * 1000L;
    //Ignore the readings older than 2min ago
    public static final long MAX_READING_AGE_MS = 2 * 60 * 1000L;

    //which sensor to check for the selected device
    public static List<Sensor> getSensorsToCheck(boolean trackFreshness, boolean trackMoisture, boolean trackFermentation) {
        List<Sensor> sensors = new ArrayList<>();
        sensors.add(Sensor.TEMPERATURE);
        if (trackFreshness) {
            sensors.add(Sensor.TVOC);
            sensors.add(Sensor.GAS_RESISTANCE);
        }
        if (trackMoisture)
            sensors.add(Sensor.HUMIDITY);
        if (trackFermentation) {
            sensors.add(Sensor.ECO2);
            sensors.add(Sensor.ALCOHOL);
        }
        return sensors;
    }

}