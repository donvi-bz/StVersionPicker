package biz.donvi.syncthingversionpicker;

import atlantafx.base.theme.Styles;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.kordamp.ikonli.evaicons.Evaicons;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class PickerController implements Initializable, EventHandler<TreeItem.TreeModificationEvent<Object>> {

    private final SyncthingScraper syncScraper = SyncPickerApp.getApplication().syncScraper;

    private ObservableList<StFolder> textFlows;

    @FXML
    private ComboBox<StFolder> comboBox;

    @FXML
    private TreeView<SyncFile> treeView;

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
    }

    @FXML
    void onComboBoxChange() {
        SyncFile rootFile = new SyncFile(
            comboBox.getValue(),
            Path.of("")
        );
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
                    var graphic = file.getRealFile().isDirectory()
                        ? new FontIcon(Evaicons.FOLDER)
                        : new FontIcon(Evaicons.FILE);
                    String text = file.getRealFile().getName();
                    if (!file.getRealFile().exists()) {
                        graphic.getStyleClass().add(Styles.WARNING);
                    }else if (!file.getPreviousVersions().isEmpty() && !file.isDirectory()) {
                        text += " (" + file.getPreviousVersions().size() + ")";
                        graphic.getStyleClass().add(Styles.ACCENT);
                    }
                    setText(text);
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
        SyncFile parentDir = parent.getValue();
        // This only works for directories
        if (!parentDir.getRealFile().isDirectory())
            return;
        // Adding Data
        StFolder folder = comboBox.getValue();
        List<SyncFile> files = parentDir.listFiles();
        if (!files.isEmpty()) {
            var children = parent.getChildren();
            for (SyncFile file : files) {
                var item = new TreeItem<SyncFile>(file);
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
            fileScannerAndAdder(child, false);
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"}) // Yeah... casting wasn't going as expected, but I know it should work.
    public void handle(TreeItem.TreeModificationEvent<Object> event) {
        event.getTreeItem().removeEventHandler(TreeItem.branchExpandedEvent(), this);
        fileScannerAndAdder2((TreeItem) event.getTreeItem());
    }
}
