package me.fellowhead.audioeditor.mashup;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import me.fellowhead.audioeditor.VisualArea;

abstract class MashupTimeline extends VisualArea {
    // in beats
    double cursor;
    double scrollPos;
    double ghost;
    double beatWidth;


    MashupTimeline(Canvas canvas) {
        super(canvas);
    }
}
