// Jonas Dorsch    115763
// Jana  Puschmann 115753

package com.example.mis.sensor;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import com.example.mis.sensor.FFT;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Random;

/**
 * GraphView lib: http://www.android-graphview.org
 * https://code.tutsplus.com/tutorials/using-the-accelerometer-on-android--mobile-22125
 * https://developer.android.com/guide/topics/sensors/sensors_motion
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final float NS2S = 1.0f / 1000000000.0f;

    //example variables
    private double[] rndAccExamplevalues;
    private double[] freqCounts;

    // sensor
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    // graph visualization
    private GraphView mAccelerometerVis;
    private Switch mFFTswitch; // switch to FFT graph
    private SeekBar mWindowSizeBar;
    private SeekBar mSampleRateBar;
    private LineGraphSeries<DataPoint> mCoorXLine, mCoorYLine, mCoorZLine, mMagnitudeLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // check whether accelerometer exists
        mAccelerometer = null;
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); // accelerometer exists
            mSensorManager.registerListener(this, mAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);

            mFFTswitch = findViewById(R.id.fft_switch);
            mWindowSizeBar = findViewById(R.id.window_size);
            mSampleRateBar = findViewById(R.id.sample_rate);

            // hide seekbars in non-fft visualization
            if (!mFFTswitch.isChecked()) {
                mWindowSizeBar.setVisibility(View.GONE);
                mSampleRateBar.setVisibility(View.GONE);
            }

            // initialize graph and curves
            mAccelerometerVis = findViewById(R.id.accelerometer_vis);
            mAccelerometerVis.setTitle("Accelerometer Data Visualization");
            mAccelerometerVis.setBackgroundColor(Color.LTGRAY); // since magnitude curve will be white
            Viewport viewport = mAccelerometerVis.getViewport();
            viewport.setScalable(true);
            viewport.setScalableY(true);

            mCoorXLine = new LineGraphSeries<>();
            mCoorXLine.setTitle("x direction data");
            mCoorXLine.setColor(Color.RED);
            mAccelerometerVis.addSeries(mCoorXLine);

            mCoorYLine = new LineGraphSeries<>();
            mCoorYLine.setTitle("y direction data");
            mCoorYLine.setColor(Color.GREEN);
            mAccelerometerVis.addSeries(mCoorYLine);

            mCoorZLine = new LineGraphSeries<>();
            mCoorZLine.setTitle("z direction data");
            mCoorZLine.setColor(Color.BLUE);
            mAccelerometerVis.addSeries(mCoorZLine);

            mMagnitudeLine = new LineGraphSeries<>();
            mMagnitudeLine.setTitle("magnitude data");
            mMagnitudeLine.setColor(Color.WHITE);
            mAccelerometerVis.addSeries(mMagnitudeLine);
        }
        else {
            Toast accelerometer_error = Toast.makeText(MainActivity.this, "Error! No accelerometer found!", Toast.LENGTH_SHORT);
            accelerometer_error.show();
        }

        //initiate and fill example array with random values
        rndAccExamplevalues = new double[64];
        randomFill(rndAccExamplevalues);
        new FFTAsynctask(64).execute(rndAccExamplevalues);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    /**
     * real time data handling: http://www.android-graphview.org/realtime-chart/
     * https://developer.android.com/reference/android/hardware/SensorEvent
     *
     * @param event
     */
    @Override
    public void onSensorChanged(final SensorEvent event) {
        Sensor sensor = event.sensor;

        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER && mAccelerometer != null) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (!mFFTswitch.isChecked()) {
                        // graph: x direction time, y direction coor value
                        mCoorXLine.appendData(new DataPoint(event.timestamp * NS2S, event.values[0]), true, 40);
                        mCoorYLine.appendData(new DataPoint(event.timestamp * NS2S, event.values[1]), true, 40);
                        mCoorZLine.appendData(new DataPoint(event.timestamp * NS2S, event.values[2]), true, 40);
                        mMagnitudeLine.appendData(new DataPoint(event.timestamp * NS2S, getMagnitude(event.values[0], event.values[1], event.values[2])), true, 40);
                    }

                }
            });
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // https://stackoverflow.com/questions/8565401/android-get-normalized-acceleration
    public float getMagnitude(float x, float y, float z) {
        return (float) Math.sqrt(x * x +  y * y + z * z);
    }


    // hide seekbars in non-fft mode
    public void seekbarToggle(View view) {
        if (!mFFTswitch.isChecked()) {
            mWindowSizeBar.setVisibility(View.GONE);
            mSampleRateBar.setVisibility(View.GONE);
        } else {
            mWindowSizeBar.setVisibility(View.VISIBLE);
            mSampleRateBar.setVisibility(View.VISIBLE);
        }
    }


    /**
     * Implements the fft functionality as an async task
     * FFT(int n): constructor with fft length
     * fft(double[] x, double[] y)
     */

    private class FFTAsynctask extends AsyncTask<double[], Void, double[]> {

        private int wsize; //window size must be power of 2

        // constructor to set window size
        FFTAsynctask(int wsize) {
            this.wsize = wsize;
        }

        @Override
        protected double[] doInBackground(double[]... values) {


            double[] realPart = values[0].clone(); // actual acceleration values
            double[] imagPart = new double[wsize]; // init empty

            /**
             * Init the FFT class with given window size and run it with your input.
             * The fft() function overrides the realPart and imagPart arrays!
             */
            FFT fft = new FFT(wsize);
            fft.fft(realPart, imagPart);
            //init new double array for magnitude (e.g. frequency count)
            double[] magnitude = new double[wsize];


            //fill array with magnitude values of the distribution
            for (int i = 0; wsize > i ; i++) {
                magnitude[i] = Math.sqrt(Math.pow(realPart[i], 2) + Math.pow(imagPart[i], 2));
            }

            return magnitude;

        }

        @Override
        protected void onPostExecute(double[] values) {
            //hand over values to global variable after background task is finished
            freqCounts = values;
        }
    }




    /**
     * little helper function to fill example with random double values
     */
    public void randomFill(double[] array){
        Random rand = new Random();
        for(int i = 0; array.length > i; i++){
            array[i] = rand.nextDouble();
        }
    }



}
