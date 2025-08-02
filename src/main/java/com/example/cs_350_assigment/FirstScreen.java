package com.example.cs_350_assigment;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class FirstScreen {

    @FXML
    private void handleStartButton(ActionEvent event) {
        try {
            // Load the second screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Second_Screen.fxml"));
            Parent secondScreenRoot = loader.load();

            // Get the stage from the event source
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Switch the scene
            stage.setScene(new Scene(secondScreenRoot));
            stage.setTitle("Bible Quiz - Question Screen");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
