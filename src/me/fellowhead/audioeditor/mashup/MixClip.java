package me.fellowhead.audioeditor.mashup;

public class MixClip {
    private PrepAudioFile audio;
    private BeatTime startFi; // in beats
    private BeatTime length; // in beats

    private BeatTime startTl;

    public MixClip(PrepAudioFile audio, BeatTime startFi, BeatTime length, BeatTime startTl) {
        this.audio = audio;
        this.startFi = startFi;
        this.length = length;
        this.startTl = startTl;
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

    public void setStartInTimeline(double beats, boolean keepEnd, boolean adjustStartInFile) {
        double diff = beats - startTl.beats;
        startTl = new BeatTime(beats);
        if (keepEnd) {
            length = new BeatTime(length.beats + diff);
        }
        if (adjustStartInFile) {
            startFi = new BeatTime(startFi.beats + diff);
        }
    }

    @Override
    public String toString() {
        return "(file: " + startFi + ", timeline: " + startTl + " | length: " + length + ")";
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
