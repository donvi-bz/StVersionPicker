package biz.donvi.syncthingversionpicker;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SyncPickerApp extends Application {

    private static SyncPickerApp application;

    SyncthingScraper syncScraper;

    private FXMLLoader pickerLoader;
    private Stage stage;

    @Override
    public void start(Stage stage) throws IOException {
        SyncPickerApp.application = this;
        this.stage = stage;
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        FXMLLoader homeLoader = new FXMLLoader(SyncPickerApp.class.getResource("home-view.fxml"));
        pickerLoader = new FXMLLoader(SyncPickerApp.class.getResource("picker-view.fxml"));
        Scene scene = new Scene(homeLoader.load(), 320, 240);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    public static SyncPickerApp getApplication() { return application; }

    public void setPickerStage() throws IOException {
        stage.setScene(new Scene(pickerLoader.load(), 320, 240));
    }

    public static void main(String[] args) {
        launch();
    }
}