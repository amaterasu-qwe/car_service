package com.carservice;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;

import java.io.IOException;

public class AdminLoginController {

    @FXML
    private TextField loginField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Text actionTarget;

    @FXML
    protected void handleSubmitButtonAction() {
        String login = loginField.getText();
        String password = passwordField.getText();

        if (login.equals("admin") && password.equals("admin")) {
            actionTarget.setText("Вход выполнен");

            // Закрытие текущего окна
            Stage stage = (Stage) actionTarget.getScene().getWindow();
            stage.close();

            // Открытие окна интерфейса для администратора
            openAdminInterface();
        } else {
            actionTarget.setText("Неверные данные");
        }
    }

    private void openAdminInterface() {
        try {
            Stage adminStage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Admin.fxml"));
            Parent root = loader.load();
            AdminController adminController = loader.getController();
            Scene scene = new Scene(root, 826, 866);
            adminStage.setTitle("Панель администратора");
            adminStage.setScene(scene);
            adminStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

