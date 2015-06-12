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
    final static float FLOOR_H = 2.9f; // meters

    long tprevAccNanos = System.currentTimeMillis();

    private long hmeterEnabledT0 = 0;
    double hmeter = 0;
    private double hmeterV = 0;
    boolean vfastReached, vslowReached;
    //double accMin, accMax;
    long prevSecond, prevMeters;
    List<Double> accList = new ArrayList<>();
    List<Double> speedList = new ArrayList<>();
    List<Integer> secChanges = new ArrayList<>();
    List<Integer> meterChanges = new ArrayList<>();

    boolean avgEnabled = false;
    Avg avgGravity = new Avg();

    Sounds sounds;

    public AccListener(Sounds sounds) {
        this.sounds = sounds;
    }

    void reset() {
        avgGravity.reset();
        avgEnabled = true;
        hmeterEnabledT0 = 0; // disabled
    }

    void userStop(){
        hmeter = 0;
        hmeterEnabledT0 = 0; // stop on swipe
    }

    abstract void avgText(String txt);
    abstract void setShiftText(String line1, String line2);
    abstract void postInvalidate();

    double getFloorDelta(){
        return hmeter / FLOOR_H;
    }

    void clickAndStop(){
        if( hmeterEnabledT0 != 0 ) {
            sounds.sound();
        }
        hmeterEnabledT0 = 0;
    }

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
            long dtNanos = sensorEvent.timestamp - tprevAccNanos;
            double acc = accAll - avgGravity.avg(); // - 0.06;
//            accMin = Math.min(acc, accMin);
//            accMax = Math.max(acc, accMax);
            hmeterV += acc * dtNanos / 1_000_000_000;
            if (accList.size() < 1500) {
                accList.add(acc);
                speedList.add(hmeterV);
                postInvalidate();
            }
            autoStopLogic(sec);
            hmeter += hmeterV * dtNanos / 1_000_000_000;
            long newHmeter = (int) Math.abs(hmeter);
            boolean meterChange = prevMeters != newHmeter;
            prevMeters = newHmeter;

            if (secChange) secChanges.add(accList.size());
            if (meterChange) meterChanges.add(accList.size());

            //String shiftStr = String.format("Shift %.2f m, acc %.2f .. %.2f", hmeter, accMin, accMax);
            String shiftStr = String.format("Shift %.2f m, speed %.2f m/s", hmeter, hmeterV);
            setShiftText(
                    shiftStr,
                    String.format("sec=%d, dtMs=%d", sec, dtNanos / 1_000_000));
        }

        tprevAccNanos = sensorEvent.timestamp;
    }

    private void autoStopLogic(long sec) {
        if (sec > 20) {
            clickAndStop(); // disabled
        }
        if( Math.abs(hmeterV)>0.5 ){
            vfastReached = true;
        }
        if( vfastReached ){
            if( Math.abs(hmeterV)<0.03 ){
                vslowReached = true;
            }
        }
        if( vslowReached && Math.abs(hmeterV)>0.06 ){
            clickAndStop();
            // we had fast motion and then nearly stopped (lift came to its destination),
            // we have to stop tracking if we abruptly move phone by hand
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    public void start() {
        avgEnabled = false;
        hmeter = 0;
        hmeterV = 0;
        vfastReached = false;
        vslowReached = false;
//        accMin = 100;
//        accMax = -100;
        prevSecond = 0;
        prevMeters = 0;
        accList.clear();
        speedList.clear();
        secChanges.clear();
        meterChanges.clear();
        hmeterEnabledT0 = System.currentTimeMillis();
    }
}
