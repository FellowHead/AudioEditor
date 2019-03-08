package me.fellowhead.audioeditor.corrector;

import me.fellowhead.audioeditor.mashup.BeatTime;
import me.fellowhead.io.docs.PropHolder;
import me.fellowhead.io.docs.Property;
import me.fellowhead.io.docs.Savable;

public class BeatMarker extends Savable implements Comparable<BeatMarker> {
    private long samplePos;
    private double beats;

    public long getSamplePos() {
        return samplePos;
    }

    public void setSamplePos(long samplePos) {
        this.samplePos = samplePos;
    }

    public double getBeats() {
        return beats;
    }

    public void setBeats(double beats) {
        this.beats = beats;
    }

    public BeatMarker(long position) {
        this.samplePos = position;
    }

    @Override
    public int compareTo(BeatMarker o) {
        return (int) (samplePos - o.samplePos);
    }

    @Override
    protected PropHolder toProp() {
        return new PropHolder(new Property("pos", samplePos), new Property("beats", beats));
    }

    @Override
    protected void fromProp(PropHolder propHolder) {
        samplePos = propHolder.find("pos").getIntValue();
        beats = propHolder.find("beats").getIntValue();
    }
}
