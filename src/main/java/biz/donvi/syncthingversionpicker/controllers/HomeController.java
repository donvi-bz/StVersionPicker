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
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

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
        localStPickerController.setTexts("http://127.0.0.1:8384", "");
        remoteStPickerController.setTexts("https://192.168.68.5:8384", "Dsi7UuwNwPzD9XfUnHPUCPjTkKcYUZza");
        readSettingsFile();
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
                    // Save state
                    writeSettingsFile();
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

    private final Path stvpHome = Path.of(System.getProperty("user.home"), "StVersionPicker");

    void readSettingsFile() {
        try {
            var path = stvpHome.resolve("connections");
            if (path.toFile().exists()) {
                var lines = Files.readAllLines(path);
                var local = lines.get(0).split("\t");
                var remote = lines.get(1).split("\t");
                localStPickerController.setTexts(local[0], local[1]);
                remoteStPickerController.setTexts(remote[0], remote[1]);
            }
        } catch (IndexOutOfBoundsException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    void writeSettingsFile() {
        var lines = Stream.of(localStPickerController, remoteStPickerController)
                          .map(StPickerComponentController::getTexts)
                          .toList();
        try {
            var file = stvpHome.resolve("connections");
            stvpHome.toFile().mkdirs();
            Files.write(file, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}