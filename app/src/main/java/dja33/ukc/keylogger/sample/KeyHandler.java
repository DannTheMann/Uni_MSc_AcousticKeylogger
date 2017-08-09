package dja33.ukc.keylogger.sample;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

import dja33.ukc.keylogger.SoundMeter;

/**
 * Created by Dante on 26/02/2017.
 */

public class KeyHandler implements Serializable{

    private final String DIRECTORY;

    // Map to hold all key samples to their associate keys
    private final TreeMap<String, KeySample> keystrokeSamples = new TreeMap<>();
    // Set of all keys we're sampling for
    private final String[] keySamples = {"SPACE", "Q", "ENTER", "H", ";"};

    private transient String activeKey;

    public KeyHandler(String dir){

        this.DIRECTORY = dir;
        activeKey = keySamples[0];
        loadKeySamples();

    }

    private void loadKeySamples(){

        for(String key : keySamples){
            System.out.println("Loading '" + key + "'.ser");
            try {
                keystrokeSamples.put(key, loadKeySample(key));
            }catch(Exception e){
                System.err.println("Failed to load '" + key + "'.");
                e.printStackTrace();
            }
        }

    }

    private KeySample loadKeySample(String key) throws Exception{

        KeySample sample = null;
        final String file = DIRECTORY + File.separator + key + ".ser";

        if(!new File(file).exists()){
            System.out.println("File did not exist for this key (" + key + "), creating one.");
            return new KeySample(key);
        }

        try {
            FileInputStream fileIn = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            sample = (KeySample) in.readObject();
            in.close();
            fileIn.close();

            return sample;
        }catch(IOException i) {
            i.printStackTrace();
            throw new IOException("Failed to deserialize the KeySample - '" + key + "'.");
        }catch(ClassNotFoundException c) {
            System.err.println("KeySample class not found");
            c.printStackTrace();
            throw new ClassNotFoundException("Failed to deserialize the KeyHandler.");
        }

    }

    private boolean saveKey(String key){

        final String file = DIRECTORY + File.separator + key + ".ser";
        final KeySample ks = keystrokeSamples.get(key);

        if(ks == null){
            System.err.println("KeySample for " + key + " was null.");
            return false;
        }

        if(key != null){
            try {
                FileOutputStream fileOut =
                        new FileOutputStream(file);
                ObjectOutputStream out = new ObjectOutputStream(fileOut);
                out.writeObject(ks);
                out.close();
                fileOut.close();
                System.out.printf("Serialized KeySample " + key + " data is saved in " + DIRECTORY + ".");
                return true;
            }catch(IOException i) {
                System.err.println("Failed to serialize data for " + key + " .");
                i.printStackTrace();
            }
        }
        return false;

    }

    public boolean saveActiveKey(){

        return saveKey(activeKey);

    }

    public boolean saveAllKeys(){

        boolean saved = true;

        for(String key : keystrokeSamples.keySet()){

            if(saved){
                if(!saveKey(key))
                    saved = false;
            }else
                saveKey(key);


        }

        return saved;

    }

    /**
     * Returns all keys that are being trained.
     * @return keys
     */
    public String[] getKeys(){
        return keySamples;
    }

    /**
     * Set the active key to be trained.
     * @param activeKey key
     */
    public void setActiveKey(String activeKey) {
        this.activeKey = activeKey;
    }

    /**
     * Get the current key being trained
     * @return key
     */
    public String getActiveKey() {
        return activeKey;
    }

    public void addSampleData(SoundMeter.AmplitudeSample sample, boolean save){
        keystrokeSamples.get(sample.getKey()).update(sample);
        if(save){
            saveActiveKey();
        }
    }

    /**
     * Called whenever the KeyHandler is deserialized such that any
     * transient fields can be remolded.
     */
    public void prepareTransient() {
        activeKey = keySamples[0];
    }
}
