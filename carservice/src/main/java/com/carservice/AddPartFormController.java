package com.carservice;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

public class AddPartFormController {

    @FXML
    private TextField partNameField;

    @FXML
    private TextField partPriceField;

    @FXML
    private TextField partCountField;

    private DatabaseManager dbManager = new DatabaseManager();

    @FXML
    private void handleAddPartSubmit() {
        // Показываем окно подтверждения
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Подтверждение действия");
        confirmationAlert.setHeaderText("Подтвердите действие");
        confirmationAlert.setContentText("Вы уверены, что хотите добавить запчасть?");

        Optional<ButtonType> result = confirmationAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Если пользователь подтвердил действие, продолжаем с записью данных
            String name = partNameField.getText();
            String priceStr = partPriceField.getText();
            String countStr = partCountField.getText();

            int price;
            int count;
            try {
                price = Integer.parseInt(priceStr);
            } catch (NumberFormatException e) {
                showAlert("Ошибка", "Цена должна быть числом", Alert.AlertType.ERROR);
                return;
            }

            try {
                count = Integer.parseInt(countStr);
            } catch (NumberFormatException e) {
                showAlert("Ошибка", "Количество должно быть числом", Alert.AlertType.ERROR);
                return;
            }

            String query = "INSERT INTO zapchasti (name, price, count) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(query)) {
                stmt.setString(1, name);
                stmt.setInt(2, price);
                stmt.setInt(3, count);

                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 1) {
                    showAlert("Успешно", "Запчасть добавлена в базу данных", Alert.AlertType.INFORMATION);
                    // Закрываем диалоговое окно после добавления
                    Stage stage = (Stage) partNameField.getScene().getWindow();
                    stage.close();
                } else {
                    System.err.println("Ошибка добавления запчасти");
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
