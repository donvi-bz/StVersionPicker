package biz.donvi.syncthingversionpicker.services;

import biz.donvi.syncthingversionpicker.SyncPickerApp;
import biz.donvi.syncthingversionpicker.files.*;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
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

    /**
     * Restores a <b>specific version</b> of a file, optionally replacing the existing a current file if one already
     * exists locally.
     *
     * @param fileToRestore   The specific version of the file to restore.
     * @param replaceExisting If the user currently has a file in this group, do we replace it?
     * @return A completable future holding the {@code File} that has been copied.
     * TODO: This is the file *before* it was copied, right? Does that make sense? Is that useful?
     */
    public CompletableFuture<File> restoreVersion(StFileGroup.File fileToRestore, boolean replaceExisting) {
        logger.info("Attempting to restore file `{}`", fileToRestore);
        // Find the current file
        Optional<StFileGroup.File> oCurrentFile = fileToRestore
            .getParent().getFiles()
            .stream().filter(f -> f.location == Location.LocalCurrent)
            .findFirst();
        if (oCurrentFile.isPresent() && !replaceExisting) {
            // If we don't want to replace the file, let the logs know, then exit early.
            logger.debug("File `{}` already had a current version and `replaceExisting` is set to false. Skipping.",
                         fileToRestore);
            return CompletableFuture.completedFuture(null);
        } else if (oCurrentFile.isPresent()) {
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
        CompletableFuture<File> fileCopyFuture
            = fileToRestore.getLocalFile().whenCompleteAsync((iof, ex) -> copyFile(saveLocation, iof, ex));
        logger.debug("Finished restoring file `{}`", fileToRestore);
        return fileCopyFuture;
    }

    /**
     * Restores a file denoted by its file group. Since this takes a file group, this method will decide which version
     * of the file to restore it with. This method uses {@link #restoreVersion(StFileGroup.File, boolean)} behind the
     * scenes, and also can replace the existing file if needed.
     *
     * @param fileToRestore   The fileGroup that we want to restore a file from. This method will pick which specific
     *                        file to restore.
     * @param replaceExisting If the user currently has a file in this group, do we replace it?
     * @return A completable future holding the {@code File} that has been copied.
     */
    public CompletableFuture<File> restoreVersion(StFileGroup fileToRestore, boolean replaceExisting) {
        logger.info("Restoring StFileGroup `{}`", fileToRestore);
        // Find the first file that isn't a current file
        var firstVersion = fileToRestore
            .getFiles().stream()
            .filter(f -> f.location.when != Location.When.Current)
            .findFirst();
        if (firstVersion.isPresent()) {
            logger.debug("A previous version does exist. Restoring...");
            return restoreVersion(firstVersion.get(), replaceExisting);
        } else return CompletableFuture.completedFuture(null);
    }

    /**
     * Helper method that takes a {@link StFile} and directs it to the correct method. <br/>
     * Type {@link StFileGroup} goes to {@link #restoreVersion(StFileGroup, boolean)} <br/>
     * Type {@link StDirectory} goes to {@link #restoreVersion(StDirectory, boolean)} <br/>
     * Note: The actual file to take will be decided automatically.
     *
     * @param fileToRestore   The {@link StFile} to restore.
     * @param replaceExisting If a current file exists, do we replace it or just cancel.
     * @return A completable future holding a {@code List} of {@code File}s that have been copied.
     */
    private CompletableFuture<List<File>> restoreVersion(StFile fileToRestore, boolean replaceExisting) {
        if (fileToRestore instanceof StFileGroup fileGroup)
            return restoreVersion(fileGroup, replaceExisting).thenApplyAsync(List::of);
        else if (fileToRestore instanceof StDirectory folder)
            return restoreVersion(folder, replaceExisting);
        else return CompletableFuture.completedFuture(List.of());
    }

    /**
     * Restores an entire directory. Since this takes a directory, there is no way to specify individually which
     * versions will be restored. Like the other overloads of this method, replaceExisting can be used to control
     * whether current files are replace. It is recommended to leave this as false.
     *
     * @param directoryToRestore The directory of which to restore all child files (recursively)
     * @param replaceExisting    If a current file exists, do we replace it or skip it?
     * @return A completable future holding a list of all the files that have been copied.
     */
    public CompletableFuture<List<File>> restoreVersion(StDirectory directoryToRestore, boolean replaceExisting) {
        // This method is a bit to unpack. It's recursive, yet still returns a flat list at the end.
        logger.info("Restoring entire directory `{}`", directoryToRestore);
        // So we'll be doing the manual future completion. This future will hold all the results.
        CompletableFuture<List<File>> theFuture = new CompletableFuture<>();
        // Alright, first things ~~third~~ first, we need to actually list the files.
        // Once they are listed, we can begin restoring.
        directoryToRestore.listFilesAsync().thenAcceptAsync((List<StFile> listOfFiles) -> {
            // Let's take that list of StFiles and stream it...
            List<CompletableFuture<List<File>>> listOfLists = listOfFiles
                .stream() // ↓ Here ↓ we call the #restoreVersion() method. Remember that this method EITHER restores a
                .map(f -> restoreVersion(f, replaceExisting)) //  single file, OR recursively calls the method we're in.
                .toList(); // The recursion ends when all calls to #restoreVersion() restore a file, not a directory.
            // Now we need to know when all of these are done.
            CompletableFuture
                .allOf(listOfLists.toArray(CompletableFuture[]::new))
                .whenComplete((unused, throwable) -> {
                    // If we have some sort of error, propagate it.
                    if (throwable != null) {
                        theFuture.completeExceptionally(new Exception(throwable));
                        return; // Explicitly return here so there is less nesting.
                    }
                    // If we've gotten here, that means that all the futures completed. Time to get all the files that
                    // we updated. Remember: listOfLists is a List<CompletableFuture<List<File>>>, though we now know
                    // that the Future part is complete, we want to remove that and flatten it.
                    List<File> listOfModified = listOfLists
                        .stream() // First ↓ getNow() gets the inner List<File>, removing the CompletableFuture portion.
                        .flatMap(f -> f.getNow(List.of()).stream()) // Then its flatMapped, removing the inner list.
                        .toList(); // Last it's turned from a stream into a list.
                    // Now everything's been flattened, we can return our value!
                    // If your thinking of this in its recursive call sequence, now read where `listOfLists` is declared
                    theFuture.complete(listOfModified);
                });
        });
        return theFuture;
    }
}
