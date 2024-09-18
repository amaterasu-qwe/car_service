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

public class AddStuffFormController {

    @FXML
    private VBox adminVBox;
    @FXML
    private TextField nameField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField birthdayField;
    @FXML
    private TextField workField;
    @FXML
    private TextField postField;

    private DatabaseManager dbManager = new DatabaseManager();

    @FXML
    private void handleAddStuffSubmit() {
        String name = nameField.getText();
        String phone = phoneField.getText();
        String date_birthday = birthdayField.getText();
        String date_work = workField.getText();
        String post = postField.getText();

        // Показываем окно подтверждения
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Подтверждение действия");
        confirmationAlert.setHeaderText("Подтвердите действие");
        confirmationAlert.setContentText("Вы уверены, что хотите добавить сотрудника?");

        Optional<ButtonType> result = confirmationAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Если пользователь подтвердил действие, продолжаем с записью данных
            String query = "INSERT INTO sotrudniki (name, phone, date_birthday, date_work, post) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(query)) {
                stmt.setString(1, name);
                stmt.setString(2, phone);
                stmt.setString(3, date_birthday);
                stmt.setString(4, date_work);
                stmt.setString(5, post);

                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 1) {
                    showAlert("Успешно", "Сотрудник добавлен в базу данных", Alert.AlertType.INFORMATION);
                    // Закрываем диалоговое окно после добавления сотрудника
                    Stage stage = (Stage) nameField.getScene().getWindow();
                    stage.close();
                } else {
                    showAlert("Ошибка", "Не удалось добавить сотрудника", Alert.AlertType.ERROR);
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
