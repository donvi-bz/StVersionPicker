package biz.donvi.syncthingversionpicker.services;

import biz.donvi.syncthingversionpicker.SyncPickerApp;
import biz.donvi.syncthingversionpicker.files.FullStLister;
import biz.donvi.syncthingversionpicker.files.StFileGroup;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class FileManipulationService implements FullStLister.FileService {
    private static final Logger logger = LogManager.getLogger(FileManipulationService.class);

    final SyncPickerApp app = SyncPickerApp.getApplication();

    public void showFileInExplorer(StFileGroup.File file) {
        logger.info("Will show file in explorer `{}`", file);
        file.getLocalFile().whenCompleteAsync((iof, ex) -> {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                logger.debug("Using windows specific explorer.");
                try {
                    Runtime.getRuntime().exec("explorer.exe /select," + iof.getPath());
                } catch (IOException ex2) {
                    logger.error("Could not open file in explorer", ex2);
                }
            } else {
                logger.debug("Using generic java explorer");
                app.getHostServices().showDocument(iof.getPath());
            }
        });
    }

    public void openInDefaultApp(StFileGroup.File file) {
        logger.info("Will open file in default app `{}`", file);
        file.getLocalFile().whenCompleteAsync((iof, ex) -> app.getHostServices().showDocument(iof.getPath()));
    }

    public void saveACopy(StFileGroup.File file) {
        logger.info("Attempting to save a copy of file `{}`", file);
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Copy As...");
        chooser.setInitialFileName(file.getParent().fileName);
        String ext = file.getParent().fileExtension;
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Save as Original (%s)".formatted(ext), "*" + ext),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        java.io.File saveLocation = chooser.showSaveDialog(app.getStage());
        if (saveLocation != null) {
            logger.debug("Got potential save location: `{}`", saveLocation.getPath());
            file.getLocalFile()
                .whenCompleteAsync((iof, ex) -> {
                    if (iof != null) try {
                        Files.copy(iof.toPath(), saveLocation.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        logger.debug("Successfully copied file.");
                    } catch (IOException e) {
                        logger.error("Could not copy file %s to %s".formatted(iof, saveLocation), e);
                    }
                    if (ex != null) {
                        logger.error("Could not get file %s".formatted(iof), ex);
                    }
                });
        } else {
            logger.debug("Did not get a location to save the file too. Aborting action.");
        }
    }
}
