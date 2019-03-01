package me.fellowhead.audioeditor.corrector;

import me.fellowhead.io.docs.Property;
import me.fellowhead.io.docs.Savable;

public class BeatMarker implements Comparable<BeatMarker>, Savable {
    private long samplePos;
    private int beats;

    public long getSamplePos() {
        return samplePos;
    }

    public void setSamplePos(long samplePos) {
        this.samplePos = samplePos;
    }

    public int getBeats() {
        return beats;
    }

    public void setBeats(int beats) {
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
    public Property toProperty(String s) {
        return new Property(s, new Property("pos", samplePos), new Property("beats", beats));
    }

    @Override
    public void fromProperty(Property property) {
        //TODO
        samplePos = property.find("pos").getIntValue();
        beats = property.find("beats").getIntValue();
    }
}
