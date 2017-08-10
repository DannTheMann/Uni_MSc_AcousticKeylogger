package dja33.ukc.keylogger.sample;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.Key;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import dja33.ukc.keylogger.SoundMeter;

/**
 * Created by Dante on 26/02/2017.
 */

public class KeyHandler{

    private final String DIRECTORY;

    // Map to hold all key samples to their associate keys
    private final TreeMap<String, KeySample> keystrokeSamples = new TreeMap<>();
    // Set of all keys we're sampling for
    private final String[] keySamples = {"SPACE", "Q", "ENTER", "H", ";"};

    private KeySample activeKey;

    public KeyHandler(String dir){

        this.DIRECTORY = dir;
        loadKeySamples();
        activeKey = keystrokeSamples.firstEntry().getValue();

    }

    private void loadKeySamples(){

        for(String key : keySamples){
            System.out.format("Loading '%s'.ser\n", key);
            try {
                keystrokeSamples.put(key, loadKeySample(key));
            }catch(Exception e){
                System.err.format("Failed to load '%s'.\n", key);
                e.printStackTrace();
            }
        }

    }

    private KeySample loadKeySample(String key) throws Exception{

        KeySample sample = null;
        final String file = DIRECTORY + File.separator + key + ".ser";

        if(!new File(file).exists()){
            System.err.format("File did not exist for this key ('%s'), creating one.\n", key);
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
            System.err.format("KeySample for '%s' was null.\n", key);
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

        return saveKey(activeKey.getKey());

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

    public Collection<KeySample> getAllKeys() { return keystrokeSamples.values(); }

    public int getTotalKeys(){ return keySamples.length; }

    /**
     * Set the active key to be trained.
     * @param activeKey key
     */
    public void setActiveKey(String activeKey) {
        this.activeKey = keystrokeSamples.get(activeKey);
    }

    /**
     * Get the current key being trained
     * @return key
     */
    public KeySample getActiveKey() {
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
        activeKey = keystrokeSamples.firstEntry().getValue();
    }
}
