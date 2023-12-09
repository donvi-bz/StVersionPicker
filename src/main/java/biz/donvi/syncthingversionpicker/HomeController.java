package biz.donvi.syncthingversionpicker;

import biz.donvi.syncthingversionpicker.remoteaccess.RemoteLister;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class HomeController {

    @FXML private TextField localSyncthingUrl;
    @FXML private TextField localSyncthingApiKey;
    @FXML private Text      localSyncthingTestBtn;
    @FXML private Text      localSyncthingTestAnswer;

    private int              localSyncTestCount       = 0;
    private SyncthingScraper localSyncthingScraperRef = null;

    @FXML private TextField remoteSyncthingUrl;
    @FXML private TextField remoteSyncthingApiKey;
    @FXML public  Text      remoteSyncthingTestBtn;
    @FXML private Text      remoteSyncthingTestAnswer;

    private int              remoteSyncTestCount    = 0;
    private SyncthingScraper remoteSyncthingScraper = null;

    @FXML private TextField     sshUser;
    @FXML private TextField     shhAddress;
    @FXML private TextField     sshPort;
    @FXML private PasswordField sshPassword;
    @FXML private Text          checkText;
    @FXML private Button        btnCheckText;

    RemoteLister remoteLister;

    @FXML
    public void initialize() {
        clearLocalSyncthingAnswer();
        clearRemoteSyncthingAnswer();
    }

    @FXML
    protected void onSubmitApiKeyBtnPress() throws IOException {
        SyncthingScraper localSyncScraper = new SyncthingScraper(
            localSyncthingUrl.getText(),
            localSyncthingApiKey.getText()
        );
        SyncthingScraper remoteSyncScraper = new SyncthingScraper(
            remoteSyncthingUrl.getText(),
            remoteSyncthingApiKey.getText()
        );
        testSsh();
        // This will throw if its no good.
        localSyncScraper.updateFolders();
        remoteSyncScraper.updateFolders();
        // Put in global state
        SyncPickerApp app = SyncPickerApp.getApplication();
        app.localSyncScraper = localSyncScraper;
        app.remoteSyncScraper = remoteSyncScraper;
        app.remoteLister = remoteLister;
//        app.remoteLister = remoteLister;
        app.setPickerStage();
    }

    @FXML
    protected void clearLocalSyncthingAnswer() {
        localSyncthingTestAnswer.setText("");
    }

    @FXML
    protected void clearRemoteSyncthingAnswer() {
        remoteSyncthingTestAnswer.setText("");
    }


    @FXML
    @SuppressWarnings("DuplicatedCode") // I've tried so much, but each new requirement makes me go back...
    protected void testLocalSyncthing() {
        localSyncthingTestBtn.setText("Testing...");
        int currentNum = ++localSyncTestCount;
        CompletableFuture.supplyAsync(() -> {
            var scraper = new SyncthingScraper(
                localSyncthingUrl.getText(),
                localSyncthingApiKey.getText()
            );
            return scraper.testConnection();
        }).thenAcceptAsync(responseStr -> {
            if (currentNum < localSyncTestCount)
                return;
            localSyncthingTestBtn.setText("Test Connection");
            localSyncthingTestAnswer.setText(responseStr);
        }, Platform::runLater);
    }

    @FXML
    @SuppressWarnings("DuplicatedCode") // I've tried so much, but each new requirement makes me go back...
    protected void testRemoteSyncthing() {
        remoteSyncthingTestBtn.setText("Testing...");
        int currentNum = ++remoteSyncTestCount;
        CompletableFuture.supplyAsync(() -> {
            var scraper = new SyncthingScraper(
                remoteSyncthingUrl.getText(),
                remoteSyncthingApiKey.getText()
            );
            return scraper.testConnection();
        }).thenAcceptAsync(responseStr ->  {
            if (currentNum < remoteSyncTestCount)
                return;
            remoteSyncthingTestBtn.setText("Test Connection");
            remoteSyncthingTestAnswer.setText(responseStr);
        }, Platform::runLater);
    }


    @FXML
    protected void testSsh() {
        remoteLister = new RemoteLister(
            sshUser.getText(),
            shhAddress.getText(),
            Integer.parseInt(sshPort.getText()),
            sshPassword.getText(), Path.of("")
        );
        checkText.setText("Testing...");
        btnCheckText.setDisable(true);
        remoteLister.setupSessionAsync().thenAcceptAsync(e -> {
            String message = e.isEmpty() ? "Connected" : e.get().getLocalizedMessage();
            checkText.setText(message);
            btnCheckText.setDisable(false);
        }, Platform::runLater);
    }
}