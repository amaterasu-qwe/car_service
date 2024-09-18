package com.carservice;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

public class AddClientFormController {

    @FXML
    private VBox adminVBox;
    @FXML
    private TextField nameField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField vinField;
    @FXML
    private TextField autoMarkField;
    @FXML
    private TextField autoModelField;
    @FXML
    private TextField gosNumberField;

    private DatabaseManager dbManager = new DatabaseManager();

    @FXML
    private void handleAddClientSubmit() {
        // Показываем окно подтверждения
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Подтверждение действия");
        confirmationAlert.setHeaderText("Подтвердите действие");
        confirmationAlert.setContentText("Вы уверены, что хотите добавить клиента?");

        Optional<ButtonType> result = confirmationAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Если пользователь подтвердил действие, продолжаем с записью данных
            String name = nameField.getText();
            String phone = phoneField.getText();
            String vin = vinField.getText();
            String autoMark = autoMarkField.getText();
            String autoModel = autoModelField.getText();
            String gosNumber = gosNumberField.getText();

            String query = "INSERT INTO clients (name, phone, vin, auto_mark, auto_model, gos_number) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(query)) {
                stmt.setString(1, name);
                stmt.setString(2, phone);
                stmt.setString(3, vin);
                stmt.setString(4, autoMark);
                stmt.setString(5, autoModel);
                stmt.setString(6, gosNumber);

                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 1) {
                    showAlert("Успешно", "Клиент добавлен в базу данных");
                    // Закрываем диалоговое окно после добавления клиента
                    Stage stage = (Stage) nameField.getScene().getWindow();
                    stage.close();
                } else {
                    System.err.println("Ошибка добавления клиента");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Ошибка", "Ошибка при добавлении клиента: " + e.getMessage());
            }
        } else {
            // Если пользователь не подтвердил действие, просто закрываем окно подтверждения
            confirmationAlert.close();
        }
    }



    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
