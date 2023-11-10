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
    private TreeView<File> treeView;

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
        File rootFile = new File(comboBox.getValue().path());
        var root = new TreeItem<>(rootFile, new FontIcon(Feather.FOLDER));

        // Setting Cell Factory
        treeView.setCellFactory(c -> new TreeCell<>() {
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);

                if (file == null || empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(file.getName());
                    var graphic = file.isDirectory() ? new FontIcon(Feather.FOLDER) : new FontIcon(Feather.FILE);
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


    void fileScannerAndAdder(TreeItem<File> parent) {
        fileScannerAndAdder(parent, true);
    }

    void fileScannerAndAdder(TreeItem<File> parent, boolean recursive) {
        var parentDir = parent.getValue();
        // This only works for directories
        if (!parentDir.isDirectory())
            return;
        // Adding Data
        File[] files = parentDir.listFiles();
        if (files != null) {
            var children = parent.getChildren();
            for (File file : files) {
                var item = new TreeItem<File>(file);
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

    void fileScannerAndAdder2(TreeItem<File> parent) {
        var children = parent.getChildren();
        for (TreeItem<File> child : children) {
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
