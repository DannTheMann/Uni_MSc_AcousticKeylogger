package dja33.ukc.keylogger.sample;

import java.io.Serializable;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;

import dja33.ukc.keylogger.SoundMeter;

/**
 * Created by Dante on 26/02/2017.
 */

public class KeySample implements Serializable {

    private final String key;

    private Average magnitude;
    private Average frequency;

    public KeySample(String key){
        this.key = key;
        magnitude = new Average();
        frequency = new Average();
    }

    public String getKey(){
        return this.key;
    }

    protected void update(final SoundMeter.AmplitudeSample sample) {

        for(SoundMeter.FrequencySample fs : sample.getFrequencySamples()){
            magnitude.update(fs.getHighestMagnitude());
            frequency.update(fs.getProminentFrequency());
        }

    }

    public double getAverageFrequency(){
        return frequency.avg();
    }

    public double getAverageMagnitude(){
        return magnitude.avg();
    }

    public void reset() {
        magnitude = new Average();
        frequency = new Average();
    }

    private class Average implements Serializable{

        private double total;
        private int totalInputs;

        public double update(double val){
            totalInputs++;
            total += val;
            return avg();
        }

        public double avg(){
            return total / totalInputs;
        }



    }
}
