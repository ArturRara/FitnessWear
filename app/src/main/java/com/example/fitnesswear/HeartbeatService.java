package com.example.fitnesswear;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class HeartbeatService extends WearableActivity implements SensorEventListener{
    private TextView measure;
    private ImageButton startButton, stopButton;
    private ImageView callEmergency;
    private SensorManager sensorManager;
    private Sensor heartRateSensor;
    private boolean measuring;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.heartbbeat_layout);

        measure = (TextView) findViewById(R.id.measure);
        startButton = (ImageButton) findViewById(R.id.startButton);
        stopButton = (ImageButton) findViewById(R.id.stopButton);
        callEmergency = (ImageView) findViewById(R.id.callEmergencyButton);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startButton.setVisibility(ImageButton.INVISIBLE);
                stopButton.setVisibility(ImageButton.VISIBLE);
                Toast toast = Toast.makeText(getApplicationContext(), "Start", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM,0,20);
                toast.show();
                startMeasure();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopButton.setVisibility(ImageButton.INVISIBLE);
                startButton.setVisibility(ImageButton.VISIBLE);
                Toast toast = Toast.makeText(getApplicationContext(), "Stop", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM,0,20);
                toast.show();
                stopMeasure();
            }
        });

        callEmergency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast toast = Toast.makeText(getApplicationContext(), "Emergency", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM,0,20);
                toast.show();
            }
        });

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        setAmbientEnabled();
    }

    private void startMeasure(){
        if (sensorManager != null && heartRateSensor != null) {
            sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_FASTEST);
            measuring = true;
        }
    }

    private void stopMeasure(){
        measuring = false;
        sensorManager.unregisterListener(this,heartRateSensor);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float hearRateFloat = event.values[0];
        int heartRate = Math.round(hearRateFloat);// Arredonda para int
        measure.setText(Integer.toString(heartRate));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d("acc", "onAccuracyChanged:" + accuracy);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}