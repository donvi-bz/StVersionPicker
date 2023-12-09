package biz.donvi.syncthingversionpicker;

import atlantafx.base.theme.Styles;
import biz.donvi.syncthingversionpicker.files.StDirectory;
import biz.donvi.syncthingversionpicker.files.StFile;
import biz.donvi.syncthingversionpicker.files.StFile.Location;
import biz.donvi.syncthingversionpicker.files.StFileGroup;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.kordamp.ikonli.evaicons.Evaicons;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;

public class PickerController implements Initializable {

    public record DoubleStFolder(StFolder local, StFolder remote) implements Comparable<DoubleStFolder> {

        @Override
        public String toString() {
            return first().toString();
        }

        StFolder first() {
            if (local != null)
                return local;
            else return remote;
        }

        String label() {
            if (local != null)
                return local.label();
            if (remote != null)
                return remote.label();
            return "!";
        }

        String id() {
            if (local != null)
                return local.id();
            if (remote != null)
                return remote.id();
            return "!";
        }

        static ObservableList<DoubleStFolder> combine(List<StFolder> locals, List<StFolder> remotes) {
            HashMap<String, DoubleStFolder> map = new HashMap<>();
            for (var local : locals)
                map.put(local.id(), new DoubleStFolder(local, null));
            DoubleStFolder d;
            for (var remote : remotes)
                if ((d = map.get(remote.id())) != null)
                    map.put(remote.id(), new DoubleStFolder(d.local, remote));
                else
                    map.put(remote.id(), new DoubleStFolder(null, remote));
            return FXCollections.observableArrayList(map.values().stream().sorted().toList());
        }

        @Override
        public int compareTo(DoubleStFolder o) {
            return this.label().compareTo(o.label());
        }
    }


    private ObservableList<StFolder> textFlows;

    @FXML
    private ComboBox<DoubleStFolder> comboBox;

    @FXML
    private TreeView<StFile> treeView;

    @FXML
    private ListView<StFileGroup.File> fileGroupList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SyncPickerApp app = SyncPickerApp.getApplication();
        comboBox.setCellFactory(c -> new ListCell<>() {
            @Override
            protected void updateItem(DoubleStFolder item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setGraphic(null);
                } else {
                    TextFlow outerTextFlow = new TextFlow();

                    Text title = new Text(item.label());
                    title.setStyle("-fx-font-weight: bold");

                    Text id = new Text(String.format(" (%s)", item.id()));
                    id.setStyle("-fx-font-weight: regular");
                    id.setStyle("-fx-font-family: Monospaced");

                    outerTextFlow.getChildren().addAll(title, id);

                    if (item.local != null) {
                        TextFlow localBadge = new TextFlow(new Text("local"));
                        localBadge.getStyleClass().addAll("badge", "b-blue");
                        outerTextFlow.getChildren().addAll(localBadge, new Text(" "));
                    }
                    if (item.remote != null) {
                        TextFlow remoteBadge = new TextFlow(new Text("remote"));
                        remoteBadge.getStyleClass().addAll("badge", "b-purple");
                        outerTextFlow.getChildren().add(remoteBadge);
                    }


                    setGraphic(outerTextFlow);
                }
            }
        });
        comboBox.setItems(DoubleStFolder.combine(
            app.localSyncScraper.getFolders(),
            app.remoteSyncScraper.getFolders()
        ));

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
                    if (item.location == Location.LocalReal)
                        setText("Current Version");
                    else
                        setText(item.getTimeStamp() + "\t|   " + item.getTimeAgo(LocalDateTime.now()));

