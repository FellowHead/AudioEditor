package me.fellowhead.audioeditor;

import be.tarsos.dsp.*;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

import javax.sound.sampled.*;
import java.io.*;
import java.util.ArrayList;

public class AudioUtility {
    public static AudioFile subtract(AudioFile main, AudioFile... minus) {
        main = toMono(main);
        for (int i = 0; i < minus.length; i++) {
            minus[i] = toMono(minus[i]);
        }
        float[][] out = new float[1][main.getData()[0].length];
        for (int i = 0; i < out[0].length; i++) {
            float v = main.getData()[0][i];
            v = Math.abs(v);
            for (AudioFile af : minus) {
                if (i < af.getData()[0].length) {
                    v -= Math.abs(af.getData()[0][i]);
                }
            }
            out[0][i] = v;
        }

        AudioFile file = new AudioFile(out);
        return file;
    }

    private static boolean[] createPattern(float[] data) {
        boolean[] res = new boolean[data.length];
        for (int i = 1; i < data.length; i++) {
            res[i] = data[i-1] > data[i];
        }
        return res;
    }

    private static boolean[] createPattern(float[] data, int length) {
        boolean[] res = new boolean[length];
        for (int i = 1; i < length; i++) {
            res[i] = data[i-1] > data[i];
        }
        return res;
    }

    public static int findPatternPos(float[] data, boolean[] pattern) {
        int index = 0;
        double best = 0;
        for (int i = 0; i < 100000; i++) {
            double value = patternMatch(data, pattern, i);
            if (value > best) {
                best = value;
                index = i;
                System.out.println("match at " + i + ": " + value);
            }
        }
        return index;
    }

    private static double patternMatch(float[] data, boolean[] pattern, int offset) {
        int match = 0;
        for (int i = 1; i < pattern.length; i++) {
            boolean up = data[i+offset] > data[i+offset-1];
            if (pattern[i] == up) {
                match++;
            }
        }
        return match / (double)pattern.length;
    }

    public static int findPeak(float[] data) {
        int calc = 100;
        for (int i = calc; i < 1000000; i++) {
            float avg = 0;
            for (int c = i - calc; c < i; c++) {
                avg += Math.abs(data[c]);
            }
            avg /= (float) calc;
            if (Math.abs(data[i]) > avg + 0.2f) {
                return i;
            }
        }
        return -1;
    }

    public static AudioFile shift(AudioFile f, int add) {
        System.out.println("Shifting by " + add);
        float[][] out = new float[f.getChannels()][f.getData()[0].length + add];
        for (int i = 0; i < out[0].length; i++) {
            if (i - add >= 0 && i - add < out[0].length) {
                for (int ch = 0; ch < f.getChannels(); ch++) {
                    out[ch][i] = f.getData()[ch][i - add];
                }
            }
        }
        return new AudioFile(out);
    }

    @Deprecated
    public static AudioFile smartShiftPeak(AudioFile template, AudioFile file) {
        template = toMono(template);
        file = toMono(file);
        int pos1 = findPeak(template.getData()[0]);
        int pos2 = findPeak(file.getData()[0]);
        System.out.println("Peak1 at " + pos1);
        System.out.println("Peak2 at " + pos2);

        return shift(file, pos1 - pos2);
    }


    public static AudioFile smartShift(AudioFile template, AudioFile file) {
        template = toMono(template);
        file = toMono(file);
        boolean[] pattern = createPattern(template.getData()[0], 1000);
        int pos2 = findPatternPos(file.getData()[0], pattern);
        System.out.println("Pos2 at " + pos2);

        return shift(file, -pos2);
    }

    public static AudioFile smartSubtract(AudioFile main, AudioFile minus) {
        minus = smartShift(main, minus);

        return subtract(minus, main);
    }

