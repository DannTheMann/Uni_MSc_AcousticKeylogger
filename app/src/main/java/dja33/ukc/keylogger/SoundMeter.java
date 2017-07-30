package dja33.ukc.keylogger;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import org.jtransforms.fft.DoubleFFT_1D;

import java.util.ArrayList;

/**
 * Class borrows functionality from - https://stackoverflow.com/questions/38033068/android-audiorecord-wont-initialize
 */
public class SoundMeter {

    private static final int RECORD_SAMPLE_RATE = 44100;
    private static final int RECORD_AUDIO_SOURCE = MediaRecorder.AudioSource.DEFAULT;
    private static final int RECORD_FORMAT = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORD_PCM = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord audioRecord = null;
    private final int minSize; // Must be a power of 2, used for computing the scale of the frequency domain and amplitude
    private boolean recording;
    private double[] frequencySweep;

    public SoundMeter(){
        int tempMinSize = AudioRecord.getMinBufferSize(RECORD_SAMPLE_RATE, RECORD_FORMAT, RECORD_PCM);
        if(tempMinSize < 256) {
            minSize = RECORD_SAMPLE_RATE;
        }else{
            minSize = RECORD_SAMPLE_RATE;
        }
        frequencySweep = new double[minSize];
        audioRecord = new AudioRecord(RECORD_AUDIO_SOURCE, RECORD_SAMPLE_RATE, RECORD_FORMAT, RECORD_PCM, minSize);
    }

    public boolean start() {
        //minSize = AudioRecord.getMinBufferSize(RECORD_SAMPLE_RATE, RECORD_FORMAT, RECORD_PCM);
        System.out.println("Minimum size for buffer: " + minSize);
        audioRecord.startRecording();
        recording = true;
        return true;
    }

