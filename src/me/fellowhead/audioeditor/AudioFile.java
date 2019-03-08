package me.fellowhead.audioeditor;

import javax.sound.sampled.*;
import java.io.IOException;
import java.util.ArrayList;

public class AudioFile {
    private float[][] data;

    public AudioFile(float[] data, int channels) {
        this.data = slice(data, channels);
    }

    public AudioFile(float[][] data) {
        this.data = data;
    }

    public int getLengthInSamples() {
        return data[0].length;
    }

    public int getChannels() {
        return data.length;
    }

    public float[] getRawData() {
        float[] out = new float[getChannels() * data[0].length];
        for (int i = 0; i < out.length; i += getChannels()) {
            for (int ch = 0; ch < getChannels(); ch++) {
                out[i + ch] = data[ch][i / getChannels()];
            }
        }
        return out;
    }

    void setData(float[][] data) {
        this.data = data;
    }

    public float[][] getData() {
        return data;
    }

    public byte[] getByteData(int bytesPerSmp) {
        final int bitMult = (int)Math.pow(2, bytesPerSmp * 8 - 1) - 1;
        float[] data = getRawData();
        byte[] out = new byte[data.length * bytesPerSmp];
        for (int i = 0; i < out.length; i += bytesPerSmp) {
            int v = (int) (data[i / bytesPerSmp] * bitMult);
            for (int b = 0; b < bytesPerSmp; b++) {
                out[i + b] = (byte)((v >> (b * 8)));
            }
        }
        return out;
    }

    public static float[][] slice(float[] samples, int channels) {
        float[][] out = new float[channels][samples.length/channels];
        for (int ch = 0; ch < channels; ch++) {
            for (int i = 0; i < samples.length / channels; i++) {
                out[ch][i] = samples[i*channels+ch];
            }
        }
        return out;
    }

    public static void checkArr(float[] arr) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != 0) {
                System.out.println(i + ": " + arr[i]);
                return;
            }
        }
        System.out.println("No values found!");
    }

    public static AudioFile fromFilePreview(AudioInputStream stream) {
        System.out.println("Reading from file (preview)...");
        ArrayList<Float> list = new ArrayList<>();
        final int readLength = 4096*2;
        try {
            long lol = 0;
            while (lol < 5000000) {
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

                lol += n2;
                //System.out.println(list.get(list.size() - 2));
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
        //checkArr(out);
        System.out.println("Done!");
        return new AudioFile(slice(out, stream.getFormat().getChannels()));
    }

    public void toMono() {
        if (getChannels() == 1) {
            return;
        }
        System.out.println("Forcing to mono");
        float[][] nData = new float[1][getData()[0].length];
        for (int i = 0; i < nData[0].length; i++) {
            float d = 0;
            for (int ch = 0; ch < getChannels(); ch++) {
                d += getData()[ch][i];
            }
            d = d / getChannels();
            nData[0][i] = d;
        }
        setData(nData);
    }

    @Deprecated
    public static AudioFile fromFile(AudioInputStream stream) {
        System.out.println("Reading from file...");
        final float sampleRate = stream.getFormat().getSampleRate();
        ArrayList<Float> list = new ArrayList<>();
        final int readLength = 4096*2;
        try {
            long lol = 0;
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
                lol += n2;
                //System.out.println(list.get(list.size() - 2));
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
        //checkArr(out);
        System.out.println("Done!");
        return new AudioFile(slice(out, stream.getFormat().getChannels()));
    }

    public float[] process(float[] samples) {
        return samples;
    }
}
