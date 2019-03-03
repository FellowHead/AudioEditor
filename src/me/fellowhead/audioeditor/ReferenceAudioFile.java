package me.fellowhead.audioeditor;

import javafx.concurrent.Task;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ReferenceAudioFile extends AudioFile {
    private int loadState;
    private File file;
    private float sampleRate;
    private int bytesPerSmp;

    public ReferenceAudioFile(File file, AudioIOListener listener) {
        super(new float[0][0]);
        this.file = file;
        new Thread(() -> {
            try {
                initFile(AudioSystem.getAudioInputStream(file), listener);
            } catch (UnsupportedAudioFileException | IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public File getFile() {
        return file;
    }

    public float getSampleRate() {
        return sampleRate;
    }

    public int getBytesPerSmp() {
        return bytesPerSmp;
    }

    private void initFile(AudioInputStream stream, AudioIOListener listener) {
        System.out.println("Reading from file...");
        this.sampleRate = stream.getFormat().getSampleRate();
        //set sample rate
        this.bytesPerSmp = stream.getFormat().getSampleSizeInBits() / 8;
        Task task = createImportWorker(stream);
        listener.taskCreated(task);
        new Thread(task).start();
    }

    private Task createImportWorker(AudioInputStream stream) {
        return new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                ArrayList<Float> list = new ArrayList<>();
                final int readLength = 4096*2;
                try {
                    loadState = 0;
                    while (true) {
                        byte[] read = new byte[readLength];
                        int n = stream.read(read);
                        if (n < 0) {
                            break;
                        }
                        float[] samples = new float[readLength];
                        int n2 = SimpleAudioConversion.decode(read, samples, n, stream.getFormat());
                        for (int i = 0; i < n2; i++) {
                            list.add(samples[i]);
                        }
                        loadState += n2 / stream.getFormat().getChannels();
                        updateProgress(loadState, stream.getFrameLength());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                float[] out = new float[list.size()];
                for (int i = 0; i < out.length; i++) {
                    out[i] = list.get(i);
                }

                setData(slice(out, stream.getFormat().getChannels()));
                return true;
            }
        };
    }
}
