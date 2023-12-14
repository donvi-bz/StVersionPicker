package biz.donvi.syncthingversionpicker;

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

    @FXML private TextField localSyncthingUrl;
    @FXML private TextField localSyncthingApiKey;
    @FXML private Text      localSyncthingTestBtn;
    @FXML private Text      localSyncthingTestAnswer;

    private int              localSyncTestCount         = 0;
    private boolean          localSyncthingScraperValid = false;
    private SyncthingScraper localSyncthingScraper      = null;

    @FXML private TextField remoteSyncthingUrl;
    @FXML private TextField remoteSyncthingApiKey;
    @FXML public  Text      remoteSyncthingTestBtn;
    @FXML private Text      remoteSyncthingTestAnswer;

    private int              remoteSyncTestCount         = 0;
    private boolean          remoteSyncthingScraperValid = false;
    private SyncthingScraper remoteSyncthingScraper      = null;

    @FXML private TextField     sshUser;
    @FXML private TextField     shhAddress;
    @FXML private TextField     sshPort;
    @FXML private PasswordField sshPassword;
    @FXML private Text          sshTestBtn;
    @FXML private Text          sshTestAnswer;

    RemoteLister remoteLister;

    @FXML
    public void initialize() {
        clearLocalSyncthingAnswer();
        clearRemoteSyncthingAnswer();
        clearSshTestAnswer();
    }

    @FXML
    protected void onSubmitApiKeyBtnPress() {
        @SuppressWarnings("unchecked")
        CompletableFuture<Boolean>[] futures = new CompletableFuture[]{
            testLocalSyncthingIfNecessary(),
            testRemoteSyncthingIfNecessary(),
            testSsh()
        };
        CompletableFuture.allOf(futures).thenApplyAsync(results -> {
            try {
                if (Arrays.stream(futures).allMatch(CompletableFuture::resultNow)) {
                    // This will throw if its no good.0
                    localSyncthingScraper.updateFolders();
                    remoteSyncthingScraper.updateFolders();
                    // Put in global state
                    SyncPickerApp app = SyncPickerApp.getApplication();
                    app.localSyncScraper = localSyncthingScraper;
                    app.remoteSyncScraper = remoteSyncthingScraper;
                    app.remoteLister = remoteLister;
                    return app;
                } else if (futures[0].resultNow()) {
                    // TODO: make this better
                    localSyncthingScraper.updateFolders();
                    SyncPickerApp app = SyncPickerApp.getApplication();
                    app.localSyncScraper = localSyncthingScraper;
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
    protected void clearLocalSyncthingAnswer() {
        localSyncthingTestAnswer.setText("");
        localSyncthingScraperValid = false;
    }

    @FXML
    protected void clearRemoteSyncthingAnswer() {
        remoteSyncthingTestAnswer.setText("");
        remoteSyncthingScraperValid = false;
    }

    @FXML
    protected void clearSshTestAnswer() {
        sshTestAnswer.setText("");
    }

    protected CompletableFuture<Boolean> testLocalSyncthingIfNecessary() {
        // Short-circuit if we already have a good one. Since there is a bunch of thread
        // hopping involved in the test, we'd like to avoid it all if we KNOW we can.
        if (localSyncthingScraperValid && localSyncthingScraper != null)
            return CompletableFuture.completedFuture(true);
            // And if we can't - Check for real.
        else return testLocalSyncthing();
    }

    @FXML
    protected CompletableFuture<Boolean> testLocalSyncthing() {
        // I tried to make it so these methods weren't just duplicates of each other,
        // but partly due to how JavaFX requires field names to be done, each way I
        // tried ended up horribly ugly. Instead, use the "Duplicate Code" warning to
        // make sure that these methods are properly duplicates.
        clearLocalSyncthingAnswer();
        localSyncthingTestBtn.setText("Testing...");
        int currentNum = ++localSyncTestCount;
        return CompletableFuture.supplyAsync(() -> {
            var scraper = new SyncthingScraper(
                localSyncthingUrl.getText(),
                localSyncthingApiKey.getText()
            );
            return scraper.testConnection();
        }).thenApplyAsync(testResult -> {
            if (currentNum < localSyncTestCount)
                return null;
            if (testResult.valid())
                localSyncthingScraper = testResult.self();
            localSyncthingScraperValid = testResult.valid();
            localSyncthingTestBtn.setText("Test Connection");
            localSyncthingTestAnswer.setText(testResult.msg());
            return testResult.valid();
        }, Platform::runLater);
    }

    protected CompletableFuture<Boolean> testRemoteSyncthingIfNecessary() {
        // Short-circuit if we already have a good one. Since there is a bunch of thread
        // hopping involved in the test, we'd like to avoid it all if we KNOW we can.
        if (remoteSyncthingScraperValid && remoteSyncthingScraper != null)
            return CompletableFuture.completedFuture(true);
            // And if we can't - Check for real.
        else return testRemoteSyncthing();
    }

    @FXML
    protected CompletableFuture<Boolean> testRemoteSyncthing() {
        // I tried to make it so these methods weren't just duplicates of each other,
        // but partly due to how JavaFX requires field names to be done, each way I
        // tried ended up horribly ugly. Instead, use the "Duplicate Code" warning to
        // make sure that these methods are properly duplicates.
        clearRemoteSyncthingAnswer();
        remoteSyncthingTestBtn.setText("Testing...");
        int currentNum = ++remoteSyncTestCount;
        return CompletableFuture.supplyAsync(() -> {
            var scraper = new SyncthingScraper(
                remoteSyncthingUrl.getText(),
                remoteSyncthingApiKey.getText()
            );
            return scraper.testConnection();
        }).thenApplyAsync(testResult -> {
            if (currentNum < remoteSyncTestCount)
                return null;
            if (testResult.valid())
                remoteSyncthingScraper = testResult.self();
            remoteSyncthingScraperValid = testResult.valid();
            remoteSyncthingTestBtn.setText("Test Connection");
            remoteSyncthingTestAnswer.setText(testResult.msg());
            return testResult.valid();
        }, Platform::runLater);
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