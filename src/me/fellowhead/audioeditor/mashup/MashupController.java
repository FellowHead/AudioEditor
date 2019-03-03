package me.fellowhead.audioeditor.mashup;

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
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import me.fellowhead.audioeditor.*;
import me.fellowhead.io.docs.Document;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MashupController {
    @FXML
    private ProgressBar progressBar;
    @FXML
    private TextField fieldBeats;
    @FXML
    private Canvas canvas;

    private MashupMixer mixer = new MashupMixer();

    private double beatWidth;
    private double cursor = 0; // in beats
    private double scrollPos = 0; // in beats
    private int ghost = 0;
    private boolean playing = false;
    private long offLul = 0;
    private ArrayList<MixClip> selected = new ArrayList<>();

    public static MashupController instance;
    private static SourceDataLine sdl;
    private final int buffer = 50000;
    private Thread thread;

    private VisualArea mixingArea;

    public void initialize() {
        instance = this;

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (sdl != null && sdl.isRunning()) {
                    ghost = (int) (sdl.getFramePosition() + offLul);
                    mixingArea.redraw();

                    if (ghost >= mixer.getLength()) {
                        setPlaying(false);
                    }
                }
            }
        }.start();

        mixingArea = new VisualArea(canvas) {
            @Override
            protected void onMouse(MouseEvent event) {

            }

            @Override
            protected void render(GraphicsContext g) {
                if (mixer == null) {
                    return;
                }

                double scroll = getScrollCentered();

                g.setFill(Color.LIGHTSLATEGRAY);
                g.fillRect(0,0,canvas.getWidth(), canvas.getHeight()); // paint the background

                double trackHeight = 50;

                g.setStroke(new Color(1,1,1,0.4));
                g.setLineWidth(1);
                for (int i = 0; i < 128; i++) { //TODO calculate actual needed iterations
                    double x = (i - scroll) * beatWidth;
                    g.strokeLine(x,0, x, canvas.getHeight());
                }

                g.setFill(Color.INDIANRED);

                int i = 0;
                for (MixClip clip : mixer.getClips()) {
                    g.setFill(Color.hsb(30 * i++, 1, 0.9));
                    g.fillRect((clip.getStartInTimeline().beats - scroll) * beatWidth, mixer.getClipTrack(clip) * trackHeight, clip.getLength().beats * beatWidth, trackHeight);
                }

                g.setStroke(Color.BLACK);
                g.setLineWidth(1);
                g.strokeLine((cursor - scroll) * beatWidth,0,(cursor - scroll) * beatWidth, canvas.getHeight());

                g.setFill(Color.PAPAYAWHIP);
                g.fillText(scrollPos + " / centered: " + getScrollCentered() + " | " + cursor, 5, 40);

//        if (isPlaying() && ghost >= 0) {
//            g.setStroke(Color.GREENYELLOW);
//            g.setLineWidth(2);
//            g.strokeLine((ghost - scroll) * beatWidth,0,(ghost - scroll) * beatWidth, canvas.getHeight());
//        }
            }
        };
    }

    private void reset() {
        beatWidth = 100;
        cursor = 0;
        scrollPos = 0;
        ghost = 0;

        resetSdl();
    }

    private void resetSdl() {
        AudioFormat af = new AudioFormat(44100, 8, mixer.getChannels(), true, false);

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
        mixingArea.setRenderable(false);
        return task -> {
            bindProgressBar(task);
            task.setOnSucceeded(event -> {
                System.out.println("Loaded");
                unbindProgressBar();
                mixingArea.setRenderable(true);
                reset();
                mixingArea.redraw();
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

    private void setBeatWidth(double beatWidth) {
        this.beatWidth = beatWidth;
        mixingArea.redraw();
    }

    private void saveToFile(File file) throws IOException {
        System.out.println("Saving...");

        Document doc = new Document();
        doc.add(mixer.toProperty("mixer"));
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
        mixer = new MashupMixer();
        mixer.fromProperty(doc.find("mixer"), createImportListener());
    }

    private void loadDialog() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("BeatMarker file", "*.schmup")); //TODO loading a file
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
                    setBeatWidth(beatWidth + 1); break;
                case MINUS:
                    setBeatWidth(beatWidth - 1); break;
                case ENTER:
                    if (playing && ghost >= 0) {
                        setCursor(ghost, false);
                    }
                    setPlaying(!playing); break;
                case SPACE:
                    setPlaying(!playing); break;
//                case M:
//                    addMarker(cursor); break; TODO allow user to add a clip
                case RIGHT:
                    setScrollPos(scrollPos + 1); break;
                case LEFT:
                    setScrollPos(scrollPos - 1); break;
            }
        }
    }

