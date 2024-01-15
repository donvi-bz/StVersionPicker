package biz.donvi.syncthingversionpicker.controllers;

import atlantafx.base.theme.Styles;
import biz.donvi.syncthingversionpicker.SyncPickerApp;
import biz.donvi.syncthingversionpicker.files.StFileGroup;
import biz.donvi.syncthingversionpicker.files.StFileGroup.File;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
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
            x.getValue().location.isReal ? "Real" : "Backup")
        );

        columnLocation.setCellValueFactory(x -> new ReadOnlyObjectWrapper<>(
            x.getValue().location.isLocal ? "Local" : "Remote"
        ));

        columnDateCreated.setCellValueFactory(x -> new ReadOnlyObjectWrapper<>(
            x.getValue().getTimeStamp()
        ));

        columnTimeSinceCreation.setCellValueFactory(x -> new ReadOnlyObjectWrapper<>(
            x.getValue().getTimeAgo(LocalDateTime.now())
        ));

        columnName.setCellValueFactory(x -> new ReadOnlyObjectWrapper<>(
            x.getValue().nameRaw
        ));

        fileGroupTable.getStyleClass().add(Styles.DENSE);
    }

    public void setSelected(StFileGroup fileGroup) {
        if (fileGroup == null) {
            fileGroupTable.setItems(FXCollections.observableArrayList());
        } else {
            fileGroupTable.setItems(FXCollections.observableArrayList(fileGroup.getFiles()));
        }
    }

    /****************************************************************
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
            if (file.location.isReal)
                style += "-fx-font-weight: bold;-fx-border: 2px;";

            if (isSelected())
                style += "-fx-border-color: black;";
            else
                style += "-fx-border-color: transparent;";

            if (file.location.isLocal)
                style += "-fx-background-color: -color-blue;";
            else
                style += "-fx-background-color: -color-purple;";

            setStyle(style);


            contextMenu.updateMenuForFile(file);
        }
    }

    /****************************************************************
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
                        Runtime.getRuntime().exec("explorer.exe /select," + file.getFullRawPath().toString());
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                } else {
                    app.getHostServices().showDocument(file.getFullFolderPath().toString());
                }
            });
            openInDefaultApp.setOnAction(event -> {
                app.getHostServices().showDocument(file.getFullRawPath().toString());
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
                java.io.File file = chooser.showSaveDialog(app.getStage());
                if (file != null) {
                    try (InputStream inputStream = this.file.getInputStream()){
                        Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        private

        void updateMenuForFile(File file) {
            this.file = file;
            showInExplorer.setDisable(!file.location.isLocal);
            openInDefaultApp.setDisable(!file.location.isLocal);
        }
    }
}
