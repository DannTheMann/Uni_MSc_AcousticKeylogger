package dja33.msc.ukc.msc_log.sample;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dante on 26/02/2017.
 */

public class Sample {

    private final List<AccelerometerSample> accelerometerSamples;
    private final List<FrequencySample> frequencySamples;
    private final List<AmplitudeSample> amplitudeSamples;

    public Sample(){
        this.accelerometerSamples = new ArrayList<>();
        this.frequencySamples = new ArrayList<>();
        this.amplitudeSamples = new ArrayList<>();
    }

    public String getSample(String key){

        if(accelerometerSamples.isEmpty() && frequencySamples.isEmpty() && amplitudeSamples.isEmpty())
            return null;

        String sam = key + ",";

        for(AccelerometerSample as : accelerometerSamples){
            sam += as.x + "," + as.y + "," + as.z + "," + as.speed + "\n" + key + ",";
        }

        for(FrequencySample fs : frequencySamples){

        }

        for(AmplitudeSample as : amplitudeSamples){
            sam += as.average + "\n" + key + ",";
        }

        return sam;

    }

    public void addAccelerometerSample(float x, float y, float z, float speed){
        accelerometerSamples.add(new AccelerometerSample(x,y,z,speed));
    }

    public void addFrequencySample(){

    }

    public void addAmplitudeSample(double avg){
        amplitudeSamples.add(new AmplitudeSample(avg));
    }

    private class AccelerometerSample{

        private final float x;
        private final float y;
        private final float z;
        private final float speed;

        public AccelerometerSample(float x, float y, float z, float speed){
            this.x = x;
            this.y = y;
            this.z = z;
            this.speed = speed;
        }

    }

    private class FrequencySample{}

    private class AmplitudeSample{

        private final double average;

        public AmplitudeSample(double average){
            this.average = average;
        }

    }

}
