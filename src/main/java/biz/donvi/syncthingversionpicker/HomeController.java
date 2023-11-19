package biz.donvi.syncthingversionpicker;

import biz.donvi.syncthingversionpicker.files.StFile;
import biz.donvi.syncthingversionpicker.remoteaccess.RemoteLister;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.nio.file.Path;

import static biz.donvi.syncthingversionpicker.SyncthingScraper.ST_LIST_FOLDERS;

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

        RemoteLister remoteLister;
        try {
            remoteLister = new RemoteLister(
                sshUser.getText(),
                shhAddress.getText(),
                Integer.parseInt(sshPort.getText()),
                sshPassword.getText(), Path.of("")
            );
//            StFolder folder = remoteSyncScraper.getEndpoint(ST_LIST_FOLDERS, remoteLister);
        } catch (JSchException | SftpException e) {
            throw new RuntimeException(e);
        }
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
}