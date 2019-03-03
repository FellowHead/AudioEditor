package me.fellowhead.audioeditor.mashup;

import me.fellowhead.audioeditor.AudioFile;
import me.fellowhead.audioeditor.ReferenceAudioFile;

public class MixClip {
    private PrepAudioFile audio;
    private AdvancedTime start; // in beats
    private AdvancedTime length; // in beats

    private BeatTime bStart;

    public MixClip(PrepAudioFile audio, BeatTime start, BeatTime length, BeatTime position) {
        this.audio = audio;
        this.start = start.advanced(audio.getBpm(), audio.getSampleRate());
        this.length = length.advanced(audio.getBpm(), audio.getSampleRate());
        this.bStart = position;
    }

    public BeatTime getStartInTimeline() {
        return bStart;
    }

    public BeatTime getEndInTimeline() {
        return new BeatTime(bStart.beats + length.beats);
    }

    public AdvancedTime getStartInFile() {
        return start;
    }

    public AdvancedTime getLength() {
        return length;
    }

    public AdvancedTime getEndInFile() {
        return new AdvancedTime(start.beats + length.beats, audio.getBpm(), audio.getSampleRate());
    }

    public PrepAudioFile getAudio() {
        return audio;
    }

    public float[][] cutRelevant() {
        float[][] out = new float[audio.getChannels()][(int) length.absolute()];
        for (int i = 0; i < out[0].length; i++) {
            for (int ch = 0; ch < out.length; ch++) {
                out[ch][i] = audio.getData()[ch][(int) (i + start.absolute())];
            }
        }
        return out;
    }
}
