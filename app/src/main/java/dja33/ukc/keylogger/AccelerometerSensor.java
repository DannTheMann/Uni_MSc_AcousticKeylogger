package dja33.ukc.keylogger;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

/**
 * Created by Dante on 17/02/2017.
 */

public class AccelerometerSensor implements SensorEventListener {

    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 600;
    private static final int DELAY_BETWEEN_UPDATE = 25;

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            long curTime = System.currentTimeMillis();

           // if ((curTime - lastUpdate) > DELAY_BETWEEN_UPDATE) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float speed = Math.abs(x + y + z - last_x - last_y - last_z)/ diffTime * 10000;

                last_x = x;
                last_y = y;
                last_z = z;

            // Changing this one file

//                if(speed > 3){
//                    if(MainActivity.isSampling()) {
//                        System.out.println("Logging...");
//                        SampleHandler.getHandler().addAccelerometerSample(MainActivity.getActiveCharacter(), x, y, z, speed);
//                    }else{
//                       // System.out.println("X: " + x + ", Y: " + y   +", Z: " + z + ", Speed: " + speed);
//                    }
//                }else{
//
//                }

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
