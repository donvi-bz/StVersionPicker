package biz.donvi.syncthingversionpicker;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import java.io.IOException;

public class HomeController {

    @FXML
    private TextField syncthingUrl;

    @FXML
    private TextField apiKey;

    @FXML
    protected void onSubmitApiKeyBtnPress() throws IOException {
        SyncthingScraper syncScraper = new SyncthingScraper(
            syncthingUrl.getText(),
            apiKey.getText()
        );
        // This will throw if its no good.
        syncScraper.updateFolders();
        // Put in global state
        SyncPickerApp app = SyncPickerApp.getApplication();
        app.syncScraper = syncScraper;
        app.setPickerStage();
    }
}