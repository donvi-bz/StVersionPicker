package biz.donvi.syncthingversionpicker.controllers;

import atlantafx.base.theme.Styles;
import biz.donvi.syncthingversionpicker.SyncPickerApp;
import biz.donvi.syncthingversionpicker.files.Location;
import biz.donvi.syncthingversionpicker.files.StFileGroup;
import biz.donvi.syncthingversionpicker.files.StFileGroup.File;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.ResourceBundle;

public class PickerTableComponentController implements Initializable {

    private static final Logger logger = LogManager.getLogger(PickerController.class);

    @FXML
    private TableView<File> fileGroupTable;

    @FXML
    public TableColumn<File, String> columnType;
    @FXML
    public TableColumn<File, String> columnLocation;
    @FXML
    public TableColumn<File, String> columnDateCreated;
    @FXML
    public TableColumn<File, String> columnTimeSinceCreation;
    @FXML
    public TableColumn<File, String> columnName;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        fileGroupTable.setRowFactory(x -> new PickerTableRow());

        columnType.setCellValueFactory(x -> new ReadOnlyObjectWrapper<>(
            x.getValue().location.when.which("Real", "Backup")
        ));

        columnLocation.setCellValueFactory(x -> new ReadOnlyObjectWrapper<>(
            x.getValue().location.where.which("Local", "Remote")
        ));

        columnDateCreated.setCellValueFactory(x -> new ReadOnlyObjectWrapper<>(
            x.getValue().getTimeStamp()
        ));
        columnDateCreated.setComparator(Comparator.comparing(PickerTableComponentController::toSortableDate));

        columnTimeSinceCreation.setCellValueFactory(x -> new ReadOnlyObjectWrapper<>(
            x.getValue().getTimeAgo(LocalDateTime.now())
        ));
        columnTimeSinceCreation.setSortable(false);

        columnName.setCellValueFactory(x -> new ReadOnlyObjectWrapper<>(
            x.getValue().nameRaw
        ));

        fileGroupTable.getStyleClass().add(Styles.DENSE);
    }

    private static String toSortableDate(String str) {
        var split = str.split(" ");
        if (split.length != 3)
            return str;
        var ampm = switch (split[2]) {
            case "AM" -> "1";
            case "PM" -> "2";
            default -> "0";
        };
        return split[0] + ampm + split[1];
    }


    public void setSelected(StFileGroup fileGroup) {
        if (fileGroup == null) {
            fileGroupTable.setItems(FXCollections.observableArrayList());
        } else {
            fileGroupTable.setItems(FXCollections.observableArrayList(fileGroup.getFiles()));
        }
    }

    /* **************************************************************
     MARK: - PickerTableRow
     ****************************************************************/

    static class PickerTableRow extends TableRow<File> {

        private final PickerTableContextMenu contextMenu = new PickerTableContextMenu();

        {
            setContextMenu(contextMenu);
        }

        @Override
        protected void updateItem(File file, boolean empty) {
            super.updateItem(file, empty);
            if (file == null || empty) {
                setStyle("");
                return;
            }
            String style = "";
            if (file.location.when == Location.When.Current)
                style += "-fx-font-weight: bold;-fx-border: 2px;";

            style += isSelected()
                ? "-fx-border-color: black;"
                : "-fx-border-color: transparent;";

            style += file.location.where.which(
                "-fx-background-color: -color-blue;",
                "-fx-background-color: -color-purple;"
            );

            setStyle(style);


            contextMenu.updateMenuForFile(file);
        }
    }

    /* **************************************************************
     MARK: - PickerTableContextMenu
     ****************************************************************/

    static class PickerTableContextMenu extends ContextMenu {

        private static final Logger logger = LogManager.getLogger(PickerTableContextMenu.class);

        final MenuItem showInExplorer   = new MenuItem("Show in Explorer");
        final MenuItem openInDefaultApp = new MenuItem("Open in Default App");
        final MenuItem saveACopy        = new MenuItem("Save a Copy");
        final MenuItem restoreVersion   = new MenuItem("Restore Version");

        final SyncPickerApp app = SyncPickerApp.getApplication();

        private File file;

        {
            var items = this.getItems();
            items.add(showInExplorer);
            items.add(openInDefaultApp);
            items.add(new SeparatorMenuItem());
            items.add(saveACopy);
            items.add(restoreVersion);


            showInExplorer.setOnAction(this::showInExplorerAction);
            openInDefaultApp.setOnAction(this::openInDefaultAppAction);
            saveACopy.setOnAction(this::saveACopyAction);
        }

        private void showInExplorerAction(ActionEvent event) {
            logger.info("Show file in explorer action triggered for file `{}`", file);
            file.getLocalFile().whenCompleteAsync((file, ex) -> {
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    logger.debug("Using windows specific explorer.");
                    try {
                        Runtime.getRuntime().exec("explorer.exe /select," + file.getPath());
                    } catch (IOException ex2) {
                        logger.error("Could not open file in explorer", ex2);
                    }
                } else {
                    logger.debug("Using generic java explorer");
                    app.getHostServices().showDocument(file.getPath());
                }
            });
        }

        private void openInDefaultAppAction(ActionEvent event) {
            logger.info("Open in default app action triggered for file `{}`", file);
            file.getLocalFile().whenCompleteAsync((file, ex) -> {
                app.getHostServices().showDocument(file.getPath());
            });
        }

        private void saveACopyAction(ActionEvent event) {
            logger.info("Save a copy action triggered for file `{}`", file);
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Copy As...");
            chooser.setInitialFileName(file.getParent().fileName);
            String ext = file.getParent().fileExtension;
            chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Save as Original (%s)".formatted(ext), "*" + ext),
                new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            java.io.File saveLocation = chooser.showSaveDialog(app.getStage());
            if (saveLocation != null)
                this.file
                    .getLocalFile()
                    .whenCompleteAsync((file, ex) -> {
                        if (file != null) try {
                            Files.copy(file.toPath(), saveLocation.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            logger.debug("Successfully copied file.");
                        } catch (IOException e) {
                            logger.error("Could not copy file %s to %s".formatted(file, saveLocation), e);
                        }
                        if (ex != null) {
                            logger.error("Could not get file %s".formatted(file), ex);
                        }
                    });
        }

        private void updateMenuForFile(File file) {
            this.file = file;
//            showInExplorer.setDisable(file.location.where == Location.Where.Remote);
//            openInDefaultApp.setDisable(file.location.where == Location.Where.Remote);
        }
    }
}
