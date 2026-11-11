package com.example.coen390.fridgefriends.alert;

//ALERT-1.1: Alert rules shared by every sensor
//Threshold values and checks are in Sensor (one line per sensor)
public final class AlertConfig {

    private AlertConfig() {}

    //How long a reading must stay abnormal before sending an alert
    public static final long WAIT_TIME_MS = 2 * 60 * 1000L;
    //Ignore the readings older than 2min ago
    public static final long MAX_READING_AGE_MS = 2 * 60 * 1000L;
}