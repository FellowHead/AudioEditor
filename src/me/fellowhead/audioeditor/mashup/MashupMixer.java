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

    public long getLength() {
        return 100000;
    }

    public int getChannels() {
        int ch = 1;
        for (ReferenceAudioFile ref : audioSources) {
            ch = Math.max(ch, ref.getChannels());
        }
        return ch;
    }

    public MixClip[] getClips() {
        return clips.toArray(new MixClip[0]);
    }

//    public byte[] getByteData(long sampleOff) { TODO
//        final float[] data = audio.getRawData();
//        final byte[] out = new byte[(int) (data.length - sampleOff * getChannels())];
//        final int off = (int) (sampleOff * audio.getChannels());
//
////        final Random rnd = new Random();
////
////        for (long beatPos : getClickTrack(sampleOff)) {
////            if (beatPos >= 0 && beatPos+off+100 < data.length) {
////                for (int ch = 0; ch < audio.getChannels(); ch++) {
////                    for (int i = 0; i < 100; i++) {
////                        data[(int) (audio.getChannels()*beatPos + off+i + ch)] += (rnd.nextFloat() - 0.5) * 2;
////                    }
////                }
////            }
////        }
//
//        for (int i = 0; i < out.length; i++) {
//            out[i] = (byte)(0.5 * data[i + off] * 127);
//        }
//        return out;
//    }

    //not really a click track
    public long[] getClickTrack(long sampleOff) {
        ArrayList<Long> list = new ArrayList<>();
        double targetBpm = bpm;
        for (int i = 0; i < getLength() * (targetBpm / 60.0); i++) {
            list.add((long) (i * (sampleRate / (targetBpm / 60)) - sampleOff));
        }

        long[] res = new long[list.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = list.get(i);
        }
        return res;
    }

    Property toProperty(String s) {
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
