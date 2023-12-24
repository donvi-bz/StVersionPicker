package biz.donvi.syncthingversionpicker.controllers;

import biz.donvi.syncthingversionpicker.SyncPickerApp;
import biz.donvi.syncthingversionpicker.SyncthingScraper;
import biz.donvi.syncthingversionpicker.remoteaccess.RemoteLister;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class HomeController {

    @FXML private StPickerComponentController localStPickerController;
    @FXML private StPickerComponentController remoteStPickerController;

    @FXML private TextField     sshUser;
    @FXML private TextField     shhAddress;
    @FXML private TextField     sshPort;
    @FXML private PasswordField sshPassword;
    @FXML private Text          sshTestBtn;
    @FXML private Text          sshTestAnswer;

    RemoteLister remoteLister;

    @FXML
    public void initialize() {
        localStPickerController.setTexts("http://127.0.0.1:8384", "shyoNqcSgEkjbJrD6WLPfa9egpj7PwuR");
        remoteStPickerController.setTexts("https://192.168.68.5:8384", "Dsi7UuwNwPzD9XfUnHPUCPjTkKcYUZza");
        clearSshTestAnswer();
    }

    @FXML
    protected void onSubmitApiKeyBtnPress() {
        @SuppressWarnings("unchecked")
        CompletableFuture<Boolean>[] futures = new CompletableFuture[]{
            localStPickerController.testSyncthingIfNecessary(),
            remoteStPickerController.testSyncthingIfNecessary(),
            testSsh()
        };
        CompletableFuture.allOf(futures).thenApplyAsync(results -> {
            try {
                if (Arrays.stream(futures).allMatch(CompletableFuture::resultNow)) {
                    // This will throw if its no good.0
                    localStPickerController.syncthingScraper.updateFolders();
                    remoteStPickerController.syncthingScraper.updateFolders();
                    // Put in global state
                    SyncPickerApp app = SyncPickerApp.getApplication();
                    app.localSyncScraper = localStPickerController.syncthingScraper;
                    app.remoteSyncScraper = remoteStPickerController.syncthingScraper;
                    app.remoteLister = remoteLister;
                    return app;
                } else if (futures[0].resultNow()) {
                    // TODO: make this better
                    localStPickerController.syncthingScraper.updateFolders();
                    SyncPickerApp app = SyncPickerApp.getApplication();
                    app.localSyncScraper = localStPickerController.syncthingScraper;
                    app.remoteSyncScraper = SyncthingScraper.EmptyScraper;
                    return app;
                } else {
                    return null;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).thenApplyAsync(app -> {
            if (app == null) return -1;
            try {
                app.setPickerStage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return 0;
        }, Platform::runLater);
    }

    @FXML
    protected void clearSshTestAnswer() {
        sshTestAnswer.setText("");
    }

    @FXML
    protected CompletableFuture<Boolean> testSsh() {
        remoteLister = new RemoteLister(
            sshUser.getText(),
            shhAddress.getText(),
            Integer.parseInt(sshPort.getText()),
            sshPassword.getText(), Path.of("")
        );
        sshTestBtn.setText("Testing...");
        sshTestBtn.setDisable(true);
        sshTestAnswer.setText("");
        return remoteLister.setupSessionAsync().thenApplyAsync(e -> {
            String message = e.isEmpty() ? "Connected" : e.get().getLocalizedMessage();
            sshTestAnswer.setText(message);
            sshTestBtn.setText("Test SSH Connection");
            sshTestBtn.setDisable(false);
            return e.isEmpty();
        }, Platform::runLater);
    }
}