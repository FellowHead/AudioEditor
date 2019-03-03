package me.fellowhead.audioeditor.mashup;

import javafx.concurrent.Task;
import me.fellowhead.audioeditor.AudioIOListener;
import me.fellowhead.audioeditor.ReferenceAudioFile;
import me.fellowhead.audioeditor.SimpleAudioConversion;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class PrepAudioFile extends ReferenceAudioFile {
    private double bpm;

    public double getBpm() {
        return bpm;
    }

    public PrepAudioFile(File file, AudioIOListener listener, double bpm) {
        super(file, listener);
        this.bpm = bpm;
    }
}
