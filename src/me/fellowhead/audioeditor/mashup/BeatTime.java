package me.fellowhead.audioeditor.mashup;

public class BeatTime {
    public double beats;

    public AdvancedTime advanced(double bpm, double sampleRate) {
        return new AdvancedTime(beats, bpm, sampleRate);
    }

    public AdvancedTime advanced(AdvancedTime template) {
        return new AdvancedTime(beats, template.bpm, template.sampleRate);
    }

    public BeatTime(double beats) {
        this.beats = beats;
    }

    @Override
    public String toString() {
        return "[" + beats + " beats]";
    }
}
