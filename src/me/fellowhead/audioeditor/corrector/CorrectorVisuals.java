package me.fellowhead.audioeditor.corrector;

import javafx.scene.canvas.Canvas;
import me.fellowhead.audioeditor.VisualArea;

abstract class CorrectorVisuals extends VisualArea {
    float zoom = 5000; //TODO convert to a width for better zooming
    int cursor = 0;
    int scrollPos = 0;
    int ghost = 0;

    public CorrectorVisuals(Canvas canvas) {
        super(canvas);
    }
}
