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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Created by vontell on 9/16/17.
 */

class RollingAverage {
    private int size;
    private double total = 0d;
    private int index = 0;
    private double samples[];

    public RollingAverage(int size) {
        this.size = size;
        samples = new double[size];
        for (int i = 0; i < size; i++) samples[i] = 0d;
    }

    public void add(double x) {
        total -= samples[index];
        samples[index] = x;
        total += x;
        if (++index == size) index = 0; // cheaper than modulus
    }

    public double getAverage() {
        return total / size;
    }
}

public class OverlayView extends View implements SensorEventListener {

    private float[] lastAccelerometer = new float[3];
    private float[] lastCompass = new float[3];
    private final int averageSize = 20;
    private RollingAverage accXAverage = new RollingAverage(averageSize);
    private RollingAverage accYAverage = new RollingAverage(averageSize);
    private RollingAverage accZAverage = new RollingAverage(averageSize);
    private RollingAverage compXAverage = new RollingAverage(averageSize);
    private RollingAverage compYAverage = new RollingAverage(averageSize);
    private RollingAverage compZAverage = new RollingAverage(averageSize);

    private Camera camera;
    private Random rand = new Random();

    public static final String DEBUG_TAG = "OverlayView Log";
    String accelData = "Accelerometer Data";
    String compassData = "Compass Data";
    private LocationManager locationManager;
    ARFragment arFrag;

    static final float ALPHA = 0.9f; // if ALPHA = 1 OR 0, no filter applies.

    public OverlayView(Context context, Camera camera, ARFragment frag) {
        super(context);

        this.camera = camera;
        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor accelSensor = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor compassSensor = sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        arFrag = frag;

        boolean isAccelAvailable = sensors.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME);
        boolean isCompassAvailable = sensors.registerListener(this, compassSensor, SensorManager.SENSOR_DELAY_GAME);

    }

    protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;
        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    private ArrayList<Location> peopleLocations;

    public void providePeople(JSONArray people) {

        peopleLocations = new ArrayList<>();
        for (int i = 0; i < 1; i++) {
            try {
                JSONObject person = people.getJSONObject(i);
                Location location = new Location(person.getString("name"));
                JSONObject locationJ = person.getJSONArray("location").getJSONObject(0);
                location.setLatitude(locationJ.getDouble("lat"));
                location.setLongitude(locationJ.getDouble("long"));
                location.setAltitude(10);
                peopleLocations.add(location);
            } catch (Exception e) {}

        }

    }

    public void onSensorChanged(SensorEvent event) {

        switch(event.sensor.getType())
        {
            case Sensor.TYPE_ACCELEROMETER:
                accXAverage.add(event.values[0]);
                accYAverage.add(event.values[1]);
                accZAverage.add(event.values[2]);
                lastAccelerometer[0] = (float) accXAverage.getAverage();
                lastAccelerometer[1] = (float) accYAverage.getAverage();
                lastAccelerometer[2] = (float) accZAverage.getAverage();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                compXAverage.add(event.values[0]);
                compYAverage.add(event.values[1]);
                compZAverage.add(event.values[2]);
                lastCompass[0] = (float) compXAverage.getAverage();
                lastCompass[1] = (float) compYAverage.getAverage();
                lastCompass[2] = (float) compZAverage.getAverage();
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
        contentPaint.setColor(Color.RED);
        arUpdate(canvas, contentPaint, camera);

    }

    public void arUpdate(Canvas canvas, Paint targetPaint, Camera camera) {

        Location lastLocation = ((MainActivity) getContext()).getLocation();

        if (lastLocation != null && peopleLocations != null) {
            for (Location loc : peopleLocations) {
                float curBearingToMW = lastLocation.bearingTo(loc);
                // compute rotation matrix
                float rotation[] = new float[9];
                float identity[] = new float[9];
                boolean gotRotation = false;
                if (lastAccelerometer != null && lastCompass != null) {
                    gotRotation = SensorManager.getRotationMatrix(rotation,
                            identity, lastAccelerometer, lastCompass);
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

                    // use roll for screen rotation
                    canvas.rotate((float) (0.0f - Math.toDegrees(orientation[2])));
                    // Translate, but normalize for the FOV of the camera -- basically, pixels per degree, times degrees == pixels
                    float dx = (float) ((canvas.getWidth() / horizontalFOV) * (Math.toDegrees(orientation[0]) - curBearingToMW));
                    float dy = (float) ((canvas.getHeight() / verticalFOV) * Math.toDegrees(orientation[1]));

                    // wait to translate the dx so the horizon doesn't get pushed off
                    canvas.translate(0.0f, 0.0f - dy);

                    if (dx > canvas.getWidth() / 2) {
                        Log.e("POS", "OFF SCREEN");
                        arFrag.acceptARUpdate(loc.getProvider(), false);
                    } else {
                        Log.e("POS", "ON SCREEN");
                        arFrag.acceptARUpdate(loc.getProvider(), true);
                    }

                    // make our line big enough to draw regardless of rotation and translation
                    //canvas.drawLine(0f - canvas.getHeight(), canvas.getHeight() / 2, canvas.getWidth() + canvas.getHeight(), canvas.getHeight() / 2, targetPaint);

                    // now translate the dx
                    canvas.translate(0.0f - dx, 0.0f);

                    // draw our point -- we've rotated and translated this to the right spot already
                    canvas.drawCircle(canvas.getWidth() / 2, canvas.getHeight() / 2, 32.0f, targetPaint);

                }
            }
        }
    }
}
