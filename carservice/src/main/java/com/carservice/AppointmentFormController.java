package com.carservice;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class AppointmentFormController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField vinField;

    @FXML
    private TextField brandField;

    @FXML
    private TextField modelField;

    @FXML
    private TextField licenseField;

    @FXML
    private DatePicker dateField;

    @FXML
    private TextField timeField;

    @FXML
    private ListView<String> servicesListView;

    @FXML
    private ListView<String> partsListView;

    @FXML
    private Label totalPriceLabel;

    private DatabaseManager dbManager = new DatabaseManager();

    @FXML
    public void initialize() {
        loadServices();
        loadParts();
    }

    private void loadServices() {
        ObservableList<String> services = FXCollections.observableArrayList();
        String query = "SELECT name, price FROM uslugi";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                services.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        servicesListView.setItems(services);
        servicesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void loadParts() {
        ObservableList<String> parts = FXCollections.observableArrayList();
        String query = "SELECT name, price FROM zapchasti";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                parts.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        partsListView.setItems(parts);
        partsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    @FXML
    public void calculateTotalPrice() {
        double totalServicePrice = 0;
        double totalPartPrice = 0;

        // Вычисляем сумму выбранных услуг
        for (String service : servicesListView.getSelectionModel().getSelectedItems()) {
            try (PreparedStatement stmt = dbManager.getConnection().prepareStatement("SELECT price FROM uslugi WHERE name = ?")) {
                stmt.setString(1, service);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    totalServicePrice += rs.getDouble("price");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // Вычисляем сумму выбранных запчастей
        for (String part : partsListView.getSelectionModel().getSelectedItems()) {
            try (PreparedStatement stmt = dbManager.getConnection().prepareStatement("SELECT price FROM zapchasti WHERE name = ?")) {
                stmt.setString(1, part);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    totalPartPrice += rs.getDouble("price");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // Вычисляем общую сумму заказа
        double totalPrice = totalServicePrice + totalPartPrice;

        // Отображаем сумму на экране
        totalPriceLabel.setText(String.format("Общая сумма заказа: %.2f" + " Рублей", totalPrice));
    }

    private void showAlert(Alert.AlertType alertType, String title, String headerText, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    public void submitAppointment() {
        // Показываем окно подтверждения
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Подтверждение действия");
        confirmationAlert.setHeaderText("Подтвердите действие");
        confirmationAlert.setContentText("Вы уверены, что хотите записать данные?");

        Optional<ButtonType> result = confirmationAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Если пользователь подтвердил действие, продолжаем с записью данных
            String name = nameField.getText();
            String phone = phoneField.getText();
            String vin = vinField.getText();
            String brand = brandField.getText();
            String model = modelField.getText();
            String license = licenseField.getText();
            String date = dateField.getValue().toString();
            String time = timeField.getText();
            String selectedServices = String.join(",", servicesListView.getSelectionModel().getSelectedItems());
            String selectedParts = String.join(",", partsListView.getSelectionModel().getSelectedItems());

            double totalServicePrice = 0;
            double totalPartPrice = 0;

            // Вычисляем сумму выбранных услуг
            for (String service : servicesListView.getSelectionModel().getSelectedItems()) {
                try (PreparedStatement stmt = dbManager.getConnection().prepareStatement("SELECT price FROM uslugi WHERE name = ?")) {
                    stmt.setString(1, service);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        totalServicePrice += rs.getDouble("price");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            // Вычисляем сумму выбранных запчастей
            for (String part : partsListView.getSelectionModel().getSelectedItems()) {
                try (PreparedStatement stmt = dbManager.getConnection().prepareStatement("SELECT price FROM zapchasti WHERE name = ?")) {
                    stmt.setString(1, part);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        totalPartPrice += rs.getDouble("price");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            // Вычисляем общую сумму заказа
            double totalPrice = totalServicePrice + totalPartPrice;

            // Сохраняем данные в базу данных
            String zapisiQuery = "INSERT INTO zapisi (name, phone, vin, auto_mark, auto_model, gos_number, date_zapisi, time_zapisi, usluga, zapchast, price) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String clientsQuery = "INSERT INTO clients (name, phone, vin, auto_mark, auto_model, gos_number) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement zapisiStmt = dbManager.getConnection().prepareStatement(zapisiQuery);
                 PreparedStatement clientsStmt = dbManager.getConnection().prepareStatement(clientsQuery)) {

                // Запись в таблицу zapisi
                zapisiStmt.setString(1, name);
                zapisiStmt.setString(2, phone);
                zapisiStmt.setString(3, vin);
                zapisiStmt.setString(4, brand);
                zapisiStmt.setString(5, model);
                zapisiStmt.setString(6, license);
                zapisiStmt.setString(7, date);
                zapisiStmt.setString(8, time);
                zapisiStmt.setString(9, selectedServices);
                zapisiStmt.setString(10, selectedParts);
                zapisiStmt.setDouble(11, totalPrice);
                zapisiStmt.executeUpdate();

                // Запись в таблицу clients
                clientsStmt.setString(1, name);
                clientsStmt.setString(2, phone);
                clientsStmt.setString(3, vin);
                clientsStmt.setString(4, brand);
                clientsStmt.setString(5, model);
                clientsStmt.setString(6, license);
                clientsStmt.executeUpdate();

                // Показываем сообщение об успешном сохранении
                showAlert(Alert.AlertType.INFORMATION, "Успешно", "Запись добавлена успешно", "Общая сумма заказа: " + totalPrice);

                // Закрываем окно
                Stage stage = (Stage) nameField.getScene().getWindow();
                stage.close();
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Ошибка при добавлении записи", e.getMessage());
            }
        } else {
            // Если пользователь не подтвердил действие, просто закрываем окно подтверждения
            confirmationAlert.close();
        }
    }


}
