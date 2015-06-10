package com.example.testuser.liftfloor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity {
    public static final int SENSOR_DELAY = SensorManager.SENSOR_DELAY_GAME;//SensorManager.SENSOR_DELAY_FASTEST;
    SensorManager senSensorManager;

    Sensor senAccelerometer;
    SensorEventListener accListener;
    long tprevAccNanos=System.currentTimeMillis();

    long hmeterEnabledT0=0;
    double hmeter = 0;
    double hmeterV = 0;
    double accMin, accMax;
    List<Double> accList = new ArrayList<>();

    boolean avgEnabled=false;
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

    class Graph extends SurfaceView{
        Paint paint = new Paint(){{
            setStyle(Paint.Style.FILL);
            setColor(Color.GREEN);
        }};

        public Graph(Context context) {
            super(context);

            // This call is necessary, or else the
            // draw method will not be called.
            setWillNotDraw(false);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawCircle(50, 50, 30, paint);

            for( int i=0; i<accList.size(); i++ ){
                int x = (int)(canvas.getWidth() * (1 + accList.get(i))/2);
                canvas.drawCircle(x,i,3, paint);
            }
        }
    }

//    public void resetAvg(View view) {
//        avgGravity.reset();
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Graph graph = new Graph(this);
        ((FrameLayout)findViewById(R.id.graphLayout)).addView(graph);
        //graph.postInvalidateDelayed(3000);

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

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
                    tvAvg.setText(String.format("avg=%.3f %d", avgGravity.avg(), avgGravity.count));
                }

                long sec = (System.currentTimeMillis() - hmeterEnabledT0)/1000;
                if( sec > 15 ){
                    hmeterEnabledT0 = 0; // disabled
                }
                if( hmeterEnabledT0>0 ) {
                    long dtNanos = sensorEvent.timestamp - tprevAccNanos;
                    double acc = z - avgGravity.avg();
                    accMin = Math.min(acc, accMin);
                    accMax = Math.max(acc, accMax);
                    if( accList.size()<500 ){
                        accList.add(acc);
                        graph.postInvalidate();
                    }
                    hmeterV += acc * dtNanos / 1_000_000_000;
                    hmeter += hmeterV * dtNanos / 1_000_000_000;

                    TextView tv = (TextView) findViewById(R.id.xyz);
                    tv.setText(String.format("Shift %.2f m, acc %.2f .. %.2f",hmeter, accMin, accMax));

                    TextView tv2 = (TextView) findViewById(R.id.xyz2);
                    tv2.setText(String.format("sec=%d, dtMs=%d", sec, dtNanos/1_000_000));
                }

                tprevAccNanos = sensorEvent.timestamp;
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };

        senSensorManager.registerListener(accListener, senAccelerometer, SENSOR_DELAY);

        View.OnTouchListener btnTouch = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    avgGravity.reset();
                    avgEnabled = true;
                    hmeterEnabledT0 = 0; // disabled
                }else if (action == MotionEvent.ACTION_UP) {
                    avgEnabled = false;
                    hmeter = 0;
                    hmeterV = 0;
                    accMin = 100;
                    accMax = -100;
                    accList.clear();
                    hmeterEnabledT0 = System.currentTimeMillis();
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
        senSensorManager.registerListener(accListener, senAccelerometer, SENSOR_DELAY);
    }




}
