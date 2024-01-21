package biz.donvi.syncthingversionpicker.controllers;

import biz.donvi.syncthingversionpicker.SyncthingScraper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class StPickerComponentController implements Initializable {

    @FXML private TextField syncthingUrl;
    @FXML private TextField syncthingApiKey;
    @FXML private Text      syncthingTestBtn;
    @FXML private Text      syncthingTestAnswer;

    private int              syncTestCount         = 0;
    private boolean          syncthingScraperValid = false;
    public  SyncthingScraper syncthingScraper      = null;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        clearSyncthingAnswer();
    }

    /**
     * Sets the two text fields (url & apiKey) in this component.
     * <br/>
     * Note: Passing {@code null} will not modify the text. To clear the text, pass an empty string.
     * @param url The new url text.
     * @param apiKey The new apiKey text.
     */
    public void setTexts(String url, String apiKey) {
        if (url != null)
            syncthingUrl.setText(url);
        if (apiKey != null)
            syncthingApiKey.setText(apiKey);
    }

    public String getTexts() {
        return syncthingUrl.getText() + "\t" + syncthingApiKey.getText();
    }

    /**
     * Clears the state of the previous Syncthing test. (Also clears the text on the UI)
     */
    @FXML
    protected void clearSyncthingAnswer() {
        syncthingTestAnswer.setText("");
        syncthingScraperValid = false;
    }

    /**
     * <em><b>If</b></em> the {@link SyncthingScraper} has already been tested <em><b>and</</b></em> is valid,
     * this method will return a pre-completed future saying that it is valid. If the last test
     * returned invalid, or the {@code SyncthingScraper} hasn't yet been tested, this method will
     * test the scraper and return a future with the test results.
     * @return A {@link CompletableFuture} with this {@link SyncthingScraper}'s test results.<br/>
     *         Note: {@code true} for success, {@code false} for failure.
     */
    protected CompletableFuture<Boolean> testSyncthingIfNecessary() {
        // Short-circuit if we already have a good one. Since there is a bunch of thread
        // hopping involved in the test, we'd like to avoid it all if we KNOW we can.
        if (syncthingScraperValid && syncthingScraper != null)
            return CompletableFuture.completedFuture(true);
            // And if we can't - Check for real.
        else return testSyncthing();
    }

    /**
     * Tests the {@link SyncthingScraper} to see if a connection can be made. This method re-tests every
     * time it is called regardless of the state of the component.
     * @return A {@link CompletableFuture} with this {@link SyncthingScraper}'s test results.<br/>
     *         Note: {@code true} for success, {@code false} for failure.
     */
    @FXML
    protected CompletableFuture<Boolean> testSyncthing() {
        clearSyncthingAnswer();
        syncthingTestBtn.setText("Testing...");
        int currentNum = ++syncTestCount;
        return CompletableFuture.supplyAsync(() -> {
            var scraper = new SyncthingScraper(
                syncthingUrl.getText(),
                syncthingApiKey.getText()
            );
            return scraper.testConnection();
        }).thenApplyAsync(testResult -> {
            if (currentNum < syncTestCount)
                return null;
            if (testResult.valid())
                syncthingScraper = testResult.self();
            syncthingScraperValid = testResult.valid();
            syncthingTestBtn.setText("Test Connection");
            syncthingTestAnswer.setText(testResult.msg());
            return testResult.valid();
        }, Platform::runLater);
    }
}

