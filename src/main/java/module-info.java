module hr.ipicek.jamb {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.rmi;
    requires java.naming;

    opens hr.ipicek.jamb.controller to javafx.fxml;
    exports hr.ipicek.jamb;
    exports hr.ipicek.jamb.network.rmi;
    exports hr.ipicek.jamb.network.protocol;
}