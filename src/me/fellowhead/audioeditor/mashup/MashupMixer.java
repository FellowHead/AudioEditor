package me.fellowhead.audioeditor.mashup;

import me.fellowhead.audioeditor.AudioFile;
import me.fellowhead.audioeditor.AudioIOListener;
import me.fellowhead.audioeditor.ReferenceAudioFile;
import me.fellowhead.audioeditor.corrector.BeatMarker;
import me.fellowhead.io.docs.Property;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

public class MashupMixer {
    private ArrayList<PrepAudioFile> audioSources = new ArrayList<>();
    private ArrayList<MixClip> clips = new ArrayList<>();
    private double bpm = 151; //TODO
    private double sampleRate = 44100;

    public void setBpm(double bpm) {
        this.bpm = bpm;
    }

    public void addSource(PrepAudioFile file) {
        audioSources.add(file);
    }

    public void addClip(MixClip clip) {
        clips.add(clip);
    }

    public double getBpm() {
        return bpm;
    }
    public double getSampleRate() {
        return sampleRate;
    }

    public AdvancedTime getLength() {
        double end = 0;
        for (int i = 1; i < clips.size(); i++) {
            MixClip mc = clips.get(i);
            if (mc.getEndInTimeline().beats > end) {
                end = mc.getEndInTimeline().beats;
            }
        }
        return new AdvancedTime(end, bpm, sampleRate);
    }

    public int getChannels() {
        int ch = 1;
        for (ReferenceAudioFile ref : audioSources) {
            ch = Math.max(ch, ref.getChannels());
        }
        return ch;
    }

    public int getClipTrack(MixClip clip) {
        return audioSources.indexOf(clip.getAudio());
    }

    public MixClip[] getClips() {
        return clips.toArray(new MixClip[0]);
    }

    public byte[] getByteData(double start) { //TODO add timestretching on bpm differences
        AdvancedTime needed = new AdvancedTime(getLength().beats - start, bpm, sampleRate);
        final float[] data = new float[(int) (needed.absolute() * getChannels())];

        for (MixClip clip : clips) {
            if (clip.getEndInTimeline().beats > start) {
                AdvancedTime clipNeeded = new AdvancedTime(Math.min(clip.getLength().beats, clip.getEndInTimeline().beats - start), bpm, sampleRate);
                final int writeLength = (int) clipNeeded.absolute();

                final int startInOutArray = Math.max(0, (int) new AdvancedTime(clip.getStartInTimeline().beats - start, bpm, sampleRate).absolute());

                final int startInFileArray = (int) new AdvancedTime(clip.getStartInFile().beats, clip.getAudio().getBpm(), clip.getAudio().getSampleRate()).absolute();
                final int shiftedStartInFileArray = Math.max(0, (int) (startInFileArray - new AdvancedTime(clip.getStartInTimeline().beats - start, clip.getAudio().getBpm(), clip.getAudio().getSampleRate()).absolute()));

                float[][] src = clip.getAudio().getData();
                for (int i = 0; i < writeLength; i++) {
                    for (int ch = 0; ch < getChannels(); ch++) {
                        try {
                            data[(startInOutArray + i) * getChannels() + ch]
                                    += src[ch % clip.getAudio().getChannels()][shiftedStartInFileArray + i];
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println(clip + " | " + start + "");
                            return new byte[0];
                        }
                    }
                }
            }
        }

//        final Random rnd = new Random();
//
//        final int off = (int) new AdvancedTime(start, bpm, sampleRate).absolute();
//        final int offInArr = off * getChannels();
//
//        for (long beatPos : getClickTrack(off)) {
//            if (beatPos >= 0 && getChannels()*(beatPos+100)+offInArr < data.length) {
//                for (int ch = 0; ch < getChannels(); ch++) {
//                    for (int i = 0; i < 100; i++) {
//                        try {
//                            data[(int) (getChannels()*beatPos + offInArr+i + ch)] += (rnd.nextFloat() - 0.5) * 2;
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }
//        }

        final byte[] out = new byte[data.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte)(data[i] * 127);
        }
        return out;
    }

    //not really a click track
    public long[] getClickTrack(long sampleOff) {
        ArrayList<Long> list = new ArrayList<>();
        for (double i = 0; i < getLength().beats; i++) {
            list.add(new AdvancedTime(i, bpm, sampleRate).absolute() - sampleOff);
        }

        long[] res = new long[list.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = list.get(i);
        }
        return res;
    }

    Property toProperty(String s) { //TODO correct this stuff
        Property pMarkers = new Property("sources");
        for (ReferenceAudioFile src : audioSources) {
            pMarkers.add(new Property("file", src.getFile().getAbsolutePath()));
        }
        return new Property(s, pMarkers);
    }

    void fromProperty(Property property, AudioIOListener listener) {
        audioSources = new ArrayList<>();
        for (Property p : property.find("sources").getChildren()) {
            //audioSources.add(new ReferenceAudioFile(new File(p.getStringValue()), listener)); TODO
        }
    }
}
