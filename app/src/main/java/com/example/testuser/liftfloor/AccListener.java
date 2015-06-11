package com.example.testuser.liftfloor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by denny on 6/11/15.
 */
abstract class AccListener implements SensorEventListener {
    private final SurfaceView graph;

    long tprevAccNanos = System.currentTimeMillis();

    long hmeterEnabledT0 = 0;
    double hmeter = 0;
    double hmeterV = 0;
    double accMin, accMax;
    long prevSecond, prevMeters;
    List<Double> accList = new ArrayList<>();
    List<Integer> secChanges = new ArrayList<>();
    List<Integer> meterChanges = new ArrayList<>();

    boolean avgEnabled = false;
    Avg avgGravity = new Avg();

    public AccListener(SurfaceView graph) {
        this.graph = graph;
    }

    void reset() {
        avgGravity.reset();
        avgEnabled = true;
        hmeterEnabledT0 = 0; // disabled
    }

    abstract void avgText(String txt);

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];
        double accAll = Math.sqrt(x * x + y * y + z * z);

        if (avgEnabled) {
            avgGravity.add((float) accAll);
            avgText(String.format("avg=%.3f %d", avgGravity.avg(), avgGravity.count));
        }

        if (hmeterEnabledT0 > 0) {
            long sec = (System.currentTimeMillis() - hmeterEnabledT0) / 1000;
            boolean secChange = prevSecond != sec;
            prevSecond = sec;
            if (sec > 20) {
                hmeterEnabledT0 = 0; // disabled
            }
            long dtNanos = sensorEvent.timestamp - tprevAccNanos;
            double acc = accAll - avgGravity.avg();
            accMin = Math.min(acc, accMin);
            accMax = Math.max(acc, accMax);
            if (accList.size() < 1500) {
                accList.add(acc);
                graph.postInvalidate();
            }
            hmeterV += acc * dtNanos / 1_000_000_000;
            hmeter += hmeterV * dtNanos / 1_000_000_000;
            long newHmeter = (int) Math.abs(hmeter);
            boolean meterChange = prevMeters != newHmeter;
            prevMeters = newHmeter;

            if (secChange) secChanges.add(accList.size());
            if (meterChange) meterChanges.add(accList.size());

            setShiftText(
                    String.format("Shift %.2f m, acc %.2f .. %.2f", hmeter, accMin, accMax),
                    String.format("sec=%d, dtMs=%d", sec, dtNanos / 1_000_000));
        }

        tprevAccNanos = sensorEvent.timestamp;
    }

    protected abstract void setShiftText(String line1, String line2);

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    public void start() {
        avgEnabled = false;
        hmeter = 0;
        hmeterV = 0;
        accMin = 100;
        accMax = -100;
        prevSecond = 0;
        prevMeters = 0;
        accList.clear();
        secChanges.clear();
        meterChanges.clear();
        hmeterEnabledT0 = System.currentTimeMillis();
    }
}
