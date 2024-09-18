package com.carservice;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainController {

    @FXML
    protected void openClientInterface(javafx.event.ActionEvent event) {
        openWindow(event,"Client.fxml", "Интерфейс клиента", 600, 740);
    }

    @FXML
    protected void openAdminLogin(javafx.event.ActionEvent event) {
        openWindow(event, "AdminLogin.fxml", "Авторизация администратора", 400, 600);
    }

    private void openWindow(ActionEvent event, String fxmlFile, String title, int width, int height) {
        try {
            Stage stage = new Stage();
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            stage.setTitle(title);
            stage.setScene(new Scene(root, width, height));
            stage.show();
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
