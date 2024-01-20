package biz.donvi.syncthingversionpicker.controllers;

import atlantafx.base.theme.Styles;
import biz.donvi.syncthingversionpicker.SyncPickerApp;
import biz.donvi.syncthingversionpicker.files.Location;
import biz.donvi.syncthingversionpicker.files.StFileGroup;
import biz.donvi.syncthingversionpicker.files.StFileGroup.File;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.ResourceBundle;

public class PickerTableComponentController implements Initializable {

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

        final MenuItem showInExplorer   = new MenuItem("Show in Explorer");
        final MenuItem openInDefaultApp = new MenuItem("Open in Default App");
        final MenuItem saveACopy        = new MenuItem("Save a Copy");
        final MenuItem restoreVersion   = new MenuItem("Restore Version");

        private File file;

        {
            var items = this.getItems();
            items.add(showInExplorer);
            items.add(openInDefaultApp);
            items.add(new SeparatorMenuItem());
            items.add(saveACopy);
            items.add(restoreVersion);

            SyncPickerApp app = SyncPickerApp.getApplication();

            showInExplorer.setOnAction(event -> {
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    try {
                        Runtime.getRuntime().exec("explorer.exe /select," + file.getRawFullPath().toString());
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                } else {
                    app.getHostServices().showDocument(file.getRawFullPath().toString());
                }
            });
            openInDefaultApp.setOnAction(event -> {
                app.getHostServices().showDocument(file.getRawFullPath().toString());
            });

            saveACopy.setOnAction(event -> {
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
                        .getInputStream()
                        .whenCompleteAsync((in, ex) -> {
                            if (in != null) try {
                                Files.copy(in, saveLocation.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            if (ex instanceof FileNotFoundException notFoundException) {
                                notFoundException.printStackTrace();
                            } else if (ex instanceof IOException ioException) {
                                ioException.printStackTrace();
                            } else {
                                ex.printStackTrace();
                            }
                        }, Platform::runLater);
            });
        }

        private void updateMenuForFile(File file) {
            this.file = file;
            showInExplorer.setDisable(file.location.where == Location.Where.Remote);
            openInDefaultApp.setDisable(file.location.where == Location.Where.Remote);
        }
    }
}
