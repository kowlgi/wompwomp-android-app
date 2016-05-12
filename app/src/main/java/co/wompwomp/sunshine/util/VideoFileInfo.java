package co.wompwomp.sunshine.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class VideoFileInfo implements Serializable{
    private String videouri = null;
    private int filesize = 0;

    public VideoFileInfo(String videouri, int filesize) {
        this.videouri = videouri;
        this.filesize = filesize;
    }

    public void setVideouri(String videouri) {
        this.videouri = videouri;
    }

    public void setFilesize(int filesize) {
        this.filesize = filesize;
    }

    public String getVideouri() {
        return this.videouri;
    }

    public int getFilesize(){
        return this.filesize;
    }

    /**
     * Always treat de-serialization as a full-blown constructor, by
     * validating the final state of the de-serialized object.
     */
    private void readObject(
            ObjectInputStream aInputStream
    ) throws ClassNotFoundException, IOException {
        //always perform the default de-serialization first
        aInputStream.defaultReadObject();

        //MUSTFIX: ensure that object state has not been corrupted or tampered with maliciously
    }

    /**
     * This is the default implementation of writeObject.
     * Customise if necessary.
     */
    private void writeObject(
            ObjectOutputStream aOutputStream
    ) throws IOException {
        //perform the default serialization for all non-transient, non-static fields
        aOutputStream.defaultWriteObject();
    }
}
