package com.example.coen390.fridgefriends.alert;

//ALL THE VALUES HERE ARE FROM THE RASPBERRY TEST
//DONT FORGET TO CHANGE THE VALUES WNV WE FIGURE OUT THE RIGHT VALUE FOR EACH PARRAMTER
public enum Sensor {
    //name, unit, min, max, alert if above, limit, clear
    TEMPERATURE("Temperature", "°C", -40, 85, true, 4.0, 3.5),
    HUMIDITY("Humidity", "%", 0, 100, true, 85, 80),
    GAS_RESISTANCE("Gas resistance", "kΩ", 0, Double.MAX_VALUE, false, 50, 60),
    ECO2("eCO2", "ppm", 400, 8192, true, 7000, 5000),
    TVOC("TVOC", "ppb", 0, 1187, true, 1000, 700),
    ALCOHOL("Alcohol (MQ-3)", "raw", 0, 4095, true, 1400, 1370);

    private final String sensorName;
    private final String unit;
    private final double minValue;
    private final double maxValue;
    private final boolean alertIfAbove;
    private final double limit;
    private final double clear;

    Sensor(String sensorName, String unit, double minValue, double maxValue, boolean alertIfAbove, double limit, double clear){        this.sensorName = sensorName;
        this.unit = unit;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.alertIfAbove = alertIfAbove;
        this.limit = limit;
        this.clear = clear;
    }
    public String getSensorName() {
        return sensorName;
    }
    public String getUnit() {
        return unit;
    }
    public double getMinValue() {
        return minValue;
    }
    public double getMaxValue() {
        return maxValue;
    }
    public boolean getAlertIfAbove() {
        return alertIfAbove;
    }
    public double getLimit() {
        return limit;
    }

    public double getClear() {
        return clear;
    }

    //Ignores missing readings like NaN or -1 and readings outside the sensor range
    public boolean isValid(double reading) {
        return !Double.isNaN(reading) && reading >= minValue && reading <= maxValue;
    }

    //Checks if a reading shows that the food might be going bad
    public boolean isAbnormal(double reading) {
        if (!isValid(reading))
            return false;
        if (alertIfAbove)
            return reading > limit;
        return reading < limit;
    }

    //Reading went back to a safe level, so the active alert can stop
    public boolean isBackToNormal(double reading) {
        if (!isValid(reading))
            return false;
        if (alertIfAbove)
            return reading <= clear;
        return reading >= clear;
    }
}