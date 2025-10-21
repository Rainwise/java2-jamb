module hr.ipicek.jamb {
    requires javafx.controls;
    requires javafx.fxml;

    opens hr.ipicek.jamb.controller to javafx.fxml;
    exports hr.ipicek.jamb;
}