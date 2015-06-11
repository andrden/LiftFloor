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
import android.widget.Toast;

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
    long prevSecond, prevMeters;
    List<Double> accList = new ArrayList<>();
    List<Integer> secChanges = new ArrayList<>();
    List<Integer> meterChanges = new ArrayList<>();

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

    class Floor extends SurfaceView{
        Paint paintText = new Paint(){{
            setStyle(Paint.Style.FILL);
            setColor(Color.BLUE);
            setTextSize(80);
        }};
        Paint paintRect = new Paint(){{
            setStyle(Style.STROKE);
            setColor(Color.WHITE);
        }};
        int floor=1;

        public Floor(Context context) {
            super(context);

            // This call is necessary, or else the
            // draw method will not be called.
            setWillNotDraw(false);
        }
        @Override
        protected void onDraw(Canvas canvas) {
            String txt = ""+floor;
            canvas.drawText(txt, (canvas.getWidth()-paintText.measureText(txt))/2, 50, paintText);

            canvas.drawRect(0, 0, canvas.getWidth() - 1, canvas.getHeight() - 1, paintRect);
        }
        void incr(int delta){
            floor += delta;
            this.postInvalidate();
        }
    }

    class Graph extends SurfaceView{
        Paint paintGreen = new Paint(){{
            setStyle(Paint.Style.FILL);
            setColor(Color.GREEN);
        }};
        Paint paintRed = new Paint(){{
            setStyle(Paint.Style.FILL);
            setColor(Color.RED);
        }};
        Paint paintWhite = new Paint(){{
            setStyle(Paint.Style.FILL);
            setColor(Color.WHITE);
        }};

        public Graph(Context context) {
            super(context);

            // This call is necessary, or else the
            // draw method will not be called.
            setWillNotDraw(false);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawCircle(50, 50, 30, paintGreen);

            for( int i=0; i<accList.size(); i++ ){
                int x = (int)(canvas.getWidth() * (1 + accList.get(i))/2);
                canvas.drawCircle(x,i/2,2, paintGreen);
            }
            for( int i : meterChanges ){
                canvas.drawRect(0,i/2,50,i/2+2, paintRed);
            }
            for( int i : secChanges ){
                canvas.drawRect(150,i/2,200,i/2+1, paintWhite);
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
        final Floor floor = new Floor(this);
        ((FrameLayout)findViewById(R.id.floorNumber)).addView(floor);
        OnSwipeTouchListener onTouchListener = new OnSwipeTouchListener(this) {
            public void onSwipeTop() {
                //Toast.makeText(MainActivity.this, "top", Toast.LENGTH_SHORT).show();
                floor.incr(+1);
            }

            public void onSwipeRight() {
                //Toast.makeText(MainActivity.this, "right", Toast.LENGTH_SHORT).show();
            }

            public void onSwipeLeft() {
                //Toast.makeText(MainActivity.this, "left", Toast.LENGTH_SHORT).show();
            }

            public void onSwipeBottom() {
                floor.incr(-1);
                //Toast.makeText(MainActivity.this, "bottom", Toast.LENGTH_SHORT).show();
            }

            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        };
        floor.setOnTouchListener(onTouchListener);
        graph.setOnTouchListener(onTouchListener);

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        accListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                float z = sensorEvent.values[2];
                double accAll = Math.sqrt(x*x+y*y+z*z);

                if( avgEnabled ) {
                    avgGravity.add((float)accAll);
                    TextView tvAvg = (TextView) findViewById(R.id.xyzAvg);
                    tvAvg.setText(String.format("avg=%.3f %d", avgGravity.avg(), avgGravity.count));
                }

                if( hmeterEnabledT0>0 ) {
                    long sec = (System.currentTimeMillis() - hmeterEnabledT0)/1000;
                    boolean secChange = prevSecond != sec;
                    prevSecond = sec;
                    if( sec > 20 ){
                        hmeterEnabledT0 = 0; // disabled
                    }
                    long dtNanos = sensorEvent.timestamp - tprevAccNanos;
                    double acc = accAll - avgGravity.avg();
                    accMin = Math.min(acc, accMin);
                    accMax = Math.max(acc, accMax);
                    if( accList.size()<1500 ){
                        accList.add(acc);
                        graph.postInvalidate();
                    }
                    hmeterV += acc * dtNanos / 1_000_000_000;
                    hmeter += hmeterV * dtNanos / 1_000_000_000;
                    long newHmeter = (int)Math.abs(hmeter);
                    boolean meterChange = prevMeters != newHmeter;
                    prevMeters = newHmeter;

                    if( secChange ) secChanges.add(accList.size());
                    if( meterChange ) meterChanges.add(accList.size());

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
                    prevSecond=0;
                    prevMeters=0;
                    accList.clear();
                    secChanges.clear();
                    meterChanges.clear();
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
