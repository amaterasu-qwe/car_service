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

public class AddRecordController {

    @FXML
    private ComboBox<String> clientComboBox;

    @FXML
    private DatePicker dateField;

    @FXML
    private TextField timeField;

    @FXML
    private TextField priceField;

    @FXML
    private ListView<String> servicesListView;

    @FXML
    private ListView<String> partsListView;

    @FXML
    private Label totalPriceLabel;

    private DatabaseManager dbManager = new DatabaseManager();

    @FXML
    private void initialize() {
        loadClients();
        loadServices();
        loadParts();
    }

    private void loadClients() {
        String query = "SELECT name FROM clients";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                clientComboBox.getItems().add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

    @FXML
    private void handleAddRecordSubmit() {
        // Показываем окно подтверждения
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Подтверждение действия");
        confirmationAlert.setHeaderText("Подтвердите действие");
        confirmationAlert.setContentText("Вы уверены, что хотите добавить запись?");

        Optional<ButtonType> result = confirmationAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Если пользователь подтвердил действие, продолжаем с записью данных
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

            String selectedClient = clientComboBox.getSelectionModel().getSelectedItem();
            if (selectedClient == null) {
                showAlert("Ошибка", "Выберите клиента", Alert.AlertType.ERROR);
                return;
            }
            String query = "INSERT INTO zapisi (name, phone, vin, auto_mark, auto_model, gos_number, date_zapisi, time_zapisi, usluga, zapchast, price) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(query)) {
                stmt.setString(1, selectedClient);
                stmt.setString(2, getClientFieldText("phone"));
                stmt.setString(3, getClientFieldText("vin"));
                stmt.setString(4, getClientFieldText("auto_mark"));
                stmt.setString(5, getClientFieldText("auto_model"));
                stmt.setString(6, getClientFieldText("gos_number"));
                stmt.setString(7, dateField.getValue().toString());
                stmt.setString(8, timeField.getText());
                stmt.setString(9, getSelectedParts());
                stmt.setString(10, getSelectedServices());
                stmt.setDouble(11, totalPrice);

                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 1) {
                    showAlert("Успешно", "Запись добавлена", Alert.AlertType.INFORMATION);
                    // Закрываем диалоговое окно после добавления записи
                    Stage stage = (Stage) clientComboBox.getScene().getWindow();
                    stage.close();
                } else {
                    showAlert("Ошибка", "Не удалось добавить запись", Alert.AlertType.ERROR);
                }
            } catch (SQLException | NumberFormatException e) {
                e.printStackTrace();
                showAlert("Ошибка", "SQL ошибка: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            // Если пользователь не подтвердил действие, просто закрываем окно подтверждения
            confirmationAlert.close();
        }
    }

    private String getClientFieldText(String field) {
        String selectedClient = clientComboBox.getSelectionModel().getSelectedItem();
        String query = "SELECT " + field + " FROM clients WHERE name = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(query)) {
            stmt.setString(1, selectedClient);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString(field);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getSelectedParts() {
        ObservableList<String> selectedParts = partsListView.getSelectionModel().getSelectedItems();
        if (selectedParts.isEmpty()) {
            return "";
        } else {
            return String.join(", ", selectedParts);
        }
    }

    private String getSelectedServices() {
        ObservableList<String> selectedServices = servicesListView.getSelectionModel().getSelectedItems();
        if (selectedServices.isEmpty()) {
            return "";
        } else {
            return String.join(", ", selectedServices);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) clientComboBox.getScene().getWindow();
        stage.close();
    }
}
