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
import android.widget.TextView;
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
    private double[] magnitudeValues;

    // sensor
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    // graph visualization
    private GraphView mAccelerometerVis;
    private LineGraphSeries<DataPoint> mCoorXLine, mCoorYLine, mCoorZLine, mMagnitudeLine, mFFTLine;

    private Switch mFFTswitch; // switch to FFT graph
    private SeekBar mWindowSizeBar;
    private SeekBar mSampleRateBar;
    private TextView mTextWsize;
    private TextView mTextSrate;

    private int mWindowSize = 64; // has to be power of 2
    private int mSampleRate = SensorManager.SENSOR_DELAY_NORMAL; // 3
    private int mIndexFFT = 0;

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

            mTextWsize = findViewById(R.id.text_wsize);
            mTextSrate = findViewById(R.id.text_srate);

            // hide seekbars in non-fft visualization
            if (!mFFTswitch.isChecked()) {
                mWindowSizeBar.setVisibility(View.GONE);
                mSampleRateBar.setVisibility(View.GONE);
                mTextWsize.setVisibility(View.GONE);
                mTextSrate.setVisibility(View.GONE);
            }

            // initialize graph and curves
            mAccelerometerVis = findViewById(R.id.accelerometer_vis);
            mAccelerometerVis.setTitle("Accelerometer Data Visualization");
            mAccelerometerVis.setBackgroundColor(Color.LTGRAY); // since magnitude curve will be white
            Viewport viewport = mAccelerometerVis.getViewport();
            viewport.setScalable(true);
            viewport.setScalableY(true);
            viewport.setScrollable(true);
            viewport.setScrollableY(true);

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

            mFFTLine = new LineGraphSeries<>();
            mFFTLine.setTitle("transformed magnitude data");
            mFFTLine.setColor(Color.CYAN);
            mAccelerometerVis.addSeries(mFFTLine);

            magnitudeValues = new double[mWindowSize];

            // https://examples.javacodegeeks.com/android/core/widget/seekbar/android-seekbar-example/
            mTextWsize.setText("Window Size: " + mWindowSize);
            mWindowSizeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                int mProgress = 0;

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mProgress = progress + 3;
                    mWindowSize = (int) Math.pow(2, mProgress);
                    //Toast.makeText(getApplicationContext(), "Changing seekbar's progress to " + mProgress, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    //Toast.makeText(getApplicationContext(), "Started tracking seekbar", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    mTextWsize.setText("Window Size: " + mWindowSize);
                    magnitudeValues = new double[mWindowSize];
                    mIndexFFT = 0;
                    //Toast.makeText(getApplicationContext(), "Stopped tracking seekbar", Toast.LENGTH_SHORT).show();
                }
            });

            mTextSrate.setText("Sample Rate: ");
            mSampleRateBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                int mProgress = 0;

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mProgress = progress;
                    switch(mProgress) {
                        case 0: mSampleRate = SensorManager.SENSOR_DELAY_FASTEST;
                            break;
                        case 1: mSampleRate = SensorManager.SENSOR_DELAY_GAME;
                            break;
                        case 2: mSampleRate = SensorManager.SENSOR_DELAY_NORMAL;
                            break;
                        case 3: mSampleRate = SensorManager.SENSOR_DELAY_UI;
                            break;
                    }
                    //Toast.makeText(getApplicationContext(), "Changing seekbar's progress to " + mProgress, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    //Toast.makeText(getApplicationContext(), "Started tracking seekbar", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    switch(mProgress) {
                        case 0: mTextSrate.setText("Sample Rate: SENSOR_DELAY_FASTEST");
                            break;
                        case 1: mTextSrate.setText("Sample Rate: SENSOR_DELAY_GAME");
                            break;
                        case 2: mTextSrate.setText("Sample Rate: SENSOR_DELAY_NORMAL");
                            break;
                        case 3: mTextSrate.setText("Sample Rate: SENSOR_DELAY_UI");
                            break;
                    }
                    mSensorManager.unregisterListener(MainActivity.this);
                    mSensorManager.registerListener(MainActivity.this, mAccelerometer, mSampleRate);
                    //Toast.makeText(getApplicationContext(), "Stopped tracking seekbar", Toast.LENGTH_SHORT).show();
                }
            });

        }
        else {
            Toast.makeText(MainActivity.this, "Error! No accelerometer found!", Toast.LENGTH_SHORT).show();
        }

        //initiate and fill example array with random values
        //rndAccExamplevalues = new double[64];
        //randomFill(rndAccExamplevalues);
        //new FFTAsynctask(64).execute(rndAccExamplevalues);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, mSampleRate);
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
                        mCoorXLine.appendData(new DataPoint(event.timestamp * NS2S, event.values[0]), true, 250);
                        mCoorYLine.appendData(new DataPoint(event.timestamp * NS2S, event.values[1]), true, 250);
                        mCoorZLine.appendData(new DataPoint(event.timestamp * NS2S, event.values[2]), true, 250);
                        mMagnitudeLine.appendData(new DataPoint(event.timestamp * NS2S, getMagnitude(event.values[0], event.values[1], event.values[2])), true, 40);
                    } else {
                        magnitudeValues[mIndexFFT % mWindowSize] = getMagnitude(event.values[0], event.values[1], event.values[2]);
                        ++mIndexFFT;
                        new FFTAsynctask(mWindowSize).execute(magnitudeValues);
                    }

                }
            });
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    // clear graph data
    public void clearGraph(GraphView graph) {
        graph.removeAllSeries();
        DataPoint[] clear_array = new DataPoint[0]; // create empty point array
        mCoorXLine.resetData(clear_array);          // reset existing lines
        mCoorYLine.resetData(clear_array);
        mCoorZLine.resetData(clear_array);
        mMagnitudeLine.resetData(clear_array);
        mFFTLine.resetData(clear_array);

        if (!mFFTswitch.isChecked()) {
            mAccelerometerVis.addSeries(mCoorXLine);
            mAccelerometerVis.addSeries(mCoorYLine);
            mAccelerometerVis.addSeries(mCoorZLine);
            mAccelerometerVis.addSeries(mMagnitudeLine);
        } else {
            mAccelerometerVis.addSeries(mFFTLine);
        }
    }


    // https://stackoverflow.com/questions/8565401/android-get-normalized-acceleration
    public float getMagnitude(float x, float y, float z) {
        return (float) Math.sqrt(x * x +  y * y + z * z);
    }


    // hide seekbars in non-fft mode and clear the graph
    public void seekbarToggle(View view) {

        if (!mFFTswitch.isChecked()) {
            mWindowSizeBar.setVisibility(View.GONE);
            mSampleRateBar.setVisibility(View.GONE);
            mTextWsize.setVisibility(View.GONE);
            mTextSrate.setVisibility(View.GONE);
        } else {
            mWindowSizeBar.setVisibility(View.VISIBLE);
            mSampleRateBar.setVisibility(View.VISIBLE);
            mTextWsize.setVisibility(View.VISIBLE);
            mTextSrate.setVisibility(View.VISIBLE);
        }

        clearGraph(mAccelerometerVis);
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

            // add points to fft series
            DataPoint[] fft_point = new DataPoint[this.wsize];
            for (int i = 0; i < this.wsize; ++i) {
                fft_point[i] = new DataPoint(i, freqCounts[i]);
            }
            mFFTLine.resetData(fft_point);
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
