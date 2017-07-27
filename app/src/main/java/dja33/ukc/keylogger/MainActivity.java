package dja33.ukc.keylogger;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    private FileHandler fileHandler;
    private SoundMeter soundMeter;
    private Thread smRun; // Thread to control the SoundMeter

    private AccelerometerSensor accelerometerSensor; // AccelerometerSensor handler class
    private SensorManager senSensorManager; // SensorManager handler
    private Sensor senAccelerometer;
    private Spinner spinner; // Displays all keyboard keys

    private static boolean sampling = false; // whether mic is sampling
    private static boolean training = false; // training mode active

    public static final String[] keyboardKeys = {"SPACE", "Q", "ENTER", "H", ";"};
    private static String activeKeyboardKey = keyboardKeys[0];

    public static boolean isSampling() { return sampling; }
    public static boolean isTraining() { return training; }
    public static String getActiveCharacter(){ return activeKeyboardKey; }

    private final int CHARACTER_SELECTION_ID = R.id.characterSelection;
    private final int TRAINER_MODE_BUTTON = R.id.trainerMode;
    private final int PROGRESS_SOUND_VOLUME = R.id.progress;
    private final int PROGRESS_RESULT = R.id.progressResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        fileHandler = new FileHandler(Environment.getExternalStorageDirectory().getAbsolutePath(), "data.csv", getApplicationContext());
        soundMeter = new SoundMeter();
        spinner = (Spinner) findViewById(CHARACTER_SELECTION_ID);

        accelerometerSensor = new AccelerometerSensor();

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(accelerometerSensor, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);

        /* Add alphabet characters to the spinner, or the characters we define as our alphabet */
        inputCharactersFromAlphabet();

        /* Add listeners to buttons on the UI */
        addButtonListeners();

        /* Will update the UI based on the sound meters finding */
        UpdateProgress up = new UpdateProgress(soundMeter, spinner.getSelectedItem().toString());
        smRun = new Thread(up);
        smRun.start();

        sampling = true;

        System.out.println("Startup finished.");

    }

    /**
     * Add button listeners
     */
    private void addButtonListeners() {
        /* training button */
        ((Button)findViewById(TRAINER_MODE_BUTTON)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                training = !training; // Toggle training
                if(training) {
                    activeKeyboardKey = spinner.getSelectedItem().toString(); // Update active key to spinner button
                    System.out.println("Training mode enabled, selected character is '" + activeKeyboardKey + "'.");
                }else{
                    System.out.println("Training mode disabled.");
                }
            }
        });
    }

    private void inputCharactersFromAlphabet() {
        try {

            ArrayAdapter<String> spinnerItems = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, keyboardKeys);
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

                while (true) {

                    long start = System.currentTimeMillis();

                    amplitude = soundMeter.getAmplitude();
                    progress = (int) ((amplitude / 32768) * 100); // Value out of 100
                    double[] frequencySweep = null;

                    if(amplitude > 5000) {
                        frequencySweep = soundMeter.getFrequencyDomain(); // Perform FFT
                        //                    System.out.println("Amplitude: " + amp);
                        //                    System.out.println("Amplitude accelerometerSensor %: " + ((amp/32768)*100));

                        samples[(index + 1) % 1024] = amplitude;

                        System.out.print(">");
                        for (double s : frequencySweep) {
                            if (s < 0)
                                continue;
                            DecimalFormat df = new DecimalFormat("#.##");
                            s = Double.valueOf(df.format(s));
                            System.out.print(s + "|");
                        }
                        System.out.println("<");
                        System.out.print("FSweepSize: " + frequencySweep.length + " | ");
                    }

                    /* Update user interface components on the main thread */
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            out.setText("Prog: " + progress + "/100 | " + amplitude);
                            volumeBar.setProgress(progress); // Show the 'volume' of the sound

                        }
                    });

                    Thread.sleep(10);

                    long end = System.currentTimeMillis();

                    System.out.println("Took : " + ((end - start) / 1000) + " >>> Amp: " + amplitude);
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

            System.out.println("Average: " + average + " | Over " + samples.length + " samples.");

            //SampleHandler.getHandler().addAmplitudeSample(getActiveCharacter(), average);

            System.out.println("Logging: " + keyboardKey + " -> " + average);

        }
    }
}
