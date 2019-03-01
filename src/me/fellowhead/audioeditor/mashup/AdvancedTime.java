package me.fellowhead.audioeditor.mashup;

public class AdvancedTime extends BeatTime {
    public double bpm;
    public double sampleRate;

    public float seconds() {
        return ((float) (beats / bpm) * 60f);
    }

    public long absolute() {
        return (long) ((beats / bpm) * 60.0 * sampleRate);
    }

    public AdvancedTime(double beats, double bpm, double sampleRate) {
        super(beats);
        this.bpm = bpm;
        this.sampleRate = sampleRate;
    }

    public AdvancedTime(long absolute, double bpm, double sampleRate) {
        super((absolute / sampleRate) * (bpm / 60));
        this.bpm = bpm;
        this.sampleRate = sampleRate;
    }

    @Override
    public String toString() {
        return "[" + beats + " beats | " + bpm + "bpm, " + sampleRate + "Hz]";
    }
}
