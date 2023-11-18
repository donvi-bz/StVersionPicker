package biz.donvi.syncthingversionpicker;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class HomeController {

    @FXML private TextField localSyncthingUrl;
    @FXML private TextField localSyncthingApiKey;

    @FXML private TextField remoteSyncthingUrl;
    @FXML private TextField remoteSyncthingApiKey;

    @FXML private TextField     sshUser;
    @FXML private TextField     shhAddress;
    @FXML private TextField     sshPort;
    @FXML private PasswordField sshPassword;

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
        // This will throw if its no good.
        localSyncScraper.updateFolders();
        // Put in global state
        SyncPickerApp app = SyncPickerApp.getApplication();
        app.localSyncScraper = localSyncScraper;
        app.setPickerStage();
    }
}