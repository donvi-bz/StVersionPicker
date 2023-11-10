package biz.donvi.syncthingversionpicker;

import atlantafx.base.theme.Styles;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.util.Comparator;
import java.util.ResourceBundle;

public class PickerController implements Initializable, EventHandler<TreeItem.TreeModificationEvent<Object>> {

    private final SyncthingScraper syncScraper = SyncPickerApp.getApplication().syncScraper;

    private ObservableList<SyncthingScraper.Folder> textFlows;

    @FXML
    private ComboBox<SyncthingScraper.Folder> comboBox;

    @FXML
    private TreeView<SyncFile> treeView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        comboBox.setItems(syncScraper.getFolders());
        comboBox.setCellFactory(c -> new ListCell<>() {
            @Override
            protected void updateItem(SyncthingScraper.Folder item, boolean empty) {
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
    }

    @FXML
    void onComboBoxChange() {
        SyncFile rootFile = new SyncFile(new File(comboBox.getValue().path()));
        var root = new TreeItem<>(rootFile, new FontIcon(Feather.FOLDER));

        // Setting Cell Factory
        treeView.setCellFactory(c -> new TreeCell<>() {
            @Override
            protected void updateItem(SyncFile file, boolean empty) {
                super.updateItem(file, empty);

                if (file == null || empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(file.getFile().getName());
                    var graphic = file.getFile().isDirectory() ? new FontIcon(Feather.FOLDER) : new FontIcon(Feather.FILE);
                    setGraphic(graphic);
                }
            }
        });

        fileScannerAndAdder(root);
        // Adding styles
        treeView.setRoot(root);
        treeView.getStyleClass().add(Styles.DENSE);
        treeView.setShowRoot(false);
    }


    void fileScannerAndAdder(TreeItem<SyncFile> parent) {
        fileScannerAndAdder(parent, true);
    }

    void fileScannerAndAdder(TreeItem<SyncFile> parent, boolean recursive) {
        var parentDir = parent.getValue();
        // This only works for directories
        if (!parentDir.getFile().isDirectory())
            return;
        // Adding Data
        File[] files = parentDir.getFile().listFiles();
        if (files != null) {
            var children = parent.getChildren();
            for (File file : files) {
                var item = new TreeItem<SyncFile>(new SyncFile(file));
                if (file.isDirectory() && recursive) {
                    fileScannerAndAdder(item, false);
                }
                children.add(item);
            }

            if (!recursive) {
                parent.addEventHandler(TreeItem.branchExpandedEvent(), this);
            }
        }
        parent.getChildren().sort(
            Comparator.comparing(TreeItem::getValue)
        );
    }

    void fileScannerAndAdder2(TreeItem<SyncFile> parent) {
        var children = parent.getChildren();
        for (TreeItem<SyncFile> child : children) {
            fileScannerAndAdder(child, true);
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"}) // Yeah... casting wasn't going as expected, but I know it should work.
    public void handle(TreeItem.TreeModificationEvent<Object> event) {
        event.getTreeItem().removeEventHandler(TreeItem.branchExpandedEvent(), this);
        fileScannerAndAdder2((TreeItem) event.getTreeItem());
    }
}
