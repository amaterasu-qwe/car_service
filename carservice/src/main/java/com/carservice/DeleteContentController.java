package com.carservice;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

public class DeleteContentController {

    @FXML
    private ListView<String> uslugiListView;

    @FXML
    private ListView<String> zapchastiListView;

    @FXML
    private ListView<String> clientsListView;

    @FXML
    private ListView<String> sotrudnikiListView;

    private DatabaseManager dbManager = new DatabaseManager();

    @FXML
    private void initialize() {
        uslugiListView.setItems(getListItems("uslugi", "name"));
        zapchastiListView.setItems(getListItems("zapchasti", "name"));
        clientsListView.setItems(getListItems("clients", "name"));
        sotrudnikiListView.setItems(getListItems("sotrudniki", "name"));

        // Разрешаем множественный выбор в ListView
        uslugiListView.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        zapchastiListView.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        clientsListView.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        sotrudnikiListView.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
    }

    private ObservableList<String> getListItems(String tableName, String columnName) {
        ObservableList<String> items = FXCollections.observableArrayList();

        String query = "SELECT " + columnName + " FROM " + tableName;

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(query);
             var rs = stmt.executeQuery()) {

            while (rs.next()) {
                items.add(rs.getString(columnName));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return items;
    }

    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleDelete() {
        // Показываем окно подтверждения
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Подтверждение действия");
        confirmationAlert.setHeaderText("Подтвердите действие");
        confirmationAlert.setContentText("Вы уверены, что хотите удалить выбранные элементы?");

        Optional<ButtonType> result = confirmationAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Если пользователь подтвердил действие, продолжаем с удалением данных
            deleteSelectedItems("uslugi", "name", uslugiListView.getSelectionModel().getSelectedItems());
            deleteSelectedItems("zapchasti", "name", zapchastiListView.getSelectionModel().getSelectedItems());
            deleteSelectedItems("clients", "name", clientsListView.getSelectionModel().getSelectedItems());
            deleteSelectedItems("sotrudniki", "name", sotrudnikiListView.getSelectionModel().getSelectedItems());

            showAlert("Успешно", "Данные успешно удалены.", Alert.AlertType.INFORMATION);

            Stage stage = (Stage) uslugiListView.getScene().getWindow();
            stage.close();
        } else {
            // Если пользователь не подтвердил действие, просто закрываем окно подтверждения
            confirmationAlert.close();
        }
    }

    private void deleteSelectedItems(String tableName, String columnName, ObservableList<String> selectedItems) {
        if (selectedItems.isEmpty()) {
            return;
        }

        String query = "DELETE FROM " + tableName + " WHERE " + columnName + " = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(query)) {
            for (String item : selectedItems) {
                stmt.setString(1, item);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
