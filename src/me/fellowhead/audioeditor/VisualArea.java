package me.fellowhead.audioeditor;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

public abstract class VisualArea {
    private Canvas canvas;
    private boolean doRedraw = false;
    private boolean canRedraw = false;
    protected double mouseX;
    protected double mouseY;
    protected boolean isFocused;
    protected boolean isMouseDown;

    protected abstract void handleKey(KeyEvent key);

    public final void passKeyEvent(KeyEvent key) {
        if (isFocused) {
            handleKey(key);
        }
    }

    public VisualArea(Canvas canvas) {
        this.canvas = canvas;

        Pane parent = (Pane) canvas.getParent();
        canvas.widthProperty().bind(parent.widthProperty());
        canvas.heightProperty().bind(parent.heightProperty());

        canvas.widthProperty().addListener((observableValue, number, t1) -> redraw());
        canvas.heightProperty().addListener((observableValue, number, t1) -> redraw());

        canvas.setOnMousePressed(event -> {
            isMouseDown = true;
            onMouse(event);
            onMouseDown(event);
            foc();
        });
        canvas.setOnMouseReleased(mouseEvent -> {
            isMouseDown = false;
        });

        canvas.setOnMouseDragged(event -> {
            isFocused = true;
            onMouse(event);
        });
        canvas.setOnMouseMoved(mouseEvent -> {
            isFocused = true;
            mouseX = mouseEvent.getX();
            mouseY = mouseEvent.getY();
            redraw();
        });
        canvas.setOnMouseExited(mouseEvent -> {
            isFocused = false;
            redraw();
        });

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                onNextFrame();
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
        isFocused = true;
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
    protected void onNextFrame() {

    }
    protected void onMouseDown(MouseEvent event) {

    }
    protected abstract void onMouse(MouseEvent event);
    protected abstract void render(GraphicsContext g);
}
