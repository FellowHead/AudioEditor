package me.fellowhead.audioeditor.mashup;

public class MixClip {
    private PrepAudioFile audio;
    private BeatTime startFi; // in beats
    private BeatTime length; // in beats

    private BeatTime startTl;

    public MixClip(PrepAudioFile audio, BeatTime start, BeatTime length, BeatTime position) {
        this.audio = audio;
        this.startFi = start;
        this.length = length;
        this.startTl = position;
    }

    public BeatTime getStartInTimeline() {
        return startTl;
    }

    public BeatTime getEndInTimeline() {
        return new BeatTime(startTl.beats + length.beats);
    }

    public AdvancedTime getStartInFile() {
        return startFi.advanced(audio.getBpm(), audio.getSampleRate());
    }

    public BeatTime getLength() {
        return length;
    }

    public AdvancedTime getEndInFile() {
        return new AdvancedTime(startFi.beats + length.beats, audio.getBpm(), audio.getSampleRate());
    }

    public PrepAudioFile getAudio() {
        return audio;
    }

//    public float[][] cutRelevant() {
//        float[][] out = new float[audio.getChannels()][(int) length.];
//        for (int i = 0; i < out[0].length; i++) {
//            for (int ch = 0; ch < out.length; ch++) {
//                out[ch][i] = audio.getData()[ch][(int) (i + startFi.absolute())];
//            }
//        }
//        return out;
//    }
}
