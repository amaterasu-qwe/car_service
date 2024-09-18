package com.carservice;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientController {

    private DatabaseManager dbManager = new DatabaseManager();

    @FXML
    private TableView<Map<String, Object>> tableView;
    @FXML
    private Text servicesText;
    @FXML
    private Text partsText;

    // Конструктор по умолчанию
    public ClientController() {
    }

    @FXML
    public void initialize() {
        // Любая инициализация, связанная с tableView, можно делать здесь
    }

    @FXML
    public void handleBackButton(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/carservice/Main.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Автосервис");
            stage.setScene(new Scene(root));
            stage.show();
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showServices() {
        tableView.setVisible(true);  // Показываем таблицу
        showTable("uslugi", "usluga_id"); // Загружаем данные в таблицу
        servicesText.setVisible(false);  // Скрываем текст про услуги
        partsText.setVisible(false);
    }

    @FXML
    public void showParts() {
        tableView.setVisible(true);  // Показываем таблицу
        showTable("zapchasti", "zapchast_id"); // Загружаем данные в таблицу
        servicesText.setVisible(false);
        partsText.setVisible(false);  // Скрываем текст про запчасти
    }

    @FXML
    public void showAppointmentForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/carservice/AppointmentForm.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Записаться на ТО");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showTable(String tableName, String idColumnName) {
        tableView.getColumns().clear();
        tableView.getItems().clear();

        List<Map<String, Object>> data = new ArrayList<>();

        String query = "SELECT * FROM " + tableName;

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                switch (tableName) {
                    case "uslugi":
                        row.put("usluga_id", rs.getInt("usluga_id"));
                        row.put("name", rs.getString("name"));
                        row.put("price", rs.getInt("price"));
                        break;
                    case "zapchasti":
                        row.put("zapchast_id", rs.getInt("zapchast_id"));
                        row.put("name", rs.getString("name"));
                        row.put("price", rs.getInt("price"));
                        row.put("count", rs.getInt("count"));
                        break;
                }
                data.add(row);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Задаем столбцы в нужном порядке для каждой таблицы
        switch (tableName) {
            case "uslugi":
                addTableColumn("usluga_id", "ID");
                addTableColumn("name", "Название");
                addTableColumn("price", "Цена");
                break;
            case "zapchasti":
                addTableColumn("zapchast_id", "ID");
                addTableColumn("name", "Название");
                addTableColumn("price", "Цена");
                addTableColumn("count", "Количество");
                break;
        }

        // Загружаем данные из списка в TableView
        tableView.getItems().addAll(data);
    }

    private void addTableColumn(String key, String header) {
        TableColumn<Map<String, Object>, Object> col = new TableColumn<>(header);
        col.setCellValueFactory((Callback<TableColumn.CellDataFeatures<Map<String, Object>, Object>, ObservableValue<Object>>) param ->
                new ReadOnlyObjectWrapper<>(param.getValue().get(key)));
        tableView.getColumns().add(col);
    }
}
