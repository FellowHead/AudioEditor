package me.fellowhead.audioeditor.corrector;

import javafx.concurrent.Task;
import me.fellowhead.audioeditor.AudioFile;
import me.fellowhead.audioeditor.AudioIOListener;
import me.fellowhead.audioeditor.AudioUtility;
import me.fellowhead.audioeditor.ReferenceAudioFile;
import me.fellowhead.io.docs.Property;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class TimingCorrector {
    public ReferenceAudioFile audio;
    private ArrayList<BeatMarker> markers = new ArrayList<>();

    public BeatMarker[] getMarkers() {
        return markers.toArray(new BeatMarker[0]);
    }

    public void addMarker(BeatMarker marker, boolean autoBeats) {
        markers.add(marker);
        sort();
        if (autoBeats) {
            int index = markers.indexOf(marker);
            if (index > 0 && index < markers.size() - 1) {
                BeatMarker previous = markers.get(index - 1);
                BeatMarker next = markers.get(index + 1);
                marker.setBeats((int) (previous.getBeats() + Math.round(next.getBeats() - previous.getBeats()) *
                        ((marker.getSamplePos() - previous.getSamplePos()) / (double)(next.getSamplePos() - previous.getSamplePos()))));
            } else if (index == 0) {
                setRelativeBeats(marker, 0);
            } else if (markers.size() > 2 && index == markers.size() - 1) {
                BeatMarker a = markers.get(index - 2);
                BeatMarker b = markers.get(index - 1);
                double bpm = calcBpm(a, b, audio.getSampleRate());
                int beats = (int) Math.round(bpm * (marker.getSamplePos() - b.getSamplePos()) / audio.getSampleRate() / 60);
                setRelativeBeats(marker, beats);
            }
        }
    }

    private BeatMarker getPrevious(BeatMarker ref) {
        sort();
        int index = markers.indexOf(ref) - 1;
        if (index < 0) {
            return null;
        }
        return markers.get(index);
    }

    public void setRelativeBeats(BeatMarker ref, int beats) {
        BeatMarker prev = getPrevious(ref);
        if (prev != null) {
            ref.setBeats(prev.getBeats() + beats);
        } else {
            ref.setBeats(beats);
        }
        sort();
    }

    public int getRelativeBeats(BeatMarker ref) {
        BeatMarker prev = getPrevious(ref);
        if (prev != null) {
            return ref.getBeats() - prev.getBeats();
        }
        return ref.getBeats();
    }

    public double calcAverageBpm() {
        double res = 0;
        for (int i = 1; i < markers.size(); i++) {
            res += calcBpm(markers.get(i-1), markers.get(i), audio.getSampleRate());
        }
        return res / markers.size();
    }

    public interface CorrectionListener extends AudioIOListener {
        void onCorrected(AudioFile file);
    }

    public void createCorrectedAudioFx(double bpm, CorrectionListener listener) {
        Task task = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                sort();
                float[][] res = new float[audio.getChannels()][(int) (audio.getSampleRate() * 60.0 * (markers.get(markers.size()-1).getBeats() - markers.get(0).getBeats()) / bpm)];
                for (int i = 1; i < markers.size(); i++) {
                    for (int ch = 0; ch < audio.getChannels(); ch++) {
                        BeatMarker a = markers.get(i - 1);
                        BeatMarker b = markers.get(i);
                        int posA = (int) (audio.getSampleRate() * 60.0 * a.getBeats() / bpm);
                        int posB = (int) (audio.getSampleRate() * 60.0 * b.getBeats() / bpm);

                        float[] src = new float[(int) (b.getSamplePos() - a.getSamplePos() + 2000)];
                        for (int j = 0; j < src.length; j++) {
                            src[j] = audio.getData()[ch][(int) (j + a.getSamplePos())];
                        }
                        float[] stretched = AudioUtility.stretch(src, (int) audio.getSampleRate(), (double) (b.getSamplePos() - a.getSamplePos()) / (posB - posA));
                        for (int j = 0; j < stretched.length; j++) {
                            int pos = posA + j;
                            if (pos < res[ch].length) {
                                res[ch][pos] = stretched[j];
                            }
                        }
                    }
                    updateProgress(i, markers.size());
                }

//        Random rnd = new Random();
//        float[] res = new float[(int) ((65 * 60.0 / bpm) * audio.getSampleRate())];
//        for (int i = 0; i < 64; i++) {
//            for (int j = 0; j < 10000; j++) {
//                res[(int) (j + i * (60.0 / bpm) * audio.getSampleRate())] = (1f - j / 10000f) * (rnd.nextFloat() - 0.5f);
//            }
//        }

                listener.onCorrected(new AudioFile(res));
                return true;
            }
        };
        new Thread(task).start();
        listener.taskCreated(task);
    }

    @Deprecated
    public AudioFile createCorrectedAudio(double bpm) throws UnsupportedAudioFileException {
        sort();
        float[][] res = new float[audio.getChannels()][(int) (audio.getSampleRate() * 60.0 * (markers.get(markers.size()-1).getBeats() - markers.get(0).getBeats()) / bpm)];
        for (int i = 1; i < markers.size(); i++) {
            for (int ch = 0; ch < audio.getChannels(); ch++) {
                BeatMarker a = markers.get(i - 1);
                BeatMarker b = markers.get(i);
                int posA = (int) (audio.getSampleRate() * 60.0 * a.getBeats() / bpm);
                int posB = (int) (audio.getSampleRate() * 60.0 * b.getBeats() / bpm);

                float[] src = new float[(int) (b.getSamplePos() - a.getSamplePos() + 2000)];
                for (int j = 0; j < src.length; j++) {
                    src[j] = audio.getData()[ch][(int) (j + a.getSamplePos())];
                }
                float[] stretched = AudioUtility.stretch(src, (int) audio.getSampleRate(), (double) (b.getSamplePos() - a.getSamplePos()) / (posB - posA));
                for (int j = 0; j < stretched.length; j++) {
                    int pos = posA + j;
                    if (pos < res[ch].length) {
                        res[ch][pos] = stretched[j];
                    }
                }
            }
            System.out.println("Progress: " + (Math.round(10000f * i / markers.size()) / 100f) + "%");
        }

//        Random rnd = new Random();
//        float[] res = new float[(int) ((65 * 60.0 / bpm) * audio.getSampleRate())];
//        for (int i = 0; i < 64; i++) {
//            for (int j = 0; j < 10000; j++) {
//                res[(int) (j + i * (60.0 / bpm) * audio.getSampleRate())] = (1f - j / 10000f) * (rnd.nextFloat() - 0.5f);
//            }
//        }

        System.out.println("Done!");

        return new AudioFile(res);
    }

