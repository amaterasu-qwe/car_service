package com.carservice;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

public class AddServiceController {

    @FXML
    private TextField serviceNameField;

    @FXML
    private TextField servicePriceField;

    private DatabaseManager dbManager = new DatabaseManager();

    @FXML
    private void handleAddService() {
        String name = serviceNameField.getText();
        String priceStr = servicePriceField.getText();

        if (name.isEmpty() || priceStr.isEmpty()) {
            showAlert("Ошибка", "Заполните все поля", Alert.AlertType.ERROR);
            return;
        }

        int price;
        try {
            price = Integer.parseInt(priceStr);
        } catch (NumberFormatException e) {
            showAlert("Ошибка", "Цена должна быть числом", Alert.AlertType.ERROR);
            return;
        }

        // Показываем окно подтверждения
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Подтверждение действия");
        confirmationAlert.setHeaderText("Подтвердите действие");
        confirmationAlert.setContentText("Вы уверены, что хотите добавить услугу?");

        Optional<ButtonType> result = confirmationAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Если пользователь подтвердил действие, продолжаем с записью данных
            String query = "INSERT INTO uslugi (name, price) VALUES (?, ?)";
            try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(query)) {
                stmt.setString(1, name);
                stmt.setInt(2, price);

                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 1) {  // Check for exactly 1 row inserted
                    showAlert("Успешно", "Услуга добавлена в базу данных", Alert.AlertType.INFORMATION);
                    Stage stage = (Stage) serviceNameField.getScene().getWindow();
                    stage.close();
                } else {
                    showAlert("Ошибка", "Не удалось добавить услугу", Alert.AlertType.ERROR);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Ошибка", "SQL ошибка: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            // Если пользователь не подтвердил действие, просто закрываем окно подтверждения
            confirmationAlert.close();
        }
    }

    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
