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
import android.view.View;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity {
    SensorManager senSensorManager;

    Sensor senAccelerometer;
    SensorEventListener accListener;
    long tprevAcc=System.currentTimeMillis();

    float hmeter = 0;
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

    public void resetAvg(View view) {
        avgGravity.reset();
    }

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

                avgGravity.add(z);
                TextView tvAvg = (TextView)findViewById(R.id.xyzAvg);
                tvAvg.setText("avg="+avgGravity.avg()+" "+avgGravity.count);


                TextView tv = (TextView)findViewById(R.id.xyz);
                tv.setText(" dt="+(System.currentTimeMillis()-tprevAcc));
                tprevAcc = System.currentTimeMillis();
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };

        senSensorManager.registerListener(accListener, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
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
