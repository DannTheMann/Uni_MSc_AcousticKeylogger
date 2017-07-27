package dja33.ukc.keylogger;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dante on 16/02/2017.
 */
public class FileHandler {

    private static final String APP_DIRECTORY = "Keylogger";
    private final Context CONTEXT; // Used to access APP config
    private final String DIRECTORY;
    private final File fir;
    private boolean read;
    private final String FILE_NAME;
    private final String FILE; // Entire file path
    private final List<String> contents = new ArrayList<>();

    /**
     * Create a filehandler that can provide access to a File to write, read and store
     * all records from the keylogger.
     * @param directory The directory the file is located in
     * @param fileName The filename
     * @param context The context of the App for config
     */
    public FileHandler(String directory, String fileName, Context context) {
        this.DIRECTORY = directory + File.separator + APP_DIRECTORY;
        this.fir = new File(directory);
        this.FILE_NAME = fileName;
        this.FILE = DIRECTORY + File.separator + FILE_NAME;
        this.CONTEXT = context;
        System.out.println("Directory: " + FILE);
        new File(DIRECTORY).mkdirs(); // Try to make the directory if it doesn't exist
        try {
            new File(FILE).createNewFile(); // Make the file if it doesn't exist
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete the file holding all current records of information
     */
    public void delete(){
        new File(FILE_NAME).delete();
        contents.clear();
    }

    /**
     * Read the file and store its contents in contents array.
     * @return true if the file was read successfully
     * @throws Exception
     */
    public boolean readFile() throws Exception {

        if(read) {
            return false;
        }

        try {

            InputStream inputStream = CONTEXT.openFileInput(FILE_NAME);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                String receiveString;
                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    contents.add(receiveString);
                }

                inputStream.close();
                bufferedReader.close();

            }else{
                return false;
            }

            read = true;

        }catch(Exception e){
            return false;
        }

        return true;

    }

    private OutputStreamWriter outputStreamWriter;

    public boolean openOutput(){
        try {
            outputStreamWriter = new OutputStreamWriter(CONTEXT.openFileOutput(FILE_NAME, Context.MODE_PRIVATE));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean closeOut(){
        try {
            outputStreamWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void writeOut(String msg) throws Exception{
        outputStreamWriter.write(msg + File.separator);
        contents.add(msg + File.separator);
    }

    public ArrayList<String> getFileElements(){
        return new ArrayList<>(contents);
    }

    public boolean read(){
        return read;
    }

    public String getFilePath(){
        return FILE;
    }

}
