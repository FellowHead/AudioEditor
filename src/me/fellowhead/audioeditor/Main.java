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
    public static void main(String... args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        startStuff(primaryStage,"corrector/markereditor.fxml","Tempo Corrector").setOnKeyPressed(event -> CorrectorController.instance.handleKey(event));
        //startStuff(primaryStage,"mashup/mashupeditor.fxml","Mashup Mixer").setOnKeyPressed(event -> MashupController.instance.handleKey(event));
    }

    private Scene startStuff(Stage stage, String resource, String title) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(resource));
        stage.setTitle(title);
        Scene scene = new Scene(root, 800, 650);
        stage.setScene(scene);
        //primaryStage.setMaximized(true);
        stage.show();
        stage.setOnCloseRequest(windowEvent -> System.exit(0));
        return scene;
    }
}
