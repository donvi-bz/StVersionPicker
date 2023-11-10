package biz.donvi.syncthingversionpicker;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.StringConverter;
import javafx.util.converter.FormatStringConverter;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class PickerController implements Initializable {

    private final SyncthingScraper syncScraper = SyncPickerApp.getApplication().syncScraper;

    private ObservableList<SyncthingScraper.Folder> textFlows;

    @FXML
    private ComboBox<SyncthingScraper.Folder> comboBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        comboBox.setItems(syncScraper.getFolders());
        comboBox.setCellFactory(c -> new ListCell<>(){
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
}
