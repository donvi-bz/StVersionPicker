module biz.donvi.syncthingversionpicker {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires atlantafx.base;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;

    opens biz.donvi.syncthingversionpicker to javafx.fxml, com.fasterxml.jackson.databind;
    exports biz.donvi.syncthingversionpicker;
}