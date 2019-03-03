package me.fellowhead.audioeditor.corrector;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import me.fellowhead.audioeditor.*;
import me.fellowhead.io.docs.Document;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class CorrectorController {
    @FXML
    private ProgressBar progressBar;
    @FXML
    private TextField fieldBeats;
    @FXML
    private Canvas canvas;
    private VisualArea area;

    private TimingCorrector corrector;

    private float zoom = 5000;
    private int cursor = 0;
    private int scrollPos = 0;
    private int ghost = 0;
    private boolean playing = false;
    private long offLul = 0;
    private BeatMarker selected = null;

    public static CorrectorController instance;
    private static SourceDataLine sdl;
    private final int buffer = 50000;
    private Thread thread;

    public void initialize() {
        instance = this;

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (sdl != null && sdl.isRunning()) {
                    ghost = (int) (sdl.getFramePosition() + offLul);
                    area.redraw();

                    if (ghost >= corrector.audio.getLength()) {
                        setPlaying(false);
                    }
                }
            }
        }.start();

        fieldBeats.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                int v = Integer.parseInt(newValue);
                if (selected != null) {
                    corrector.setRelativeBeats(selected, v);
                    area.redraw();
                }
            } catch (NumberFormatException ignored) {
            }
        });

        area = new VisualArea(canvas) {
            @Override
            protected void onMouse(MouseEvent event) {
                if (event.isControlDown() && selected != null) {
                    setCursor((int) (event.getX() * zoom + getScroll()), false);
                    selected.setSamplePos(cursor);
                    corrector.sort();
                    redraw();
                } else {
                    setCursor((int) (event.getX() * zoom + getScroll()), !event.isAltDown());
                }
            }

            @Override
            protected void render(GraphicsContext g) {
                if (corrector == null) {
                    return;
                }

                int scroll = getScroll();

                int start = (int)(scroll - zoom * canvas.getWidth());
                int end = (int)(scroll + zoom * canvas.getWidth());
                AudioFile audio = corrector.audio;

                int x1 = (int) ((start - scroll) / zoom);
                int x2 = (int) ((end - scroll) / zoom);

                g.setFill(Color.LIGHTSLATEGRAY);
                g.fillRect(x1,0,x2 - x1, canvas.getHeight());

                g.setStroke(Color.DODGERBLUE);
                g.setLineWidth(1);

                for (int i = x1; i < x2; i += 1) {
                    int last = (int) ((i-1)*zoom + scroll);
                    int index = (int) (i*zoom + scroll);
                    float vol = 0;
                    for (int j = last; j < index; j++) {
                        if (j >= 0 && j < audio.getLength() && Math.abs(audio.getData()[0][j]) > vol) {
                            vol = Math.abs(audio.getData()[0][j]);
                        }
                    }
                    //vol = vol / (index - last);
                    double y = vol;
                    g.strokeLine((double) i, canvas.getHeight()*0.5 * (1+y), (double) i, canvas.getHeight()*0.5 * (1-y));
                }

                g.setStroke(new Color(1,1,1,0.4));
                g.setLineWidth(1);
                for (long bPos : corrector.getClickTrack(scroll)) {
                    double x = bPos / zoom;
                    g.strokeLine(x,0, x, canvas.getHeight());
                }

                g.setLineWidth(5);
                for (BeatMarker marker : corrector.getMarkers()) {
                    g.setStroke((marker == selected) ? Color.DEEPPINK : Color.INDIGO);
                    double x = (marker.getSamplePos() - scroll) / zoom;
                    g.strokeLine(x,0,x,canvas.getHeight());
                }

                g.setStroke(Color.BLACK);
                g.setLineWidth(1);
                g.strokeLine((cursor - scroll) / zoom,0,(cursor - scroll) / zoom, canvas.getHeight());

                if (isPlaying() && ghost >= 0) {
                    g.setStroke(Color.GREENYELLOW);
                    g.setLineWidth(2);
                    g.strokeLine((ghost - scroll) / zoom,0,(ghost - scroll) / zoom, canvas.getHeight());
                }

                g.setFill(Color.WHITE);
                for (int i = 0; i < corrector.getMarkers().length-1; i++) {
                    BeatMarker a = corrector.getMarkers()[i];
                    BeatMarker b = corrector.getMarkers()[i+1];
                    double x = (a.getSamplePos() + 0.5 * (b.getSamplePos() - a.getSamplePos()) - scroll) / zoom;
                    g.setTextAlign(TextAlignment.CENTER);
                    g.fillText((Math.round(TimingCorrector.calcBpm(a, b, corrector.audio.getSampleRate()) * 10) / 10.0) + "", x, 50);
                    g.setTextAlign(TextAlignment.LEFT);
                    g.fillText(a.getBeats() + "", (a.getSamplePos() - scroll) / zoom + 5, canvas.getHeight() - 5);
                }
            }
        };
    }

    private void reset() {
        zoom = 5000;
        cursor = 0;
        scrollPos = 0;
        ghost = 0;

        resetSdl();
    }

    private void resetSdl() {
        AudioFormat af = new AudioFormat(44100, 8, corrector.audio.getChannels(), true, false);

        try {
            sdl = AudioSystem.getSourceDataLine(af);

            try {
                sdl.open(af, buffer);
            } catch (LineUnavailableException e) {
                e.printStackTrace();
            }

            sdl.open(af, buffer);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private AudioIOListener createImportListener() {
        area.setRenderable(false);
        return task -> {
            bindProgressBar(task);
            task.setOnSucceeded(event -> {
                System.out.println("Loaded");
                unbindProgressBar();
                area.setRenderable(true);
                reset();
                area.redraw();
            });
        };
    }

    private void unbindProgressBar() {
        progressBar.progressProperty().unbind();
        progressBar.setProgress(0);
    }

    private void bindProgressBar(Task task) {
        Platform.runLater(() -> progressBar.progressProperty().bind(task.progressProperty()));
    }

    private void setZoom(float zoom) {
        if (this.zoom <= 200) {
            zoom = this.zoom + (zoom - this.zoom) * 0.1f;
        }
        this.zoom = zoom;
        area.redraw();
    }

    private void saveToFile(File file) throws IOException {
        System.out.println("Saving...");

        Document doc = new Document();
        doc.add(corrector.toProperty("corrector"));
        doc.saveToFile(file);

        System.out.println("Saved to " + file.getAbsolutePath());
    }

    private void saveDialog() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("BeatMarker file", "*.brk"));
        File file = chooser.showSaveDialog(canvas.getScene().getWindow());
        if (file != null) {
            try {
                saveToFile(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadFromFile(File file) throws IOException {
        System.out.println("Loading...");

        Document doc = Document.read(file);
        corrector = new TimingCorrector(null);
        corrector.fromProperty(doc.find("corrector"), createImportListener());
    }

    private void loadDialog() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("BeatMarker file", "*.brk"));
        File file = chooser.showOpenDialog(canvas.getScene().getWindow());
        if (file != null) {
            try {
                loadFromFile(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void handleKey(KeyEvent key) {
        if (key.isControlDown()) {
            switch (key.getCode()) {
                case O:
                    loadDialog();
                    break;
            }
        }

        if (!(key.getTarget() instanceof Canvas)) {
            return;
        }

        if (key.isControlDown()) {
            switch (key.getCode()) {
                case S:
                    saveDialog();
                    break;
            }
        } else {
            switch (key.getCode()) {
                case PLUS:
                    setZoom(zoom - 100); break;
                case MINUS:
                    setZoom(zoom + 100); break;
                case D:
                    setCursor((int) (cursor + zoom), false);
                    restartPlaying(50);
                    break;
                case A:
                    setCursor((int) (cursor - zoom), false);
                    restartPlaying(50);
                    break;
                case S:
                    restartPlaying(250); break;
                case ENTER:
                    if (playing && ghost >= 0) {
                        setCursor(ghost, false);
                    }
                    setPlaying(!playing); break;
                case SPACE:
                    setPlaying(!playing); break;
                case M:
                    addMarker(cursor); break;
                case RIGHT:
                    setScrollPos((int) (getScroll() + zoom * 15)); break;
                case LEFT:
                    setScrollPos((int) (getScroll() - zoom * 15)); break;
            }
        }
    }

    private void restartPlaying(long ms) {
        setPlaying(false);
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        setPlaying(true);
    }

    private void addMarker(long pos) {
        BeatMarker marker = new BeatMarker(pos);
        corrector.addMarker(marker, true);
        select(marker);
        area.redraw();
    }

    private void select(BeatMarker marker) {
        selected = marker;
        if (selected != null) {
            fieldBeats.setText("" + corrector.getRelativeBeats(selected));
            fieldBeats.setDisable(false);
        } else {
            fieldBeats.setText("");
            fieldBeats.setDisable(true);
        }
        area.redraw();
    }

    private boolean isPlaying() {
        return playing;
    }

    private void setPlaying(boolean playing) {
        this.playing = playing;

        if (playing) {
            if (thread != null) {
                thread.interrupt();
            }
            thread = new Thread(() -> {
                offLul = -sdl.getLongFramePosition() + cursor;
                final byte[] data = corrector.getByteData(cursor);
                sdl.start();

                int i = 0;
                while (!Thread.currentThread().isInterrupted() && (i+1) * buffer < data.length) {
                    sdl.write(data, i * buffer, buffer);
                    i++;
                }
                if (!Thread.currentThread().isInterrupted()) {
                    sdl.write(data, i * buffer, data.length - i * buffer);
                }
            });
            thread.start();
        } else {
            ghost = -1;
            area.redraw();

            if (thread != null) {
                thread.interrupt();
            }

            if (sdl != null)  {
                sdl.stop();
                sdl.close();
                resetSdl();
                System.out.println("reset Kappa");
            }
        }
    }

    private int getScroll() {
        return (int) (((this.scrollPos / zoom) - canvas.getWidth() * 0.5) * zoom);
    }

    private void setScroll(int scroll) {
        scrollPos = (int) (((scroll / zoom) + canvas.getWidth() * 0.5) * zoom);
    }

    private void setScrollPos(int scrollPos) {
        setScroll(scrollPos);
        area.redraw();
    }

    public void exportFile() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("wav only lul", "*.wav"));
        File file = chooser.showSaveDialog(canvas.getScene().getWindow());
        if (file != null) {
            double bpm = corrector.calcAverageBpm();
            System.out.println("Average bpm: " + bpm);
            bpm = Math.round(bpm * 1) / 1;
            System.out.println("Exporting at " + bpm);
            corrector.createCorrectedAudioFx(bpm, new TimingCorrector.CorrectionListener() {
                @Override
                public void onCorrected(AudioFile audioFile) {
                    AudioUtility.writeToFile(audioFile, new AudioFormat(corrector.audio.getSampleRate(), corrector.audio.getBytesPerSmp() * 8, audioFile.getChannels(), true, false), file);
                }

                @Override
                public void taskCreated(Task task) {
                    bindProgressBar(task);
                    task.setOnSucceeded(event -> {
                        System.out.println("Corrected");
                        unbindProgressBar();
                    });
                }
            });
        }
    }

    public void importFile() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("wav only lul", "*.wav", "*.aac"));
        File file = chooser.showOpenDialog(canvas.getScene().getWindow());
        if (file != null) {
            ReferenceAudioFile audio = new ReferenceAudioFile(file, createImportListener());
            corrector = new TimingCorrector(audio);
        }
    }

    private void setCursor(int cursor, boolean fitToMarker) {
        int lastCursor = this.cursor;

        if (fitToMarker && corrector != null) {
            BeatMarker found = null;
            for (BeatMarker marker : corrector.getMarkers()) {
                long lol = Math.abs(marker.getSamplePos() - cursor);
                if (lol < zoom * 25) {
                    if (found == null || lol < Math.abs(found.getSamplePos() - cursor)) {
                        found = marker;
                    }
                }
            }
            if (found != null) {
                cursor = (int) found.getSamplePos();
            }
            select(found);
        }
        cursor = Math.max(cursor, 0);
        this.cursor = cursor;

        double border = 0;
        double x = (double) (cursor - getScroll()) / zoom;
        if (cursor < lastCursor && x < border) {
            setScrollPos(cursor - (int)(border * zoom));
        } else if (cursor > lastCursor && x > canvas.getWidth() - border) {
            setScrollPos(cursor - (int)((canvas.getWidth() - border) * zoom));
        }

        area.redraw();
    }

    private void deleteMarker(BeatMarker marker) {
        corrector.removeMarker(marker);
        select(null);
        area.redraw();
    }

    public void deleteSelected() {
        if (selected != null) {
            deleteMarker(selected);
        }
    }
}
