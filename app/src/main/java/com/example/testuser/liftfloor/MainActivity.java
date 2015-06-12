package com.example.testuser.liftfloor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
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


public class MainActivity extends ActionBarActivity {
    public static final int SENSOR_DELAY = SensorManager.SENSOR_DELAY_GAME;//SensorManager.SENSOR_DELAY_FASTEST;
    SensorManager senSensorManager;

    Sensor senAccelerometer;
    AccListener accListener;

    class Floor extends SurfaceView{
        Paint paintText = new Paint(){{
            setStyle(Paint.Style.FILL);
            setColor(Color.GREEN);
            setTextSize(80);

        }};
        Paint paintRect = new Paint(){{
            setStyle(Style.STROKE);
            setColor(Color.GREEN);
        }};
        int floor=1;
        private final Rect textBounds = new Rect(); //don't new this up in a draw method

        public void drawTextCentred(Canvas canvas, Paint paint, String text, float cx, float cy){
            paint.getTextBounds(text, 0, text.length(), textBounds);
            canvas.drawText(text, cx - textBounds.exactCenterX(), cy - textBounds.exactCenterY(), paint);
        }

        public Floor(Context context) {
            super(context);

            // This call is necessary, or else the
            // draw method will not be called.
            setWillNotDraw(false);
        }
        @Override
        protected void onDraw(Canvas canvas) {
            float floorDelta = (float)accListener.getFloorDelta();
            if( floorDelta>20 ) floorDelta=20;
            if( floorDelta<-20 ) floorDelta=-20;
            float shift = floorDelta - (int)floorDelta;
            int newfloor = floor + (int)floorDelta;
            
            float maxTxtWidth=0;
            for( int i=-1; i<=1; i++ ) {
                String txt = "" + (newfloor+i);
                float txtWidth = paintText.measureText(txt);
                maxTxtWidth = Math.max(maxTxtWidth, txtWidth);
                float txtStart = (canvas.getWidth() - txtWidth) / 2;

                //canvas.drawText(txt, txtStart, 50, paintText);
                float y = (float)(10 + paintText.getTextSize() * (0.5 + shift - i));
                drawTextCentred(canvas, paintText, txt, txtStart + txtWidth / 2, y);
            }

            float txtStart = (canvas.getWidth() - maxTxtWidth) / 2;
            canvas.drawRect(txtStart - 5, 10, txtStart + maxTxtWidth + 5, 10 + paintText.getTextSize(), paintRect);

            canvas.drawRect(3, 3, canvas.getWidth() - 3, canvas.getHeight() - 3, paintRect);
        }
        void incr(int delta){
            if( accListener.getFloorDelta()!=0 ) {
                floor += Math.round(accListener.getFloorDelta());
                accListener.hmeter = 0;
                accListener.hmeterEnabledT0 = 0; // stop on swipe
            }
            floor += delta;
            this.postInvalidate();
        }
    }

    class Graph extends SurfaceView{
        Paint paintGreen = new Paint(){{
            setStyle(Paint.Style.FILL);
            setColor(Color.GREEN);
        }};
        Paint paintCyan = new Paint(){{
            setStyle(Paint.Style.FILL);
            setColor(Color.CYAN);
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

            int velocityXPrev=0;
            for( int i=0; i<accListener.accList.size(); i++ ){
                int x = (int)(canvas.getWidth() * (1 + accListener.accList.get(i))/2);
                canvas.drawCircle(x,i/2,2, paintGreen);

                double SPEED_MAX=2;
                int velocityX = (int)(canvas.getWidth() * (1 + accListener.speedList.get(i)/SPEED_MAX)/2);
                canvas.drawLine(velocityXPrev,(i-1)/2.f, velocityX, i/2.f, paintCyan );
                velocityXPrev = velocityX;
            }
            for( int i : accListener.meterChanges ){
                canvas.drawRect(0,i/2,50,i/2+2, paintRed);
            }
            for( int i : accListener.secChanges ){
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
                floor.incr(-1);
            }

            public void onSwipeRight() {
                floor.incr(1-floor.floor);
                //Toast.makeText(MainActivity.this, "right", Toast.LENGTH_SHORT).show();
            }

            public void onSwipeLeft() {
                //Toast.makeText(MainActivity.this, "left", Toast.LENGTH_SHORT).show();
            }

            public void onSwipeBottom() {
                floor.incr(+1);
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

        accListener = new AccListener(){

            @Override
            void avgText(String txt) {
                TextView tvAvg = (TextView) findViewById(R.id.xyzAvg);
                tvAvg.setText(txt);
            }

            @Override
            protected void setShiftText(String line1, String line2) {
                TextView tv = (TextView) findViewById(R.id.xyz);
                tv.setText(line1);

                TextView tv2 = (TextView) findViewById(R.id.xyz2);
                tv2.setText(line2);
            }

            @Override
            void postInvalidate() {
                graph.postInvalidate();
                floor.postInvalidate();
            }
        };

        senSensorManager.registerListener(accListener, senAccelerometer, SENSOR_DELAY);

        View.OnTouchListener btnTouch = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    accListener.reset();
                }else if (action == MotionEvent.ACTION_UP) {
                    accListener.start();
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
