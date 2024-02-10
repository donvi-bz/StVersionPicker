package biz.donvi.syncthingversionpicker.services;

import biz.donvi.syncthingversionpicker.SyncPickerApp;
import biz.donvi.syncthingversionpicker.files.FullStLister;
import biz.donvi.syncthingversionpicker.files.Location;
import biz.donvi.syncthingversionpicker.files.ParsedFileName;
import biz.donvi.syncthingversionpicker.files.StFileGroup;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
                .whenCompleteAsync((iof, ex) -> copyFile(saveLocation, iof, ex));
        } else {
            logger.debug("Did not get a location to save the file too. Aborting action.");
        }
    }
    private void copyFile(File saveLocation, File dataSoSave, Throwable ex) {
        if (dataSoSave != null) try {
            Files.copy(dataSoSave.toPath(), saveLocation.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.debug("Successfully copied file.");
        } catch (IOException e) {
            logger.error("Could not copy file %s to %s".formatted(dataSoSave, saveLocation), e);
        }
        if (ex != null) {
            logger.error("Could not get file %s".formatted(dataSoSave), ex);
        }
    }

    public CompletableFuture<File> restoreVersion(StFileGroup.File fileToRestore) {
        logger.info("Attempting to restore file `{}`", fileToRestore);
        Optional<StFileGroup.File> oCurrentFile = fileToRestore
            .getParent().getFiles()
            .stream().filter(f -> f.location == Location.LocalCurrent)
            .findFirst();
        if (oCurrentFile.isPresent()) {
            StFileGroup.File currentFile = oCurrentFile.get();
            logger.debug("Current file appears to be present. Will rename file `{}`", currentFile);
            File currentFileReal;
            try {
                // We can call `get()` because Local files don't move threads.
                // Local files should also not throw any exceptions.
                currentFileReal = currentFile.getLocalFile().get();
            } catch (Exception e) {
                logger.error("Somehow got an exception?? This should not be possible for local files...", e);
                throw new RuntimeException(e);
            }
            // Renaming shenanigans
            ParsedFileName nameParsed = new ParsedFileName(currentFileReal.getName());
            String name = nameParsed.getBeginning() + "~VP-PREV" + nameParsed.getEnd();
            File newFile = currentFileReal.toPath().getParent().resolve(name).toFile();
            // And time to actually do the renaming
            boolean didRename = currentFileReal.renameTo(newFile);
            if (didRename)
                logger.debug("Successfully renamed file to `{}`", currentFileReal.getPath());
            else logger.warn("Could not rename file to `{}`", newFile.getPath());
            // Current file should no longer be present. Now we can continue on
        }
        File saveLocation = fileToRestore.getParent().getFullPath().toFile();
        return fileToRestore.getLocalFile().whenCompleteAsync((iof, ex) -> copyFile(saveLocation, iof, ex));
    }
}
