package me.fellowhead.audioeditor;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

public abstract class VisualArea {
    private Canvas canvas;
    private boolean doRedraw = false;
    private boolean canRedraw = false;

    public VisualArea(Canvas canvas) {
        this.canvas = canvas;

        Pane parent = (Pane) canvas.getParent();
        canvas.widthProperty().bind(parent.widthProperty());
        canvas.heightProperty().bind(parent.heightProperty());

        canvas.widthProperty().addListener((observableValue, number, t1) -> redraw());
        canvas.heightProperty().addListener((observableValue, number, t1) -> redraw());

        canvas.setOnMousePressed(event -> {
            onMouse(event);
            foc();
        });
        canvas.setOnMouseDragged(this::onMouse);

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (doRedraw && canRedraw) {
                    doRedraw = false;
                    instantRender();
                }
            }
        }.start();
    }

    public void setRenderable(boolean v) {
        canRedraw = v;
    }

    public boolean isRenderable() {
        return canRedraw;
    }

    private void foc() {
        canvas.requestFocus();
        canvas.setFocusTraversable(false);
    }

    public void redraw() {
        doRedraw = true;
    }

    public final void instantRender() {
        doRedraw = false;
        render(canvas.getGraphicsContext2D());
    }

    protected abstract void onMouse(MouseEvent event);
    protected abstract void render(GraphicsContext g);
}
