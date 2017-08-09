package dja33.ukc.keylogger;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;

import dja33.ukc.keylogger.sample.KeyHandler;

public class MainActivity extends AppCompatActivity {

    private SoundMeter soundMeter;
    private Thread smRun; // Thread to control the SoundMeter
    private KeyHandler keyHandler; // Data to handle learning of the keys

    // --------------
    /* UI ELEMENTS */
    // --------------
    private AccelerometerSensor accelerometerSensor; // AccelerometerSensor handler class
    private SensorManager senSensorManager; // SensorManager handler
    private Sensor senAccelerometer; // Accelerometer handler
    private Spinner spinner; // Displays all keyboard keys

    /* ID selections */
    private final int CHARACTER_SELECTION_ID = R.id.characterSelection;
    private final int CURRENT_CHARACTER_SELECTED = R.id.charSelectText;
    private final int TRAINER_MODE_BUTTON = R.id.trainerMode;
    private final int SAMPLING_BUTTON = R.id.samplingButton;
    private final int PROGRESS_SOUND_VOLUME = R.id.progress;
    private final int PROGRESS_RESULT = R.id.progressResult;
    // ------------------
    /* END UI ELEMENTS */
    // ------------------

    private static boolean sampling = false; // whether mic is sampling
    private static boolean training = false; // training mode active
    public static boolean isSampling() { return sampling; }
    public static boolean isTraining() { return training; }

    private static final String CHARACTER_SELECTED_TEXT = "Selected Character - ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        keyHandler = new KeyHandler(getFilesDir().getAbsolutePath());
        soundMeter = new SoundMeter();
        spinner = (Spinner) findViewById(CHARACTER_SELECTION_ID);

        //accelerometerSensor = new AccelerometerSensor();

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(accelerometerSensor, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);

        /* Add alphabet characters to the spinner, or the characters we define as our alphabet */
        inputCharactersFromAlphabet();

        /* Set character select text */
        ((TextView)findViewById(CURRENT_CHARACTER_SELECTED)).setText(CHARACTER_SELECTED_TEXT + keyHandler.getActiveKey());

        /* Add listeners to buttons on the UI */
        addButtonListeners();

        /* Will update the UI based on the sound meters finding */
        //UpdateProgress up = new UpdateProgress(soundMeter, spinner.getSelectedItem().toString());
        //smRun = new Thread(up);
        //smRun.start();

        sampling = true;
        System.out.println("Startup finished.");

    }

    @Override
    protected void onStop() {
        System.out.println("Stopping...");
        super.onStop();
    }
    /**
     * Add button listeners
     */
    private void addButtonListeners() {
        /* Training button */
        ((Button)findViewById(TRAINER_MODE_BUTTON)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                training = !training; // Toggle training
                if(training) {
                    // Enable training...
                    System.out.println("Training mode enabled, selected character is '" + keyHandler.getActiveKey() + "'.");
                }else{
                    System.out.println("Training mode disabled.");
                }
            }
        });

        /* Sampling button */
        ((Button)findViewById(SAMPLING_BUTTON)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sampling = !sampling; // Toggle sampling
                if(sampling) {
                    System.out.println("Sampling enabled.");
                }else{
                    System.out.println("Sampling disabled.");
                }
            }
        });

        /* Character spinner, used for training characters */
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                System.out.println("Changed spinner item (pos: " + position + " | id: " + id +"): " +  spinner.getItemAtPosition(position).toString());
                keyHandler.setActiveKey(spinner.getSelectedItem().toString()); // Update active key to spinner button
                ((TextView)findViewById(CURRENT_CHARACTER_SELECTED)).setText(CHARACTER_SELECTED_TEXT + keyHandler.getActiveKey());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                return; // not needed
            }

        });
    }

    private void inputCharactersFromAlphabet() {
        try {
            ArrayAdapter<String> spinnerItems = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, keyHandler.getKeys());
            spinner.setAdapter(spinnerItems);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private class UpdateProgress implements Runnable {

        private SoundMeter soundMeter;
        private String keyboardKey;
        private int index;
        private double[] samples;
        private double average;
        private EditText out;
        private ProgressBar volumeBar;

        // subject to change values
        private int progress;
        private double amplitude;

        public UpdateProgress(SoundMeter sm, String key) {
            this.soundMeter = sm;
            this.samples = new double[1024];
            this.keyboardKey = key;
            this.out = (EditText) findViewById(PROGRESS_RESULT);
            this.out.setKeyListener(null);
            this.volumeBar = (ProgressBar) findViewById(PROGRESS_SOUND_VOLUME);
        }

        public void run() {

            try {
                soundMeter.start();
                boolean off = true;
                int count = 0;
                while (true) {

                    if(!sampling){
                        Thread.sleep(500); // If not sampling, then sleep
                        continue;
                    }

                    SoundMeter.AmplitudeSample amplitudeSample = soundMeter.sampleAmplitude(keyHandler.getActiveKey());

                    amplitude = amplitudeSample.getHighestAmplitude();

                    /* There has been some noise detected */
                    if(amplitudeSample.getPeaks().size() > 0){
                        amplitudeSample.parsePeaksFFT(); // Perform FFT on subsamples
                        System.out.println("Peaks: " + amplitudeSample.getPeaks().size() + " | " + amplitudeSample.getAmplitudes().length);
                        for(SoundMeter.FrequencySample fs : amplitudeSample.getFrequencySamples()){
                            System.out.println(" >>> " + fs.getProminentFrequency() + " | " + fs.getHighestMagnitude());
                        }
                        keyHandler.addSampleData(amplitudeSample);
                    }

                    //amplitude = soundMeter.getHighestAmplitude();
                    progress = (int) ((amplitude / 32768) * 100); // Value out of 100

                    /* Update user interface components on the main thread */
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            out.setText("Prog: " + progress + "/100 | " + amplitude);
                            volumeBar.setProgress(progress); // Show the 'volume' of the sound

                        }
                    });

                    //Thread.sleep(50);

                   // long end = System.currentTimeMillis();

                    //System.out.println("Took : " + ((end - start) / 1000) + " >>> Amp: " + amplitude);
                }

            }catch(InterruptedException e) {
                volumeBar.setProgress(0);
                System.out.println("Interruption!");
                soundMeter.stop();
            }

            for(double res : samples){
                if(res > 0) {
                    average += res;
                }
            }

            average /= samples.length;

            //System.out.println("Average: " + average + " | Over " + samples.length + " samples.");

            //SampleHandler.getHandler().addAmplitudeSample(getActiveCharacter(), average);

            System.out.println("Logging: " + keyboardKey + " -> " + average);

        }
    }
}