    public boolean stop() {
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            recording = false;
            return true;
        }
        return false;
    }

    public short[] sample(){
        short[] buffer = new short[minSize];
        audioRecord.read(buffer, 0, minSize);
        return buffer;
    }


    private double[] sampleDouble() {
        short[] buf = sample();
        double[] doubuf = new double[buf.length];
        for(int i = 0; i < buf.length; i++){
            doubuf[i] = buf[i];
        }
        return doubuf;
    }

    /**
     * Samples an absolute form from the buffer
     * @return
     */
    private int[] sampleAbsolute() {
        short[] buffer = sample();
        int[] absoluteBuffer = new int[buffer.length];
        /* Copy every value from the sample but remove their signed bit */
        for(int i = 0; i < buffer.length; i++){
            absoluteBuffer[i] = Math.abs(buffer[i]);
        }
        return absoluteBuffer;
    }


    /* Sample in 20 as described as so.
    *
    * Assuming we sample at 44.1KHz but our initial buffer holds 4096 values then we must
    *
    * Normally 44.1KHz / 1000 would be 44.1 samples per millisecond with 220.5 referring to
    * 5ms, however we must adjust for the buffer size.
    *
    * Buffer in this case allows that we hold 1 sample in 1 index at the cost of 11 samples as
    * follows:
    *
    *   44100 / 4096 = ~11.
    *
    * Therefore 220 / 11 = 20 and as such are samples within 5 milliseconds are within the indice
    * of 20 not 220.
    * */
    private final int samplesIn5ms = 3000;
    private final int minimumAmp = 3000;

    public AmplitudeSample sampleAudio(){

        double[] amps = sampleDouble();
        int c = 0;
//        for(int i : amps){
//            System.out.print("[" + c++ + "] = " +  i + ", ");
//            if( c % 5 == 0)
//                System.out.println();
//        }
//        System.out.println();
        final ArrayList<Integer> peaks = new ArrayList<>();
        /* While we still have peaks above the minimum amplitude */
        while(true) {

            double maxAmp = Double.MIN_VALUE;
            int candidate = -1;

            for (int i = 0; i < amps.length; i++) {

                i = skipPeaks(i, peaks);

                if(i >= amps.length){
                    break;
                }

//                /* For every peak recorded */
//                for (int peak : peaks) {
//                    /* If we've recorded this peak then skip
//                        over the entire subsample by n places */
//                    if (i == (peak - samplesIn5ms)) {
//                        i += (3 * samplesIn5ms);
//                        for (int innerPeak : peaks) {
//                            if(i == innerPeak){
//                                break;
//                            }
//                        }
//                    }
//                }

                /* Whether the current indexed amplitude exceeds the known maximum */
                if (Math.abs(amps[i]) > maxAmp) {
                    candidate = i; // Update indice
                    maxAmp = Math.abs(amps[i]); // Update max amp
                }

            }

            //System.out.println("maxAmp: " + maxAmp + "[" + candidate + "] > " + minimumAmp);;

            /* If we've yet to exhausted all subsamples within a 10ms gap of one another */
            if (maxAmp > minimumAmp) {
                peaks.add(candidate);
            } else {
                break;
            }

        }

        //System.out.println("Function returning.");

        return new AmplitudeSample(amps, peaks);

    }

    private int skipPeaks(int i, ArrayList<Integer> peaks){
        for(int peak : peaks){
            if((i >= (peak - samplesIn5ms)) && (i <= (peak + samplesIn5ms))){
                //System.out.println("Skipping 5, was @" + i + " -> " + (i + (2 * samplesIn5ms)));
                return skipPeaks(i + (2 * samplesIn5ms), peaks);
            }
        }
        return i;
    }

    public boolean isRecording() {
        return recording;
    }


    public double getHighestAmplitude() {
        short[] buffer = sample();
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

    /**
     * Retrieves a Frequency Domain Sweep of the microphone.
     * @return Array of frequency intensity
     */
    public FrequencySample getFrequencySample(String key){

        frequencySweep = new double[minSize];

        short[] buffer = sample();
        // reads in a continous set of samples into the buffer

        for (int i = 0; i < minSize; i++) {
            frequencySweep[i] = (double) buffer[i];// / 32768.0; // signed 16bit
        }

        DoubleFFT_1D fft = new DoubleFFT_1D(minSize);

        fft.realForward(frequencySweep);

        return new FrequencySample(frequencySweep, key);
    }

    public class AmplitudeSample{

        public final ArrayList<FrequencySample> frequencyPeaks;
        public final ArrayList<Integer> peaks;
        public final double[] amplitudes;

        public AmplitudeSample(double[] amps, ArrayList<Integer> peaks) {
            this.amplitudes = amps;
            this.peaks = peaks;
            this.frequencyPeaks = new ArrayList<>();
        }

        public ArrayList<Integer> getPeaks(){
            return this.peaks;
        }

        public double[] getAmplitudes(){
            return this.amplitudes;
        }

        public int[] getFFTPeaks(){

            int[] fftPeaks = new int[peaks.size()];
            double[] frequencySweep;
            DoubleFFT_1D fft = new DoubleFFT_1D(samplesIn5ms*2);

            for(int i = 0; i < fftPeaks.length; i++){

                frequencySweep = new double[samplesIn5ms*2];

                int peak = peaks.get(0);

                for(int k = 0,j = (peak-samplesIn5ms < 0) ? 0 : peak-samplesIn5ms;
                    (peak+samplesIn5ms >= amplitudes.length) ? j < amplitudes.length : j < peak+samplesIn5ms;
                    j++, k++){
                    //System.out.print(amplitudes[j] + ", ");
                    frequencySweep[k] = amplitudes[j];
                }

                fft.realForward(frequencySweep);

                FrequencySample frequencySample = new FrequencySample(frequencySweep);

                frequencyPeaks.add(frequencySample);

                fftPeaks[i] = frequencySample.getProminentFrequency();
            }

            return fftPeaks;

        }

        public int getHighestAmplitude() {
            double max = 0;
            for (double s : amplitudes)
            {
                if (Math.abs(s) > max)
                {
                    max = Math.abs(s);
                }
            }
            return (int) max;
        }

        public ArrayList<FrequencySample> getFrequencySamples(){
            return frequencyPeaks;
        }
    }

    private static int nextId;

    public class FrequencySample{

        private final int id;
        private final double[] fourierTransformedData;
        private final String key;
        private double highestMagnitude;
        // Corresponding index in the fourier array to the highest magnitude
        private int indexMagnitudeCorrespondence;

        public int getId(){ return id; }

        public FrequencySample(final double[] fourierTransformedData){
            this.id = nextId++;
            this.fourierTransformedData = fourierTransformedData;
            this.key = "N/A";
            // Set highestMagnitude and index of such
            highestMagnitude();
        }

        public FrequencySample(final double[] fourierTransformedData, final String key){
            this.id = nextId++;
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

        public int getProminentFrequency(){

            /**
             * Frequency = Fs * i / N
             *
             * Fs = sample rate (Hz)
             * i = index of peak
             * N = number of points in FFT (1024 in this case)
             *
             */

            /* Calculate the corresponding frequency */

            return (RECORD_SAMPLE_RATE * indexMagnitudeCorrespondence) / minSize;
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