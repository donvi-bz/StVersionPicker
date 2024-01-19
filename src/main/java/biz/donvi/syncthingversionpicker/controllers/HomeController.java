package biz.donvi.syncthingversionpicker.controllers;

import biz.donvi.syncthingversionpicker.SyncPickerApp;
import biz.donvi.syncthingversionpicker.files.Location;
import biz.donvi.syncthingversionpicker.remoteaccess.RemoteFileAccessor;
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

    RemoteFileAccessor remoteFileAccessor;

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
                    // Put in global state
                    return SyncPickerApp.getApplication().setStConnections(
                        localStPickerController.syncthingScraper,
                        remoteStPickerController.syncthingScraper,
                        remoteFileAccessor
                    );
                } else if (futures[0].resultNow()) {
                    return SyncPickerApp.getApplication().setStConnections(
                        localStPickerController.syncthingScraper,
                        null, null
                    );
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
        remoteFileAccessor = new RemoteFileAccessor(
            sshUser.getText(),
            shhAddress.getText(),
            Integer.parseInt(sshPort.getText()),
            sshPassword.getText(), Path.of(""),
            Location.RemoteCurrent
        );
        sshTestBtn.setText("Testing...");
        sshTestBtn.setDisable(true);
        sshTestAnswer.setText("");
        return remoteFileAccessor.setupSessionAsync().thenApplyAsync(e -> {
            String message = e.isEmpty() ? "Connected" : e.get().getLocalizedMessage();
            sshTestAnswer.setText(message);
            sshTestBtn.setText("Test SSH Connection");
            sshTestBtn.setDisable(false);
            return e.isEmpty();
        }, Platform::runLater);
    }
}