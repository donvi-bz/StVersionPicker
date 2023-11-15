package biz.donvi.syncthingversionpicker;

import atlantafx.base.theme.Styles;
import biz.donvi.syncthingversionpicker.files.StDirectory;
import biz.donvi.syncthingversionpicker.files.StFile;
import biz.donvi.syncthingversionpicker.files.StFileGroup;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.kordamp.ikonli.evaicons.Evaicons;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class PickerController implements Initializable {

    private final SyncthingScraper syncScraper = SyncPickerApp.getApplication().syncScraper;

    private ObservableList<StFolder> textFlows;

    @FXML
    private ComboBox<StFolder> comboBox;

    @FXML
    private TreeView<StFile> treeView;

    @FXML
    private ListView<StFileGroup.File> fileGroupList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        comboBox.setItems(syncScraper.getFolders());
        comboBox.setCellFactory(c -> new ListCell<>() {
            @Override
            protected void updateItem(StFolder item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setGraphic(null);
                } else {
                    TextFlow flow = new TextFlow();
                    Text title = new Text(item.label());
                    title.setStyle("-fx-font-weight: bold");

                    Text id = new Text(String.format(" (%s)", item.id()));
                    id.setStyle("-fx-font-weight: regular");
                    id.setStyle("-fx-font-family: Monospaced");

                    flow.getChildren().addAll(title, id);
                    setGraphic(flow);
                }
            }
        });
        fileGroupList.setCellFactory(c -> new ListCell<>() {
            @Override
            protected void updateItem(StFileGroup.File item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    var graphic = new FontIcon(Evaicons.FILE);
                    switch (item.location) {
                        case LocalReal -> {}
                        case RemoteReal -> graphic.getStyleClass().add(Styles.SUCCESS);
                        case LocalVersions -> graphic.getStyleClass().add(Styles.WARNING);
                        case RemoteVersions -> graphic.getStyleClass().add(Styles.ACCENT);
                    }
                    setGraphic(graphic);
                    if (item.location == StFile.Location.LocalReal)
                        setText("Current Version");
                    else
                        setText(item.timestamp);
                }
            }
        });
        fileGroupList.getStyleClass().add(Styles.DENSE);
    }

    @FXML
    void onComboBoxChange() {
        StDirectory rootFile = StFile.newDirFromStFolder(comboBox.getValue());
        var root = new TreeItem<StFile>(rootFile, new FontIcon(Feather.FOLDER));

        // Setting Cell Factory
        treeView.setCellFactory(c -> new TreeCell<StFile>() {
            @Override
            protected void updateItem(StFile file, boolean empty) {
                super.updateItem(file, empty);

                if (file == null || empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    var graphic = file instanceof StDirectory
                        ? new FontIcon(Evaicons.FOLDER)
                        : new FontIcon(Evaicons.FILE);
                    String text = file.fileName;
                    if (file.getPrimaryLocation() != StFile.Location.LocalReal) {
                        graphic.getStyleClass().add(Styles.WARNING);
                    } else if (file instanceof StFileGroup fileGroup && fileGroup.hasNonRealLocalFiles()) {
                        text += " (" + (fileGroup.getFiles().size() - 1) + ")";
                        graphic.getStyleClass().add(Styles.ACCENT);
                    }
                    setText(text);
                    setGraphic(graphic);
                }
            }
        });
        treeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                fileGroupList.setItems(FXCollections.observableArrayList());
            }
            StFile file = newValue.getValue();
            if (file instanceof StFileGroup fileGroup) {
                fileGroupList.setItems(FXCollections.observableArrayList(fileGroup.getFiles()));
            }
        });

        fileScannerAndAdder(root);
        // Adding styles
        treeView.setRoot(root);
        treeView.getStyleClass().add(Styles.DENSE);
        treeView.setShowRoot(false);
    }


    void fileScannerAndAdder(TreeItem<StFile> parent) {
        fileScannerAndAdder(parent, true);
    }

    void fileScannerAndAdder(TreeItem<StFile> parent, boolean recursive) {
        // This only works for directories
        if (!(parent.getValue() instanceof StDirectory parentDir))
            return;
        // Adding Data
        List<StFile> files = parentDir.listFiles();
        if (!files.isEmpty()) {
            var children = parent.getChildren();
            for (StFile file : files) {
                var item = new TreeItem<>(file);
                if (file instanceof StDirectory && recursive) {
                    fileScannerAndAdder(item, false);
                }
                children.add(item);
            }

            if (!recursive) {
                parent.addEventHandler(TreeItem.branchExpandedEvent(), treeItemEventHandler);
            }
        }
        parent.getChildren().sort(
            Comparator.comparing(TreeItem::getValue)
        );
    }

    void fileScannerAndAdder2(TreeItem<StFile> parent) {
        var children = parent.getChildren();
        for (TreeItem<StFile> child : children) {
            fileScannerAndAdder(child, false);
        }
    }


    private final EventHandler<TreeItem.TreeModificationEvent<Object>> treeItemEventHandler = new EventHandler<>() {
        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        // Yeah... casting wasn't going as expected, but I know it should work.
        public void handle(TreeItem.TreeModificationEvent<Object> event) {
            event.getTreeItem().removeEventHandler(TreeItem.branchExpandedEvent(), this);
            fileScannerAndAdder2((TreeItem) event.getTreeItem());
        }
    };


}
