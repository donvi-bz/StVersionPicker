module biz.donvi.syncthingversionpicker {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;

    opens biz.donvi.syncthingversionpicker to javafx.fxml;
    exports biz.donvi.syncthingversionpicker;
}