//    public AudioFile createCorrectedAudio(double bpm) throws UnsupportedAudioFileException {
//        sort();
//        ArrayList<Float> arr = new ArrayList<>();
//        if (false) {
//            for (int i = 0; i < markers.get(0).getSamplePos(); i++) {
//                arr.add(audio.getData()[0][i]);
//            }
//        }
//        for (int i = 1; i < markers.size(); i++) {
//            BeatMarker a = markers.get(i-1);
//            BeatMarker b = markers.get(i);
//
//            float[] src = new float[(int) (b.getSamplePos()-a.getSamplePos())];
//            for (int j = 0; j < src.length; j++) {
//                src[j] = audio.getData()[0][(int) (j+a.getSamplePos())];
//            }
//            float[] stretched = AudioUtility.stretch(src, (int) audio.getSampleRate(), bpm / calcBpm(a,b,audio.getSampleRate()));
//            for (float aStretched : stretched) {
//                arr.add(aStretched);
//            }
//            System.out.println("Progress: " + (Math.round(10000f * i / markers.size()) / 100f) + "%");
//        }
//
//        float[] res = new float[arr.size()];
//        for (int i = 0; i < arr.size(); i++) {
//            res[i] = arr.get(i);
//        }
//        System.out.println("Done!");
//
//        return new AudioFile(res,1);
//    }

    public void removeMarker(BeatMarker marker) {
        markers.remove(marker);
        sort();
    }

    public void sort() {
        Collections.sort(markers);
    }

    public static double calcBpm(BeatMarker a, BeatMarker b, float sampleRate) {
        return 60.0 * (b.getBeats() - a.getBeats()) / ((b.getSamplePos() - a.getSamplePos()) / sampleRate);
    }

    //not really a click track
    public long[] getClickTrack(long sampleOff) {
        if (markers.size() == 0) {
//            long[] res = new long[(int) ((audio.getLength() / audio.getSampleRate()) * (targetBpm / 60.0))];
//            for (int i = 0; i < res.length; i++) {
//                res[i] = (long) (i * (audio.getSampleRate() / (targetBpm / 60)));
//            }
//            return res;
            return new long[0];
        } else if (markers.size() == 1) {
            return new long[] { markers.get(0).getSamplePos() - sampleOff };
        }

        sort();
        ArrayList<Long> list = new ArrayList<>();
        list.add(markers.get(0).getSamplePos() - sampleOff);
        for (int i = 1; i < markers.size(); i++) {
            BeatMarker a = markers.get(i-1);
            BeatMarker b = markers.get(i);
            for (int j = 1; j <= b.getBeats() - a.getBeats(); j++) {
                list.add((long) (a.getSamplePos() + (b.getSamplePos() - a.getSamplePos()) * (j / (float)(b.getBeats() - a.getBeats()))) - sampleOff);
            }
        }
        BeatMarker a = markers.get(markers.size()-2);
        BeatMarker b = markers.get(markers.size()-1);
        double targetBpm = calcBpm(a, b, audio.getSampleRate());
        for (int i = 0; i < ((audio.getLength() - markers.get(markers.size()-1).getSamplePos()) / audio.getSampleRate()) * (targetBpm / 60.0); i++) {
            list.add((long) (i * (audio.getSampleRate() / (targetBpm / 60)) + markers.get(markers.size()-1).getSamplePos()) - sampleOff);
        }

        long[] res = new long[list.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = list.get(i);
        }
        return res;
    }

    public byte[] getByteData(long sampleOff) {
        final float[] data = audio.getRawData();
        final byte[] out = new byte[(int) (data.length - sampleOff * audio.getChannels())];
        final int off = (int) (sampleOff * audio.getChannels());
        final Random rnd = new Random();

        for (long beatPos : getClickTrack(sampleOff)) {
            if (beatPos >= 0 && beatPos+off+100 < data.length) {
                for (int ch = 0; ch < audio.getChannels(); ch++) {
                    for (int i = 0; i < 100; i++) {
                        data[(int) (audio.getChannels()*beatPos + off+i + ch)] += (rnd.nextFloat() - 0.5) * 2;
                    }
                }
            }
        }

        for (int i = 0; i < out.length; i++) {
            out[i] = (byte)(data[i + off] * 127);
        }
        return out;
    }

    public TimingCorrector(ReferenceAudioFile file) {
        this.audio = file;
    }

    public Property toProperty(String s) {
        Property pMarkers = new Property("markers");
        for (BeatMarker bm : markers) {
            pMarkers.add(bm.toProperty("marker"));
        }
        return new Property(s, pMarkers, new Property("file", audio.getFile().getAbsolutePath()));
    }

    public void fromProperty(Property property, AudioIOListener listener) {
        markers = new ArrayList<>();
        for (Property p : property.find("markers").getChildren()) {
            BeatMarker bm = new BeatMarker(0);
            bm.fromProperty(p);
            markers.add(bm);
        }

        audio = new ReferenceAudioFile(new File(property.find("file").getStringValue()), listener);
    }
}
