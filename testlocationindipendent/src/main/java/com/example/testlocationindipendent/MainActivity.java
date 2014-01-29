package com.example.testlocationindipendent;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.TextView;
import com.example.testlocationindipendent.R;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class MainActivity extends ActionBarActivity implements SensorEventListener {

    private class VectorValues {
        public float timestamp;
        public float x;
        public float y;
        public float z;

        VectorValues(float x, float y, float z, float timestamp) {
            this.x = x; this.y = y; this.z = z; this.timestamp = timestamp;
        }
    }

    private SensorManager mSensorManager;
    private Sensor mSensorAccelerometer;
    private Sensor mSensorLinearAcceleration;
    private Sensor mSensorRotationVector;
    private long lastTimestampAccelerometer = 0;
    private float[] lastValuesAccelerometer;
    private float[] lastValueRotationVector;
    private float[] lastValueLinearAcceleration;
    private int sizeBuffer = 1000 / 64;
    private int nextPositionInsertElement = 0;
    private boolean bufferFull = false;
    private ArrayList<VectorValues> lastValuesForMean = new ArrayList<VectorValues>(sizeBuffer);
    DecimalFormat numberFormat = new DecimalFormat("#0.00");
    private float[] componentsAccelerometerWithAngle = new float[3];
    private float[] componentsLinearAccelerationWithAngle = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER );
        mSensorLinearAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorRotationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void startRecording(View view) {

        mSensorManager.registerListener(this, mSensorAccelerometer, 65000);
        mSensorManager.registerListener(this, mSensorLinearAcceleration, 65000);
        mSensorManager.registerListener(this, mSensorRotationVector, 65000);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent.sensor == mSensorAccelerometer) {
            workWithAccelerationData(sensorEvent.values, sensorEvent.timestamp);
        }
        else if (sensorEvent.sensor == mSensorLinearAcceleration) {
            workWithLinearAccelerationData(sensorEvent.values);
        }
        else if (sensorEvent.sensor == mSensorRotationVector) {
            workWithRotationVectorData(sensorEvent.values);
        }
    }

    private void workWithAccelerationData(float[] values, long timestamp) {

        ((TextView) findViewById(R.id.xAccelerometro)).setText(("X: " + numberFormat.format(values[0])));
        ((TextView) findViewById(R.id.yAccelerometro)).setText(("Y: " + numberFormat.format(values[1])));
        ((TextView) findViewById(R.id.zAccelerometro)).setText(("Z: " + numberFormat.format(values[2])));

        lastValuesAccelerometer = (float[])values.clone();

        if (lastTimestampAccelerometer == 0) {
            lastTimestampAccelerometer = timestamp;
        }
        else {
            ((TextView) findViewById(R.id.deltaTimestamp)).setText("Delta tempo: " + (timestamp - lastTimestampAccelerometer) / 1000000);
            lastTimestampAccelerometer = timestamp;
        }

        lastValuesForMean.add(nextPositionInsertElement,
                new VectorValues(values[0], values[1], values[2], timestamp));

        nextPositionInsertElement++;
        if (nextPositionInsertElement == sizeBuffer) {
            nextPositionInsertElement = 0; bufferFull = true;
        }

        if (bufferFull) {

            float meanXValue = 0, meanYValue = 0, meanZValue = 0;

            for (int i = 0; i < sizeBuffer; i++) {
                meanXValue += lastValuesForMean.get(i).x;
                meanYValue += lastValuesForMean.get(i).y;
                meanZValue += lastValuesForMean.get(i).z;
            }

            meanXValue /= sizeBuffer;
            meanYValue /= sizeBuffer;
            meanZValue /= sizeBuffer;

            ((TextView) findViewById(R.id.xMedia)).setText("Xm: " + numberFormat.format(meanXValue));
            ((TextView) findViewById(R.id.yMedia)).setText("Ym: " + numberFormat.format(meanYValue));
            ((TextView) findViewById(R.id.zMedia)).setText("Zm: " + numberFormat.format(meanZValue));
            ((TextView) findViewById(R.id.normaMedia)).setText("Norma: " + numberFormat.format(
                    Math.sqrt(Math.pow(meanXValue, 2) + Math.pow(meanYValue, 2) + Math.pow(meanZValue, 2))));

            float x = values[0] - meanXValue, y = values[1] - meanYValue, z = values[2] - meanZValue;

            ((TextView) findViewById(R.id.xVettoreD)).setText("Xd: " + numberFormat.format((x)));
            ((TextView) findViewById(R.id.yVettoreD)).setText("Yd: " + numberFormat.format((y)));
            ((TextView) findViewById(R.id.zVettoreD)).setText("Zd: " + numberFormat.format((z)));

            ((TextView) findViewById(R.id.normaVettoreD)).setText("Norma: " + numberFormat.format(
                    Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2))));

            float normMeanValues = calculateNorm(meanXValue, meanYValue, meanZValue);

            float vectorProduct = (x * meanXValue + y * meanYValue + z * meanZValue) / (float)Math.pow(normMeanValues, 2);
            float vectorPComponentsX = meanXValue * vectorProduct,
                    vectorPComponentsY = meanYValue * vectorProduct,
                    vectorPComponentsZ = meanZValue * vectorProduct;

            ((TextView) findViewById(R.id.pComponenteX)).setText("Px: " + numberFormat.format(vectorPComponentsX));
            ((TextView) findViewById(R.id.pComponenteY)).setText("Py: " + numberFormat.format(vectorPComponentsY));
            ((TextView) findViewById(R.id.pComponenteZ)).setText("Pz: " + numberFormat.format(vectorPComponentsZ));

            //printArray("VECTOR P: ", vectorPComponents, 3);

            float vectorHComponentsX = x - vectorPComponentsX,
                    vectorHComponentsY = y - vectorPComponentsY,
                    vectorHComponentsZ = z - vectorPComponentsZ;

            ((TextView) findViewById(R.id.hComponenteX)).setText("Hx: " + numberFormat.format(vectorHComponentsX));
            ((TextView) findViewById(R.id.hComponenteY)).setText("Hy: " + numberFormat.format(vectorHComponentsY));
            ((TextView) findViewById(R.id.hComponenteZ)).setText("Hz: " + numberFormat.format(vectorHComponentsZ));

        }
    }

    private void workWithLinearAccelerationData(float[] values) {

        ((TextView) findViewById(R.id.xLinearAcceleration)).setText(("X: " + numberFormat.format(values[0])));
        ((TextView) findViewById(R.id.yLinearAcceleration)).setText(("Y: " + numberFormat.format(values[1])));
        ((TextView) findViewById(R.id.zLinearAcceleration)).setText(("Z: " + numberFormat.format(values[2])));
        ((TextView) findViewById(R.id.normaLinearAcceleration)).setText("Norma: " + numberFormat.format(
              Math.sqrt(Math.pow(values[0], 2) + Math.pow(values[1], 2) + Math.pow(values[2], 2))));

        lastValueLinearAcceleration = (float[])values.clone();
    }

    private void workWithRotationVectorData(float[] values) {

        float norm = calculateNorm(values[0], values[1], values[2]);
        float alpha = 2 * (float)Math.asin(norm);

        ((TextView) findViewById(R.id.xRotationVector)).setText("X: " + numberFormat.format(values[0] ));
        ((TextView) findViewById(R.id.yRotationVector)).setText("Y: " + numberFormat.format(values[1] ));
        ((TextView) findViewById(R.id.zRotationVector)).setText("Z: " + numberFormat.format(values[2] ));
        lastValueRotationVector = (float[])values.clone();

            ((TextView) findViewById(R.id.alpha)).setText("Alpha:  " + numberFormat.format(alpha));

        if (lastValuesAccelerometer != null) {
            this.calculateXYZSecond(values[0] / norm, values[1] / norm, values[2] / norm, alpha,
                    lastValuesAccelerometer[0], lastValuesAccelerometer[1], lastValuesAccelerometer[2], componentsAccelerometerWithAngle);

            ((TextView) findViewById(R.id.xAccelerometerWithAngle)).setText("X Angolo: " + numberFormat.format(componentsAccelerometerWithAngle[0]));
            ((TextView) findViewById(R.id.yAccelerometerWithAngle)).setText("Y Angolo: " + numberFormat.format(componentsAccelerometerWithAngle[1]));
            ((TextView) findViewById(R.id.zAccelerometerWithAngle)).setText("Z Angolo: " + numberFormat.format(componentsAccelerometerWithAngle[2]));

            //printArray("ACCELER_START", lastValuesAccelerometer, 3);
            //printArray("ACCELEROMETER", componentsAccelerometerWithAngle, 3);
        }

        if (lastValueLinearAcceleration != null) {

            this.calculateXYZSecond(values[0] / norm, values[1] / norm, values[2] / norm, alpha,
                    lastValueLinearAcceleration[0], lastValueLinearAcceleration[1], lastValueLinearAcceleration[2], componentsLinearAccelerationWithAngle);

            ((TextView) findViewById(R.id.xLinearWithAngle)).setText("X Angolo: " + numberFormat.format(componentsLinearAccelerationWithAngle[0]));
            ((TextView) findViewById(R.id.yLinearWithAngle)).setText("Y Angolo: " + numberFormat.format(componentsLinearAccelerationWithAngle[1]));
            ((TextView) findViewById(R.id.zLinearWithAngle)).setText("Z Angolo: " + numberFormat.format(componentsLinearAccelerationWithAngle[2]));

            //printArray("LINEAR_START", lastValueLinearAcceleration, 3);
            //printArray("LINEAR ACCELERATION: ", componentsLinearAccelerationWithAngle, 3);
        }
    }

    private void calculateXYZSecond(float x, float y, float z, float teta, float xFirst, float yFirst, float zFirst,
                                       float[] components) {

        double xSquare = Math.pow(x, 2), ySquare = Math.pow(y, 2), zSquare = Math.pow(z, 2);
        double sinTeta = Math.sin(teta), cosTeta = Math.cos(teta);

        components[0] = (float)((xSquare + (1 - xSquare) * cosTeta) * xFirst +
                (((1 - cosTeta) * x * y) - sinTeta * z) * yFirst +
                (((1 - cosTeta) * x * z) + sinTeta * y) * zFirst);

        components[1] = (float)((((1 - cosTeta) * y * x) + sinTeta * z) * xFirst +
                (ySquare + (1 - ySquare) * cosTeta) * yFirst +
                (((1 - cosTeta) * y * z) - sinTeta * x)  * zFirst);

        components[2] = (float)((((1 - cosTeta) * z * x) - sinTeta * y) * xFirst +
                        ((1 - cosTeta) * z * y + sinTeta * x) * yFirst +
                        (zSquare + (1 - zSquare) * cosTeta) * zFirst);
    }

    private float calculateNorm(float x, float y, float z) {
        return (float)Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
    }

    private void printArray(String tag, float[] array, int size) {
        String finalString = "";
        for (int i = 0; i < size; i++) {
            finalString += ("[" + i + "] => " + array[i]);
        }
        Log.d(tag, finalString);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

}