    public static float[] stretch(float[] in, int sampleRate, double m) throws UnsupportedAudioFileException {
        WaveformSimilarityBasedOverlapAdd wsola = new WaveformSimilarityBasedOverlapAdd(WaveformSimilarityBasedOverlapAdd.Parameters.musicDefaults(m, sampleRate));
        AudioDispatcher dispatcher = AudioDispatcherFactory.fromFloatArray(in, sampleRate, wsola.getInputBufferSize(), wsola.getOverlap());
        wsola.setDispatcher(dispatcher);
        dispatcher.addAudioProcessor(wsola);

        ArrayList<Float> list = new ArrayList<>();

        dispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                final float[] buffer = audioEvent.getFloatBuffer();
                for (float aBuffer : buffer) {
                    if (aBuffer != 0) {
                        list.add(aBuffer);
                    }
                }
                return true;
            }

            @Override
            public void processingFinished() {

            }
        });
        dispatcher.run();

        float[] res = new float[list.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = list.get(i);
        }

        return res;
    }

//    public static float[] stretch(float[] in, double m) {
//        final double frameSize = 5000 * m;
//        final double hopSize1 = 1000 / m;
//        final double hopSize2 = hopSize1 * m;
//        final int fade = 50;
//        float[] res = new float[(int) (in.length * m)];
//        final int maxHops = (int) (frameSize / hopSize2);
//
//        float[][] frames = new float[(int) (in.length / hopSize1)][(int) frameSize];
//        for (int i = 0; i < frames.length; i++) {
//            frames[i] = new float[(int) frameSize];
//            for (int j = 0; j < frameSize && (i * hopSize1 + j) < in.length; j++) {
//                frames[i][j] = in[(int) (i * hopSize1 + j)];
//            }
//        }
//
//        for (int i = 0; i < frames.length; i++) {
//            for (int j = 0; j < frameSize && i * hopSize2 + j < res.length; j++) {
//                double v = frames[i][j] * (1f - (float) j / frameSize);
//                if (j < fade) {
//                    v *= (j / (float)fade);
//                }
//                res[(int) (i * hopSize2 + j)] += v;
//            }
//        }
//
//        for (int i = 0; i < res.length; i++) {
//            res[i] = res[i] / maxHops;
//        }
//
////        for (int i = maxHops; i < (int)(res.length / hopSize2); i++) {
////            for (int j = 0; j < hopSize2 && i*hopSize1+j < in.length; j++) {
////                try {
////                    float v = 0;
////                    for (int h = 0; h < maxHops; h++) {
////                        //v += in[i] * (1 - (float)h / (maxHops - 1)) * ((int) ((i - h * m) * hopSize1 + j) / hopSize2);
////                        v += in[(int) (i*hopSize1 + j)];
////                    }
////                    v /= maxHops;
////                    res[(int) (i * hopSize2 + j)] = v;
////                } catch (Exception e) {
////                    System.out.println("lul");
////                }
////            }
////        }
//
//        return res;
//    }

    @Deprecated
    public static AudioFile toMono(AudioFile in) {
        if (in.getChannels() == 1) {
            return in;
        }
        System.out.println("Forcing to mono...");
        float[][] nData = new float[1][in.getData()[0].length];
        for (int i = 0; i < nData[0].length; i++) {
            float d = 0;
            for (int ch = 0; ch < in.getChannels(); ch++) {
                d += in.getData()[ch][i];
            }
            d = d / in.getChannels();
            nData[0][i] = d;
        }
        //AudioFile.checkArr(nData[0]);
        return new AudioFile(nData);
    }

    public static void writeToFile(AudioFile audio, AudioFormat format, File file) {
        byte[] data = audio.getByteData(format.getSampleSizeInBits() / 8);

        InputStream b_in = new ByteArrayInputStream(data);
        try {
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
            dos.write(data);
            AudioInputStream stream = new AudioInputStream(b_in, format,
                    data.length);
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, file);

            dos.close();
            System.out.println("Saved to file!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
