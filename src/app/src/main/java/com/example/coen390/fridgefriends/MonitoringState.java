package com.example.coen390.fridgefriends;

import android.os.Looper;
import com.example.coen390.fridgefriends.network.ESP32Receiver;
import java.util.HashSet;
import java.util.Set;

/** Shared, process-local readings. Subscribe and publish only on the main thread. */
public final class MonitoringState
{
    public interface Listener { void onChanged(Snapshot state); }

    public static final class Snapshot
    {
        public final boolean attempted, connected, hasReadings;
        public final double humidity;
        public final int tvoc, eco2, mq3Raw;

        public final double temperature;

        private Snapshot(boolean attempted, boolean connected, boolean hasReadings,  double humidity, int tvoc, int eco2, int mq3Raw, double temperature)
        {
            this.attempted = attempted;
            this.connected = connected;
            this.hasReadings = hasReadings;
            this.humidity = humidity;
            this.tvoc = tvoc;
            this.eco2 = eco2;
            this.mq3Raw = mq3Raw;
            this.temperature = temperature;
        }
    }

    private static final Set<Listener> listeners = new HashSet<>();
    private static Snapshot latest = new Snapshot(false, false, false,
            Double.NaN, -1, -1, -1, -1);

    private MonitoringState()
    {

    }

    public static void observe(Listener listener)
    {
        listeners.add(listener);
        listener.onChanged(latest);
    }

    public static void removeObserver(Listener listener)
    {
        listeners.remove(listener);
    }

    public static void publish(boolean success)
    {
        if (Looper.myLooper() != Looper.getMainLooper())
        {
            throw new IllegalStateException("Publish monitoring state on the main thread");
        }
        latest = success ? new Snapshot(true, true, true, ESP32Receiver.getHumidity(), ESP32Receiver.getTvoc(), ESP32Receiver.getEco2(), ESP32Receiver.getMq3Raw(), ESP32Receiver.getTemperature())
                : new Snapshot(true, false, latest.hasReadings, latest.humidity,
                        latest.tvoc, latest.eco2, latest.mq3Raw, latest.temperature);
        for (Listener listener : new HashSet<>(listeners))
        {
            listener.onChanged(latest);
        }
    }
}
