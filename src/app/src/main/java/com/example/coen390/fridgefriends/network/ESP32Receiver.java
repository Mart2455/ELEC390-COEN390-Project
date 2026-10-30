
package com.example.coen390.fridgefriends.network;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

// For the esp32 communication using HTTP
public class ESP32Receiver
{

    private static final String TAG = "ESP32_DATA";

    private static final String ESP32_URL =
            "http://172.20.10.6/sensors";

    // Sensor readings
    private static double temperature;
    private static double humidity;
    private static double pressure;
    private static double gasResistance;

    private static int eco2;
    private static int tvoc;
    private static int mq3Raw;

    public static void receiveData()
    {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try
            {
                URL url = new URL(ESP32_URL);

                connection = (HttpURLConnection)
                        url.openConnection();

                connection.setRequestMethod("GET"); // get method
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream()
                                )
                        );

                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null)
                {
                    response.append(line);
                }

                reader.close();

                JSONObject data =
                        new JSONObject(response.toString());

                // Store sensor readings
                temperature = data.optDouble("temperature", Double.NaN);
                humidity = data.optDouble("humidity", Double.NaN);
                pressure = data.optDouble("pressure", Double.NaN);
                gasResistance = data.optDouble("gasResistance", Double.NaN);

                eco2 = data.optInt("eco2", -1);
                tvoc = data.optInt("tvoc", -1);
                mq3Raw = data.optInt("mq3Raw", -1);


                Log.d(TAG, "Temperature: " + temperature + " C");
                Log.d(TAG, "Humidity: " + humidity + " %");
                Log.d(TAG, "Pressure: " + pressure + " hPa");
                Log.d(TAG, "Gas Resistance: " + gasResistance + " kOhms");
                Log.d(TAG, "eCO2: " + eco2 + " ppm");
                Log.d(TAG, "TVOC: " + tvoc + " ppb");
                Log.d(TAG, "MQ-3: " + mq3Raw);

            }
            catch (Exception e)
            {

                Log.e(TAG, "Failed to receive ESP32 data", e);

            }
            finally
            {
                if (connection != null)
                {
                    connection.disconnect();
                }
            }

        }).start();
    }

    // ==============================
    // GETTERS
    // ==============================

    public static double getTemperature()
    {
        return temperature;
    }

    public static double getHumidity()
    {
        return humidity;
    }

    public static double getPressure()
    {
        return pressure;
    }

    public static double getGasResistance()
    {
        return gasResistance;
    }

    public static int getEco2()
    {
        return eco2;
    }

    public static int getTvoc()
    {
        return tvoc;
    }

    public static int getMq3Raw()
    {
        return mq3Raw;
    }
}
