package com.example.testuser.liftfloor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity {
    SensorManager senSensorManager;

    Sensor senAccelerometer;
    SensorEventListener accListener;
    long tprevAcc=System.currentTimeMillis();

    double hmeter = 0;
    double hmeterV = 0;
    boolean avgEnabled=false;
    boolean hmeterEnabled=false;
    Avg avgGravity = new Avg();

    class Avg{
        int count=0;
        double sum=0;
        void add(float v){
            sum += v;
            count++;
        }
        double avg(){
            if( count < 1 ){
                return 0;
            }
            return sum / count;
        }
        void reset(){
            count=0;
            sum=0;
        }
    }

//    public void resetAvg(View view) {
//        avgGravity.reset();
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

                // SENSOR_DELAY_NORMAL
        accListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                float z = sensorEvent.values[2];
                //Math.sqrt(x*x+y*y+z*z)

                if( avgEnabled ) {
                    avgGravity.add(z);
                    TextView tvAvg = (TextView) findViewById(R.id.xyzAvg);
                    tvAvg.setText("avg=" + avgGravity.avg() + " " + avgGravity.count);
                }

                long dt = System.currentTimeMillis()-tprevAcc;
                if( hmeterEnabled ) {
                    hmeterV += (z - avgGravity.avg()) * dt / 1000;
                    hmeter += hmeterV * dt / 1000;

                    TextView tv = (TextView) findViewById(R.id.xyz);
                    tv.setText("Shift = "+(int)(hmeter*100) + " cm");
                }

                tprevAcc = System.currentTimeMillis();
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };

        senSensorManager.registerListener(accListener, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);

        View.OnTouchListener btnTouch = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    avgGravity.reset();
                    avgEnabled = true;
                    hmeterEnabled = false;
                }else if (action == MotionEvent.ACTION_UP) {
                    avgEnabled = false;
                    hmeterEnabled = true;
                    hmeter = 0;
                    hmeterV = 0;
                }
                return false;   //  the listener has NOT consumed the event, pass it on
            }
        };
        findViewById(R.id.btnStart).setOnTouchListener(btnTouch);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void onPause() {
        super.onPause();
        senSensorManager.unregisterListener(accListener);
    }

    protected void onResume() {
        super.onResume();
        senSensorManager.registerListener(accListener, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }




}
