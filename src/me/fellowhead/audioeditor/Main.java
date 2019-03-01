package me.fellowhead.audioeditor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import me.fellowhead.audioeditor.corrector.CorrectorController;
import me.fellowhead.audioeditor.mashup.MashupController;

import java.io.IOException;

public class Main extends Application {
    public static void main(String args[]) {
        launch(args);

//        try {
//            AudioFormat af = new AudioFormat(44100, 8, 1, true, false);
////
////            AudioFile main = AudioFile.fromFile(AudioSystem.getAudioInputStream(new File("C:/tmp/lbt-all.wav")));
////            AudioFile bass = AudioFile.fromFile(AudioSystem.getAudioInputStream(new File("C:/tmp/lbt-bass.wav")));
////            AudioFile melody = AudioFile.fromFile(AudioSystem.getAudioInputStream(new File("C:/tmp/lbt-1.wav")));
////            AudioFile noise = AudioFile.fromFile(AudioSystem.getAudioInputStream(new File("C:/tmp/lbt-noise.wav")));
////
////            System.out.println("Subtracting...");
////            //AudioFile file = AudioUtility.subtract(AudioUtility.subtract(AudioUtility.subtract(main, bass), melody), noise);
////            AudioFile file = AudioUtility.subtract(main, melody, bass);
////            System.out.println("Done!");
////            AudioUtility.writeToFile(file, af, new File("C:/tmp/lbt.wav"));
//
//            AudioFile main = AudioFile.fromFilePreview(AudioSystem.getAudioInputStream(new File("C:/tmp/a-all.wav")));
//            AudioFile minus = AudioFile.fromFilePreview(AudioSystem.getAudioInputStream(new File("C:/tmp/a-inst.wav")));
//            AudioFile shifted = AudioUtility.smartShift(main, minus);
//            AudioFile file = AudioUtility.subtract(main, shifted);
//
//            AudioUtility.writeToFile(file, af, new File("C:/tmp/waywf.wav"));
//
//            SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
//            byte[] data = file.get8BitData();
//
//            sdl.open(af, data.length);
//            sdl.start();
//
//
//            sdl.write(data, 0, data.length);
//        } catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        //startStuff(primaryStage,"corrector/markereditor.fxml","Tempo Corrector").setOnKeyPressed(event -> CorrectorController.instance.handleKey(event));
        startStuff(primaryStage,"mashup/mashupeditor.fxml","Mashup Mixer").setOnKeyPressed(event -> MashupController.instance.handleKey(event));
    }

    private Scene startStuff(Stage stage, String resource, String title) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(resource));
        stage.setTitle(title);
        Scene scene = new Scene(root, 800, 650);
        stage.setScene(scene);
        //primaryStage.setMaximized(true);
        stage.show();
        stage.setOnCloseRequest(windowEvent -> {
            System.exit(0);
        });
        return scene;
    }
}
