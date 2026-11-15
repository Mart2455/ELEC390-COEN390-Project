package com.example.coen390.fridgefriends;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.example.coen390.fridgefriends.alert.Sensor;
import org.junit.Test;

//unit test for the alert condition
public class SensorTest {

    //Temperature (SIM-1.3 boundary test)
    @Test
    public void temperatureLimitIsStrict() {
        assertFalse(Sensor.TEMPERATURE.isAbnormal(4.0));
        assertTrue(Sensor.TEMPERATURE.isAbnormal(4.1));
    }

    //Between limit and clear nothing changes (no alert spam near the limit)
    @Test
    public void gapBetweenLimitAndClear() {
        assertFalse(Sensor.TEMPERATURE.isAbnormal(3.8));
        assertFalse(Sensor.TEMPERATURE.isBackToNormal(3.8));
        assertTrue(Sensor.TEMPERATURE.isBackToNormal(3.5));
    }

    //Raspberry test: overripe readings are abnormal, fresh readings are normal
    @Test
    public void raspberryTestValues() {
        //Overripe medians
        assertTrue(Sensor.TVOC.isAbnormal(1055));
        assertTrue(Sensor.ECO2.isAbnormal(7327));
        assertTrue(Sensor.ALCOHOL.isAbnormal(1537));
        //Fresh maximums
        assertFalse(Sensor.TVOC.isAbnormal(660));
        assertFalse(Sensor.ECO2.isAbnormal(4738));
        assertFalse(Sensor.ALCOHOL.isAbnormal(1284));
    }

    //Gas resistance alerts when it goes down and not up
    @Test
    public void gasResistanceAlertsBelowLimit() {
        assertTrue(Sensor.GAS_RESISTANCE.isAbnormal(45));
        assertFalse(Sensor.GAS_RESISTANCE.isAbnormal(55));
        assertTrue(Sensor.GAS_RESISTANCE.isBackToNormal(60));
        assertFalse(Sensor.GAS_RESISTANCE.isBackToNormal(57));
    }

    //Missing double readings (temperature, humidity, gas resistance) arrive as NaN
    @Test
    public void missingReadingsAreIgnored() {
        for (Sensor sensor : Sensor.values()) {
            assertFalse(sensor.isValid(Double.NaN));
            assertFalse(sensor.isAbnormal(Double.NaN));
            assertFalse(sensor.isBackToNormal(Double.NaN));
        }
    }

    //Missing int readings (eCO2, TVOC, MQ-3) arrive as -1
    @Test
    public void missingIntReadingsAreIgnored() {
        Sensor[] intSensors = {Sensor.ECO2, Sensor.TVOC, Sensor.ALCOHOL};
        for (Sensor sensor : intSensors) {
            assertFalse(sensor.isValid(-1));
            assertFalse(sensor.isAbnormal(-1));
            assertFalse(sensor.isBackToNormal(-1));
        }
    }

    //Readings outside the sensor range are ignored
    @Test
    public void outOfRangeReadingsAreIgnored() {
        assertFalse(Sensor.TEMPERATURE.isAbnormal(120));
        assertFalse(Sensor.HUMIDITY.isAbnormal(150));
        assertFalse(Sensor.ALCOHOL.isAbnormal(5000));
    }

    //0 and negative temperatures are real readings, not missing ones
    @Test
    public void zeroAndNegativeTemperaturesAreValid() {
        assertTrue(Sensor.TEMPERATURE.isValid(0));
        assertTrue(Sensor.TEMPERATURE.isValid(-1));
        assertTrue(Sensor.TVOC.isValid(0));
    }

    //Every clear value is on the normal side of its limit, so every alert can end
    @Test
    public void everyAlertCanEnd() {
        for (Sensor sensor : Sensor.values()) {
            if (sensor.getAlertIfAbove())
                assertTrue(sensor.getClear() <= sensor.getLimit());
            else
                assertTrue(sensor.getClear() >= sensor.getLimit());
        }
    }
}