//    private void select(BeatMarker marker) { TODO select clips
//        selected = marker;
//        if (selected != null) {
//            fieldBeats.setText("" + mixer.getRelativeBeats(selected));
//            fieldBeats.setDisable(false);
//        } else {
//            fieldBeats.setText("");
//            fieldBeats.setDisable(true);
//        }
//        redraw();
//    }

    private boolean isPlaying() {
        return playing;
    }

    private void setPlaying(boolean playing) {
        this.playing = playing;

        if (playing) {
            if (thread != null) {
                thread.interrupt();
            }
//            thread = new Thread(() -> { TODO play audio
//                offLul = -sdl.getLongFramePosition() + cursor;
//                final byte[] data = mixer.getByteData(cursor);
//                sdl.start();
//
//                int i = 0;
//                while (!Thread.currentThread().isInterrupted() && (i+1) * buffer < data.length) {
//                    sdl.write(data, i * buffer, buffer);
//                    i++;
//                }
//                if (!Thread.currentThread().isInterrupted()) {
//                    sdl.write(data, i * buffer, data.length - i * buffer);
//                }
//            });
//            thread.start();
        } else {
            ghost = -1;
            mixingArea.redraw();

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

    private double getScrollCentered() {
        return ((this.scrollPos * beatWidth) - canvas.getWidth() * 0.5) / beatWidth;
    }

    private void setScrollPos(double scrollPos) {
        this.scrollPos = scrollPos;
        mixingArea.redraw();
    }

    public void exportFile() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("wav only lul", "*.wav"));
        File file = chooser.showSaveDialog(canvas.getScene().getWindow());
        if (file != null) {
//            double bpm = mixer.calcAverageBpm(); TODO export the mash up
//            System.out.println("Average bpm: " + bpm);
//            bpm = Math.round(bpm * 1) / 1;
//            System.out.println("Exporting at " + bpm);
//            mixer.createCorrectedAudioFx(bpm, new TimingCorrector.CorrectionListener() {
//                @Override
//                public void onCorrected(AudioFile audioFile) {
//                    AudioUtility.writeToFile(audioFile, new AudioFormat(mixer.audio.getSampleRate(), mixer.audio.getBytesPerSmp() * 8, audioFile.getChannels(), true, false), file);
//                }
//
//                @Override
//                public void taskCreated(Task task) {
//                    bindProgressBar(task);
//                    task.setOnSucceeded(event -> {
//                        System.out.println("Corrected");
//                        unbindProgressBar();
//                    });
//                }
//            });
        }
    }

    public void importFile() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("wav only lul", "*.wav"));
        File file = chooser.showOpenDialog(canvas.getScene().getWindow());
        if (file != null) {
            PrepAudioFile audio = new PrepAudioFile(file, createImportListener(), 130);
            mixer.addSource(audio);
            mixer.addClip(new MixClip(audio, new BeatTime(0), new BeatTime(8), new BeatTime(0)));
            mixer.addClip(new MixClip(audio, new BeatTime(8), new BeatTime(4), new BeatTime(8)));
        }
    }

    private void setCursor(double cursor, boolean fitToMarker) {
        double lastCursor = this.cursor;

        if (fitToMarker && mixer != null) {
//           BeatMarker found = null; TODO fit cursor to clip
//            for (BeatMarker marker : mixer.getMarkers()) {
//                long lol = Math.abs(marker.getSamplePos() - cursor);
//                if (lol < beatWidth * 25) {
//                    if (found == null || lol < Math.abs(found.getSamplePos() - cursor)) {
//                        found = marker;
//                    }
//                }
//            }
//            if (found != null) {
//                cursor = (int) found.getSamplePos();
//            }
            //select(found);
        }
        //cursor = Math.max(cursor, 0);
        this.cursor = cursor;

//        double border = 0; //TODO border scroll
//        double x = (double) (cursor - getScrollCentered()) * beatWidth;
//        if (cursor < lastCursor && x < border) {
//            setScrollPos(cursor - (int)(border / beatWidth));
//        } else if (cursor > lastCursor && x > canvas.getWidth() - border) {
//            setScrollPos(cursor - (int)((canvas.getWidth() - border) / beatWidth));
//        }

        mixingArea.redraw();
    }
}
