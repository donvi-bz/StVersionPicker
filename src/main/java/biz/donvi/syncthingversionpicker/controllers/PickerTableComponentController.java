package biz.donvi.syncthingversionpicker.controllers;

import atlantafx.base.theme.Styles;
import biz.donvi.syncthingversionpicker.SyncPickerApp;
import biz.donvi.syncthingversionpicker.files.Location;
import biz.donvi.syncthingversionpicker.files.StFileGroup;
import biz.donvi.syncthingversionpicker.files.StFileGroup.File;
import biz.donvi.syncthingversionpicker.services.FileManipulationService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
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


            showInExplorer.setOnAction(event -> {
                logger.debug("Show file in explorer action triggered for file `{}`", file);
                getFileService().showFileInExplorer(file);
            });
            openInDefaultApp.setOnAction(event -> {
                logger.debug("Open in default app action triggered for file `{}`", file);
                getFileService().openInDefaultApp(file);
            });
            saveACopy.setOnAction(event -> {
                logger.debug("Save a copy action triggered for file `{}`", file);
                getFileService().saveACopy(file);
            });
        }

        private FileManipulationService getFileService() {
            return file.getParent().parentDir.fullStLister.getService(FileManipulationService.class);
        }

        private void updateMenuForFile(File file) {
            this.file = file;
//            showInExplorer.setDisable(file.location.where == Location.Where.Remote);
//            openInDefaultApp.setDisable(file.location.where == Location.Where.Remote);
        }
    }
}