//                    TextFlow textFlow = new TextFlow();
//                    Text textLeft = new Text(item.getTimeStamp());
//                    textLeft.setTextAlignment(TextAlignment.LEFT);
//                    Text textMiddle = new Text(" | ");
//                    textMiddle.setTextAlignment(TextAlignment.CENTER);
//                    Text textRight = new Text(item.getTimeStamp());
//                    textRight.setTextAlignment(TextAlignment.RIGHT);
//                    textFlow.getChildren().addAll(textLeft, textMiddle, textRight);
//                    setGraphic(textFlow);
                }
            }
        });

        // Setting Cell Factory
        treeView.setCellFactory(c -> new TreeCell<StFile>() {

            private final FontIcon folderGraphic = new FontIcon(Evaicons.FOLDER);
            private final FontIcon fileGraphic = new FontIcon(Evaicons.FILE);

            private final HBox outerBox = new HBox();
            private final Text fileName = new Text();
            private final TextFlow localFlow = new TextFlow();
            private final Text localText = new Text();
            private final TextFlow remoteFlow = new TextFlow();
            private final Text remoteText = new Text();

            {
                localFlow.getChildren().add(localText);
                localFlow.getStyleClass().addAll("badge", "b-blue");
                remoteFlow.getChildren().add(remoteText);
                remoteFlow.getStyleClass().addAll("badge", "b-purple");
                outerBox.setSpacing(4);
                outerBox.getChildren().addAll(folderGraphic, fileName, localFlow, remoteFlow);
            }

            @Override
            protected void updateItem(StFile file, boolean empty) {
                super.updateItem(file, empty);

                if (file == null || empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText("");
                    setGraphic(outerBox);
                    fileName.setText(file.fileName);
                    if (file instanceof StFileGroup group) {
                        if (!outerBox.getChildren().isEmpty())
                            outerBox.getChildren().set(0, fileGraphic);
                        setOrHideFlow(Location.LocalVersions, group.countFiles(Location.LocalVersions));
                        setOrHideFlow(Location.RemoteVersions, group.countFiles(Location.RemoteVersions));
                    } else {
                        if (!outerBox.getChildren().isEmpty())
                            outerBox.getChildren().set(0, folderGraphic);
                        setOrHideFlow(Location.LocalVersions, 0);
                        setOrHideFlow(Location.RemoteVersions, 0);
                    }
                }
            }

            private void setOrHideFlow(Location loc, long count) {
                var correctText = switch (loc) {
                    case LocalVersions -> localText;
                    case RemoteVersions -> remoteText;
                    case LocalReal, RemoteReal -> null;
                };
                assert correctText != null; // That should be checked at compile time by the dev.
                var correctTextParent = correctText.getParent();
                outerBox.getChildren().remove(correctTextParent);
                if (count > 0) {
                    outerBox.getChildren().add(correctTextParent);
                    correctText.setText(String.valueOf(count));
                }
            }

        });
        treeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                fileGroupList.setItems(FXCollections.observableArrayList());
            } else if (newValue.getValue() instanceof StFileGroup fileGroup) {
                fileGroupList.setItems(FXCollections.observableArrayList(fileGroup.getFiles()));
            }
        });

        fileGroupList.getStyleClass().add(Styles.DENSE);
    }

    @FXML
    void onComboBoxChange() {
        StDirectory rootFile = StFile.newDirFromStFolder(
            comboBox.getValue(),
            SyncPickerApp.getApplication().remoteLister
        );
        var root = new TreeItem<StFile>(rootFile, new FontIcon(Feather.FOLDER));


        scanAndAddFiles(root);
        // Adding styles
        treeView.setRoot(root);
        treeView.getStyleClass().add(Styles.DENSE);
        treeView.setShowRoot(false);
    }


    void scanAndAddFiles(TreeItem<StFile> parent) {
        scanAndAddFiles(parent, true);
    }

    void scanAndAddFiles(TreeItem<StFile> parent, boolean recursive) {
        // This only works for directories
        if (!(parent.getValue() instanceof StDirectory parentDir))
            return;
        // Taking care of this now...
        if (!recursive)
            parent.addEventHandler(TreeItem.branchExpandedEvent(), treeItemEventHandler);
        // Adding Data. We have to do each location separately.
        parentDir.listFilesAsync().thenAcceptAsync(files -> {
            // Now the logic for adding files once we actually get them.
            var children = parent.getChildren();
            for (StFile file : files) {
                var item = new TreeItem<>(file);
                if (file instanceof StDirectory && recursive)
                    scanAndAddFiles(item, false);
                children.add(item);
            }
            parent.getChildren().sort(Comparator.comparing(TreeItem::getValue));
        }, Platform::runLater);
    }

    void fileScannerAndAdder2(TreeItem<StFile> parent) {
        var children = parent.getChildren();
        for (TreeItem<StFile> child : children) {
            scanAndAddFiles(child, false);
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
