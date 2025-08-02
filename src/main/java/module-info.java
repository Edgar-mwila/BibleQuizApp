module com.example.cs_350_assigment {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;

    requires com.dlsc.formsfx;
    requires java.desktop;

    opens com.example.cs_350_assigment to javafx.fxml, com.google.gson;
    exports com.example.cs_350_assigment;
}