package dja33.ukc.keylogger;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import org.jtransforms.fft.DoubleFFT_1D;

/**
 * Class borrows functionality from - https://stackoverflow.com/questions/38033068/android-audiorecord-wont-initialize
 */
public class SoundMeter {

    private static final int RECORD_SAMPLE_RATE = 11025;
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

    /**
     * Retrieves a Frequency Domain Sweep of the microphone.
     * @return Array of frequency intensity
     */
    public double[] getFrequencyDomain(){

        frequencySweep = new double[minSize];

        short[] buffer = new short[minSize];
        // reads in a continous set of samples into the buffer 
        int bufferReadResult = ar.read(buffer, 0, minSize);

        for (int i = 0; i < minSize && i < bufferReadResult; i++) {
            frequencySweep[i] = (double) buffer[i] / 32768.0; // signed 16bit
        }

        DoubleFFT_1D fft = new DoubleFFT_1D(minSize);

        fft.realForward(frequencySweep);

        return frequencySweep;
    }

    @Deprecated
    private double[] fourierTransform(final double[] inputReal,
                               boolean DIRECT) {

        double[] inputImag = new double[inputReal.length];
        // - n is the dimension of the problem
        // - nu is its logarithm in base e
        int n = inputReal.length;

        // If n is a power of 2, then ld is an integer (_without_ decimals)
        double ld = Math.log(n) / Math.log(2.0);

        // Here I check if n is a power of 2. If exist decimals in ld, I quit
        // from the function returning null.
        if (((int) ld) - ld != 0) {
            System.out.println("The number of elements is not a power of 2.");
            return null;
        }

        // Declaration and initialization of the variables
        // ld should be an integer, actually, so I don't lose any information in
        // the cast
        int nu = (int) ld;
        int n2 = n / 2;
        int nu1 = nu - 1;
        double[] xReal = new double[n];
        double[] xImag = new double[n];
        double tReal, tImag, p, arg, c, s;

        // Here I check if I'm going to do the direct transform or the inverse
        // transform.
        double constant;
        if (DIRECT)
            constant = -2 * Math.PI;
        else
            constant = 2 * Math.PI;

        // I don't want to overwrite the input arrays, so here I copy them. This
        // choice adds \Theta(2n) to the complexity.
        for (int i = 0; i < n; i++) {
            xReal[i] = inputReal[i];
            xImag[i] = inputImag[i];
        }

        // First phase - calculation
        int k = 0;
        for (int l = 1; l <= nu; l++) {
            while (k < n) {
                for (int i = 1; i <= n2; i++) {
                    p = bitreverseReference(k >> nu1, nu);
                    // direct FFT or inverse FFT
                    arg = constant * p / n;
                    c = Math.cos(arg);
                    s = Math.sin(arg);
                    tReal = xReal[k + n2] * c + xImag[k + n2] * s;
                    tImag = xImag[k + n2] * c - xReal[k + n2] * s;
                    xReal[k + n2] = xReal[k] - tReal;
                    xImag[k + n2] = xImag[k] - tImag;
                    xReal[k] += tReal;
                    xImag[k] += tImag;
                    k++;
                }
                k += n2;
            }
            k = 0;
            nu1--;
            n2 /= 2;
        }

        // Second phase - recombination
        k = 0;
        int r;
        while (k < n) {
            r = bitreverseReference(k, nu);
            if (r > k) {
                tReal = xReal[k];
                tImag = xImag[k];
                xReal[k] = xReal[r];
                xImag[k] = xImag[r];
                xReal[r] = tReal;
                xImag[r] = tImag;
            }
            k++;
        }

        // Here I have to mix xReal and xImag to have an array (yes, it should
        // be possible to do this stuff in the earlier parts of the code, but
        // it's here to readibility).
        double[] newArray = new double[xReal.length * 2];
        double radice = 1 / Math.sqrt(n);
        for (int i = 0; i < newArray.length; i += 2) {
            int i2 = i / 2;
            // I used Stephen Wolfram's Mathematica as a reference so I'm going
            // to normalize the output while I'm copying the elements.
            newArray[i] = xReal[i2] * radice;
            newArray[i + 1] = xImag[i2] * radice;
        }
        return newArray;
    }

    /**
     * The reference bitreverse function.
     */
    @Deprecated
    private int bitreverseReference(int j, int nu) {
        int j2;
        int j1 = j;
        int k = 0;
        for (int i = 1; i <= nu; i++) {
            j2 = j1 / 2;
            k = 2 * k + j1 - 2 * j2;
            j1 = j2;
        }
        return k;
    }
}