package com.avontell.resq_responder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.view.View;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * Created by vontell on 9/16/17.
 */

public class OverlayView extends View implements SensorEventListener {

    private final static Location mountWashington = new Location("manual");
    static {
        mountWashington.setLatitude(42.357328);
        mountWashington.setLongitude(-71.100857);
        mountWashington.setAltitude(10);
    }

    private float[] lastAccelerometer = null;
    private float[] lastCompass = null;
    private Camera camera;

    public static final String DEBUG_TAG = "OverlayView Log";
    String accelData = "Accelerometer Data";
    String compassData = "Compass Data";
    String gyroData = "Gyro Data";
    private LocationManager locationManager;

    public OverlayView(Context context, Camera camera) {
        super(context);

        this.camera = camera;
        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor accelSensor = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor compassSensor = sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor gyroSensor = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        boolean isAccelAvailable = sensors.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
        boolean isCompassAvailable = sensors.registerListener(this, compassSensor, SensorManager.SENSOR_DELAY_NORMAL);
        boolean isGyroAvailable = sensors.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);

    }

    public void onSensorChanged(SensorEvent event) {
        StringBuilder msg = new StringBuilder(event.sensor.getName()).append(" ");

        switch(event.sensor.getType())
        {
            case Sensor.TYPE_ACCELEROMETER:
                accelData = msg.toString();
                lastAccelerometer = event.values;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                compassData = msg.toString();
                lastCompass = event.values;
                break;
        }
        this.invalidate();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint contentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        contentPaint.setTextAlign(Paint.Align.CENTER);
        contentPaint.setTextSize(20);
        contentPaint.setColor(Color.RED);
        canvas.drawText(accelData, canvas.getWidth()/2, canvas.getHeight()/4, contentPaint);
        canvas.drawText(compassData, canvas.getWidth()/2, canvas.getHeight()/2, contentPaint);
        canvas.drawText(gyroData, canvas.getWidth()/2, (canvas.getHeight()*3)/4, contentPaint);
        arUpdate(canvas, contentPaint, camera);

    }

    ArrayDeque<float[]> average = new ArrayDeque<>();

    public void arUpdate(Canvas canvas, Paint targetPaint, Camera camera) {

        Location lastLocation = ((MainActivity) getContext()).getLocation();

        if (lastLocation != null) {

            float curBearingToMW = lastLocation.bearingTo(mountWashington);
            // compute rotation matrix
            float rotation[] = new float[9];
            float identity[] = new float[9];
            boolean gotRotation = false;
            if (lastAccelerometer != null && lastCompass != null) {
                gotRotation = SensorManager.getRotationMatrix(rotation,
                        identity, lastAccelerometer, lastCompass);
            }

            if (gotRotation) {
                // orientation vector
                float orientation[] = new float[3];
                SensorManager.getOrientation(rotation, orientation);
            }

            if (gotRotation) {
                float cameraRotation[] = new float[9];
                // remap such that the camera is pointing straight down the Y axis
                SensorManager.remapCoordinateSystem(rotation, SensorManager.AXIS_X,
                        SensorManager.AXIS_Z, cameraRotation);

                // orientation vector
                float orientation[] = new float[3];
                SensorManager.getOrientation(cameraRotation, orientation);
                if (gotRotation) {
                    cameraRotation = new float[9];
                    // remap such that the camera is pointing along the positive direction of the Y axis
                    SensorManager.remapCoordinateSystem(rotation, SensorManager.AXIS_X,
                            SensorManager.AXIS_Z, cameraRotation);

                    // orientation vector
                    orientation = new float[3];
                    SensorManager.getOrientation(cameraRotation, orientation);
                }

                Camera.Parameters params = camera.getParameters();
                float verticalFOV = params.getVerticalViewAngle();
                float horizontalFOV = params.getHorizontalViewAngle();

                average.addFirst(orientation);
                float x = 0;
                float y = 0;
                float z = 0;
                int s = average.size();
                for (float[] vals : average) {
                    x += vals[0];
                    y += vals[1];
                    z += vals[2];
                }
                orientation = new float[]{x / s, y /s, z / s};
                if (s > 15) {
                    average.removeLast();
                }

                // use roll for screen rotation
                canvas.rotate((float) (0.0f - Math.toDegrees(orientation[2])));
                // Translate, but normalize for the FOV of the camera -- basically, pixels per degree, times degrees == pixels
                float dx = (float) ((canvas.getWidth() / horizontalFOV) * (Math.toDegrees(orientation[0]) - curBearingToMW));
                float dy = (float) ((canvas.getHeight() / verticalFOV) * Math.toDegrees(orientation[1]));

                // wait to translate the dx so the horizon doesn't get pushed off
                canvas.translate(0.0f, 0.0f - dy);

                // make our line big enough to draw regardless of rotation and translation
                canvas.drawLine(0f - canvas.getHeight(), canvas.getHeight() / 2, canvas.getWidth() + canvas.getHeight(), canvas.getHeight() / 2, targetPaint);


                // now translate the dx
                canvas.translate(0.0f - dx, 0.0f);

                // draw our point -- we've rotated and translated this to the right spot already
                canvas.drawCircle(canvas.getWidth() / 2, canvas.getHeight() / 2, 32.0f, targetPaint);

            }

        }
    }

}
