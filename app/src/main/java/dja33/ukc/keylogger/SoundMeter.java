package dja33.ukc.keylogger;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import org.jtransforms.fft.DoubleFFT_1D;

/**
 * Class borrows functionality from - https://stackoverflow.com/questions/38033068/android-audiorecord-wont-initialize
 */
public class SoundMeter {

    private static final int RECORD_SAMPLE_RATE = 44100;
    private static final int RECORD_AUDIO_SOURCE = MediaRecorder.AudioSource.DEFAULT;
    private static final int RECORD_FORMAT = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORD_PCM = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord ar = null;
    private final int minSize; // Must be a power of 2, used for computing the scale of the frequency domain and amplitude
    private boolean running;
    private double[] frequencySweep;

    public SoundMeter(){
        int tempMinSize = AudioRecord.getMinBufferSize(RECORD_SAMPLE_RATE, RECORD_FORMAT, RECORD_PCM);
        if(tempMinSize < 256) {
            minSize = 256;
        }else{
            minSize = tempMinSize;
        }
        frequencySweep = new double[minSize];
    }

    public boolean start() {
        //minSize = AudioRecord.getMinBufferSize(RECORD_SAMPLE_RATE, RECORD_FORMAT, RECORD_PCM);
        System.out.println("Minimum size for buffer: " + minSize);
        ar = new AudioRecord(RECORD_AUDIO_SOURCE, RECORD_SAMPLE_RATE, RECORD_FORMAT, RECORD_PCM, minSize);
        ar.startRecording();
        running = true;
        return true;
    }

    public boolean stop() {
        if (ar != null) {
            ar.stop();
            ar.release();
            running = false;
            return true;
        }
        return false;
    }


    public boolean isRunning() {
        return running;
    }


    public double getAmplitude() {
        short[] buffer = new short[minSize];
        //System.out.println("Reading in (Size: " + minSize +" )");
        ar.read(buffer, 0, minSize);
        int max = 0;
        for (short s : buffer)
        {
            if (Math.abs(s) > max)
            {
                max = Math.abs(s);
            }
        }
        return max;
    }

    private final int fourierSize = 256;

    /**
     * Retrieves a Frequency Domain Sweep of the microphone.
     * @return Array of frequency intensity
     */
    public FrequencySample getFrequencySample(String key){

        frequencySweep = new double[fourierSize];

        short[] buffer = new short[fourierSize];
        // reads in a continous set of samples into the buffer

        long start = System.currentTimeMillis();
        int bufferReadResult = ar.read(buffer, 0, fourierSize);
        long end = System.currentTimeMillis();
        System.out.println("Took: " + ((end - start)));

        for (int i = 0; i < minSize && i < bufferReadResult; i++) {
            frequencySweep[i] = (double) buffer[i];// / 32768.0; // signed 16bit
        }

        DoubleFFT_1D fft = new DoubleFFT_1D(fourierSize);

        fft.realForward(frequencySweep);

        return new FrequencySample(frequencySweep, key);
    }

    public class FrequencySample{

        private final double[] fourierTransformedData;
        private final String key;
        private double highestMagnitude;
        // Corrosponding index in the fourier array to the highest magnitude
        private int indexMagnitudeCorrespondence;


        public FrequencySample(final double[] fourierTransformedData, final String key){
            this.fourierTransformedData = fourierTransformedData;
            this.key = key;
            // Set highestMagnitude and index of such
            highestMagnitude();
        }

        public double[] getFrequencySweep(){
            return this.fourierTransformedData;
        }

        public String getKey(){
            return this.key;
        }

        public int length(){
            return this.fourierTransformedData.length;
        }

        public double getProminentFrequency(){

            /**
             * Frequency = Fs * i / N
             *
             * Fs = sample rate (Hz)
             * i = index of peak
             * N = number of points in FFT (1024 in this case)
             *
             */

            /* Calculate the corresponding frequency */

            return (RECORD_SAMPLE_RATE * indexMagnitudeCorrespondence) / fourierSize;
        }

        public double getHighestMagnitude(){
            return highestMagnitude;
        }

        private void highestMagnitude(){

            /**
             * Magnitude = sqrt(re*re + im*im)
             *
             * re = Real component at 2*m
             * im = Imaginary component 2*m+1
             * m = index of array
             */

            double highestMagnitude = 0.0;
            double temporaryMagnitude = 0.0;
            int correspondingIndex = 0;

            /* Iterate over the size of the transform data (-1 to avoid out of bounds) */
            for(int i = 0; i < (fourierTransformedData.length/2)-1; i++){
                double real = fourierTransformedData[2*i]; // real component
                double imaginary = fourierTransformedData[(2*i)+1]; // imaginary component
                temporaryMagnitude = Math.sqrt( (real*real) + (imaginary*imaginary));
                if(temporaryMagnitude > highestMagnitude){
                    highestMagnitude = temporaryMagnitude;
                    correspondingIndex = i;
                }
            }

            this.highestMagnitude = highestMagnitude;
            this.indexMagnitudeCorrespondence = correspondingIndex;

        }

    }
}