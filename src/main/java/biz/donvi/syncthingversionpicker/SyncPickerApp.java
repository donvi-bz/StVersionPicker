package biz.donvi.syncthingversionpicker;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import biz.donvi.syncthingversionpicker.remoteaccess.RemoteLister;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyncPickerApp extends Application {
    private static final ArrayList<Runnable> shutdownOperations = new ArrayList<>();
    private static SyncPickerApp application;

    public SyncthingScraper localSyncScraper; // FIXME: shouldn't really be public...
    public SyncthingScraper remoteSyncScraper; // FIXME: shouldn't really be public...
    public RemoteLister remoteLister; // FIXME: shouldn't really be public...

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
        Scene scene = new Scene(homeLoader.load(), 600, 400);
        stage.setTitle("Hello!");
        stage.setOnCloseRequest(event -> {
            for (Runnable action : shutdownOperations) {
                action.run();
            }
        });
        stage.setScene(scene);
        stage.show();
    }

    public static SyncPickerApp getApplication() { return application; }

    public static void registerShutdownOperation(Runnable runnable) {
        shutdownOperations.add(runnable);
    }

    public void setPickerStage() throws IOException {
        Scene scene = new Scene(pickerLoader.load(), 1000, 500);
        scene.getStylesheets().add("/biz/donvi/syncthingversionpicker/style.css");
        stage.setScene(scene);
    }

    public static void main(String[] args) {
        launch();
    }
}