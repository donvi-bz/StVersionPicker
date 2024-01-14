package biz.donvi.syncthingversionpicker.controllers;

import atlantafx.base.theme.Styles;
import biz.donvi.syncthingversionpicker.files.StFileGroup;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class PickerTableComponentController implements Initializable {

    @FXML
    private TableView<StFileGroup.File> fileGroupTable;

    @FXML
    public TableColumn<StFileGroup.File, String> columnType;
    @FXML
    public TableColumn<StFileGroup.File, String> columnLocation;
    @FXML
    public TableColumn<StFileGroup.File, String> columnDateCreated;
    @FXML
    public TableColumn<StFileGroup.File, String> columnTimeSinceCreation;
    @FXML
    public TableColumn<StFileGroup.File, String> columnName;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        fileGroupTable.setRowFactory(x -> new TableRow<>() {
            @Override
            protected void updateItem(StFileGroup.File file, boolean empty) {
                super.updateItem(file, empty);
                if (file == null || empty) {
                    setStyle("");
                    return;
                }
                String style = "";
                if (file.location.isReal)
                    style += "-fx-font-weight: bold;";

                if (file.location.isLocal)
                    style += "-fx-background-color: -color-blue;";
                else
                    style += "-fx-background-color: -color-purple;";

                setStyle(style);
            }
        });

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
}
