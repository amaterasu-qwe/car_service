package com.carservice;


import eu.hansolo.tilesfx.chart.ChartData;
import javafx.event.ActionEvent;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.ValueAxis;
import javafx.scene.control.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class AdminController {

    @FXML
    private VBox adminVBox;

    @FXML
    private Label adminLabel;

    @FXML
    private TableView<Map<String, Object>> tableView;

    private DatabaseManager dbManager = new DatabaseManager();

    @FXML
    private void initialize() {

    }

    @FXML
    private void handleShowReports() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/carservice/ReportsDialog.fxml"));
            DialogPane dialogPane = loader.load();

            ReportsDialogController dialogController = loader.getController();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Выбор отчета");

            ButtonType generateReportButton = dialogPane.getButtonTypes().filtered(bt -> bt.getText().equals("Сформировать")).get(0);
            dialog.getDialogPane().lookupButton(generateReportButton).addEventFilter(ActionEvent.ACTION, event -> {
                dialog.close();
                generateReport(dialogController.getSelectedReportType(), dialogController.getStartDate(), dialogController.getEndDate());
                event.consume();
            });

            dialog.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateReport(String reportType, String startDate, String endDate) {
        if ("Отчет по доходам и расходам".equals(reportType)) {
            generateIncomeExpenseReport(startDate, endDate);
        }
        else if ("Отчет по сотрудникам".equals(reportType)) {
            generateEmployeeReport(startDate, endDate);
        } else if ("Отчет по клиентам".equals(reportType)) {
            generateClientReport(startDate, endDate);
        } else if ("Отчет по запчастям".equals(reportType)) {
            generatePartsReport(startDate, endDate);
        } else if ("Отчет по заканчивающимся запчастям".equals(reportType)) {
            generateLowStockPartsReport();
        }
    }



    private void generateClientReport(String startDate, String endDate) {
        LocalDate startLocalDate = LocalDate.parse(startDate);
        LocalDate endLocalDate = LocalDate.parse(endDate);
        String query = "SELECT " +
                "name AS \"Имя клиента\", " +
                "STRING_AGG(TO_CHAR(date_zapisi::DATE, 'YYYY-MM-DD'), ', ') AS \"Даты посещений\", " +
                "COUNT(*) AS \"Количество посещений\" " +
                "FROM zapisi " +
                "WHERE date_zapisi::DATE BETWEEN ? AND ? " +
                "GROUP BY name " +
                "ORDER BY name";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(query)) {
            stmt.setDate(1, java.sql.Date.valueOf(startLocalDate));
            stmt.setDate(2, java.sql.Date.valueOf(endLocalDate));
            ResultSet rs = stmt.executeQuery();

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Отчет по клиентам");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Имя клиента");
            header.createCell(1).setCellValue("Даты посещений");
            header.createCell(2).setCellValue("Количество посещений");

            int rowIndex = 1;
            while (rs.next()) {
                String clientName = rs.getString("Имя клиента");
                String visitDates = rs.getString("Даты посещений");
                int visitCount = rs.getInt("Количество посещений");

                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(clientName);
                row.createCell(1).setCellValue(visitDates);
                row.createCell(2).setCellValue(visitCount);
            }

            // Create a drawing canvas on the worksheet.
            XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
            // Define anchor points in the worksheet to position the chart.
            XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, rowIndex + 1, 10, rowIndex + 20);

            // Create the chart object based on the anchor point.
            XSSFChart chart = drawing.createChart(anchor);
            // Define chart title.
            chart.setTitleText("Количество посещений по клиентам");
            chart.setTitleOverlay(false);

            // Create chart legend.
            XDDFChartLegend legend = chart.getOrAddLegend();
            legend.setPosition(LegendPosition.BOTTOM);

            // Define data source for the chart.
            XDDFCategoryDataSource clients = XDDFDataSourcesFactory.fromStringCellRange((XSSFSheet) sheet, new CellRangeAddress(1, rowIndex - 1, 0, 0));
            XDDFNumericalDataSource<Double> visits = XDDFDataSourcesFactory.fromNumericCellRange((XSSFSheet) sheet, new CellRangeAddress(1, rowIndex - 1, 2, 2));

            // Create category and value axes.
            XDDFCategoryAxis categoryAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
            XDDFValueAxis valueAxis = chart.createValueAxis(AxisPosition.LEFT);
            valueAxis.setCrosses(AxisCrosses.AUTO_ZERO);

            // Create chart data.
            XDDFBarChartData data = (XDDFBarChartData) chart.createData(ChartTypes.BAR, categoryAxis, valueAxis);

            XDDFBarChartData.Series series = (XDDFBarChartData.Series) data.addSeries(clients, visits);
            series.setTitle("Посещения", null);
            data.setBarDirection(BarDirection.COL);
// Plot the chart data.
            chart.plot(data);

            // Save the workbook.
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
            String formattedDate = sdf.format(new Date());
            try (FileOutputStream fileOut = new FileOutputStream("Отчет_по_клиентам_" + formattedDate + ".xlsx")) {
                workbook.write(fileOut);
            }

            workbook.close();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Отчет сформирован");
            alert.setHeaderText(null);
            alert.setContentText("Отчет по клиентам успешно сформирован и сохранен в файл Отчет_по_клиентам_" + formattedDate + ".xlsx");
            alert.showAndWait();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }






    private void generateLowStockPartsReport() {
        String query = "SELECT name AS \"Название запчасти\", count AS \"Количество\" " +
                "FROM zapchasti " +
                "WHERE count < 5 " +
                "ORDER BY name";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(query)) {

            ResultSet rs = stmt.executeQuery();

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Отчет по низким запасам запчастей");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Название запчасти");
            header.createCell(1).setCellValue("Количество");

            int rowIndex = 1;
            while (rs.next()) {
                String partName = rs.getString("Название запчасти");
                int partCount = rs.getInt("Количество");

                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(partName);
                row.createCell(1).setCellValue(partCount);

                System.out.println("Adding row: " + partName + ", " + partCount);  // Debugging
            }

            if (rowIndex == 1) {
                System.out.println("No data found for low stock parts.");
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
            String formattedDate = sdf.format(new Date());

            try (FileOutputStream fileOut = new FileOutputStream("Отчет_по_низким_запасам_запчастей_" + formattedDate + ".xlsx")) {
                workbook.write(fileOut);
            }

            workbook.close();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Отчет сформирован");
            alert.setHeaderText(null);
            alert.setContentText("Отчет по низким запасам запчастей успешно сформирован и сохранен в файл Отчет_по_низким_запасам_запчастей_" + formattedDate + ".xlsx");
            alert.showAndWait();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }



    private void generatePartsReport(String startDate, String endDate) {
        // Преобразуйте строки в LocalDate
        LocalDate startLocalDate = LocalDate.parse(startDate);
        LocalDate endLocalDate = LocalDate.parse(endDate);

        // SQL-запрос для отчета по запчастям
        String query = "SELECT " +
                "zapchast AS \"Запчасть\", " +
                "COUNT(*) AS \"Количество использований\", " +
                "zc.price AS \"Цена за единицу\", " +
                "COUNT(*) * zc.price AS \"Общая стоимость\" " +
                "FROM ( " +
                "    SELECT unnest(string_to_array(usluga, ', ')) AS zapchast " +
                "    FROM zapisi " +
                "    WHERE date_zapisi::DATE BETWEEN ? AND ? " +
                ") z " +
                "JOIN zapchasti zc ON z.zapchast = zc.name " +
                "GROUP BY z.zapchast, zc.price " +
                "ORDER BY z.zapchast";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(query)) {
            stmt.setDate(1, java.sql.Date.valueOf(startLocalDate));
            stmt.setDate(2, java.sql.Date.valueOf(endLocalDate));

            System.out.println("Query: " + query + " with dates: " + startDate + " to " + endDate);

            ResultSet rs = stmt.executeQuery();

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Отчет по запчастям");

            // Создаем заголовок таблицы
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Запчасть");
            header.createCell(1).setCellValue("Количество использований");
            header.createCell(2).setCellValue("Цена за единицу");
            header.createCell(3).setCellValue("Общая стоимость");

            int rowIndex = 1;
            while (rs.next()) {
                String partName = rs.getString("Запчасть");
                int usageCount = rs.getInt("Количество использований");
                double unitPrice = rs.getDouble("Цена за единицу");
                double totalCost = rs.getDouble("Общая стоимость");

                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(partName);
                row.createCell(1).setCellValue(usageCount);
                row.createCell(2).setCellValue(unitPrice);
                row.createCell(3).setCellValue(totalCost);

                System.out.println("Adding row: " + partName + ", " + usageCount + ", " + unitPrice + ", " + totalCost);  // Debugging
            }

            if (rowIndex == 1) {
                System.out.println("No data found for the given date range.");
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
            String formattedDate = sdf.format(new Date());

            try (FileOutputStream fileOut = new FileOutputStream("Отчет_по_запчастям_" + formattedDate + ".xlsx")) {
                workbook.write(fileOut);
            }

            workbook.close();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Отчет сформирован");
            alert.setHeaderText(null);
            alert.setContentText("Отчет по запчастям успешно сформирован и сохранен в файл Отчет_по_запчастям_" + formattedDate + ".xlsx");
            alert.showAndWait();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }


    private void generateEmployeeReport(String startDate, String endDate) {
        String query = "SELECT name, date_work, post FROM sotrudniki WHERE date_work BETWEEN ? AND ? ORDER BY name";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(query)) {
            stmt.setString(1, startDate);
            stmt.setString(2, endDate);

            System.out.println("Query: " + query + " with dates: " + startDate + " to " + endDate);

            ResultSet rs = stmt.executeQuery();

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Отчет по сотрудникам");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ФИО");
            header.createCell(1).setCellValue("Дата начала работы");
            header.createCell(2).setCellValue("Должность");
            header.createCell(3).setCellValue("Стаж (дни)");

            int rowIndex = 1;
            while (rs.next()) {
                String name = rs.getString("name");
                Date dateWork = rs.getDate("date_work");
                String post = rs.getString("post");

                LocalDate localDateWork = ((java.sql.Date) dateWork).toLocalDate();
                long daysWorked = ChronoUnit.DAYS.between(localDateWork, LocalDate.now());

                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(name);
                row.createCell(1).setCellValue(dateWork.toString()); // Формат YYYY-MM-DD
                row.createCell(2).setCellValue(post);
                row.createCell(3).setCellValue(daysWorked);

                System.out.println("Adding row: " + name + ", " + dateWork + ", " + post + ", " + daysWorked); // Debugging
            }

            if (rowIndex == 1) {
                System.out.println("No data found for the given date range.");
            } else {
                System.out.println("Report contains " + (rowIndex - 1) + " rows of data.");
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
            String formattedDate = sdf.format(new Date());

            try (FileOutputStream fileOut = new FileOutputStream("Отчет_по_сотрудникам_" + formattedDate + ".xlsx")) {
                workbook.write(fileOut);
            }

            workbook.close();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Отчет сформирован");
            alert.setHeaderText(null);
            alert.setContentText("Отчет по сотрудникам успешно сформирован и сохранен в файл Отчет_по_сотрудникам_" + formattedDate + ".xlsx");
            alert.showAndWait();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }







    private void generateIncomeExpenseReport(String startDate, String endDate) {
        String query = "SELECT date_zapisi, price FROM zapisi WHERE date_zapisi BETWEEN ? AND ? ORDER BY date_zapisi";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(query)) {
            stmt.setString(1, startDate);
            stmt.setString(2, endDate);

            ResultSet rs = stmt.executeQuery();

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Отчет по доходам и расходам");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Дата");
            header.createCell(1).setCellValue("Доходы");
            header.createCell(2).setCellValue("Расходы");

            int rowIndex = 1;
            double totalIncome = 0;
            double totalExpense = 0;

            while (rs.next()) {
                double price = rs.getDouble("price");
                double expense = price * 0.75;

                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(rs.getString("date_zapisi"));
                row.createCell(1).setCellValue(price);
                row.createCell(2).setCellValue(expense);

                totalIncome += price;
                totalExpense += expense;
            }

            // Добавляем строку "ИТОГОВАЯ СУММА ДОХОДОВ"
            Row incomeRow = sheet.createRow(rowIndex++);
            incomeRow.createCell(0).setCellValue("ИТОГОВАЯ СУММА ДОХОДОВ");
            incomeRow.createCell(2).setCellValue(totalIncome);

            // Добавляем строку "ИТОГОВАЯ СУММА РАСХОДОВ"
            Row expenseRow = sheet.createRow(rowIndex++);
            expenseRow.createCell(0).setCellValue("ИТОГОВАЯ СУММА РАСХОДОВ");
            expenseRow.createCell(2).setCellValue(totalExpense);

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
            String formattedDate = sdf.format(new Date());

            try (FileOutputStream fileOut = new FileOutputStream("Отчет_по_доходам_и_расходам" + formattedDate + ".xlsx")) {
                workbook.write(fileOut);
            }

            workbook.close();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Отчет сформирован");
            alert.setHeaderText(null);
            alert.setContentText("Отчет успешно сформирован и сохранен в файл Отчет_по_доходам_и_расходам " + formattedDate + ".xlsx");
            alert.showAndWait();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }



    @FXML
    private void handleShowClients() {
        showTable("clients", "client_id");
    }

    @FXML
    private void handleAddClient() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/carservice/AddClientForm.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Добавить клиента");
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
            stage.setTitle("Добавление клиента");
            stage.showAndWait();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void handleShowServices() {
        showTable("uslugi", "usluga_id");
    }

    @FXML
    private void handleAddService() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/carservice/AddServiceForm.fxml"));
            Pane pane = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(pane));
            stage.setTitle("Добавление услуги");
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowParts() {
        showTable("zapchasti", "zapchast_id");
    }

    @FXML
    private void handleShowStuff() {
        showTable("sotrudniki", "sotrudnik_id");
    }

    @FXML
    private void handleAddRecordSubmit() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/carservice/AddRecordForm.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Добавление записи");
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteContent() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/carservice/DeleteContentForm.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Удалить содержимое");
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddPart() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/carservice/AddPartForm.fxml"));
            Pane pane = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(pane));
            stage.setTitle("Добавление запчасти");
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddStuff() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/carservice/AddStuffForm.fxml"));
            Pane pane = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(pane));
            stage.setTitle("Добавление сотрудника");
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void handleShowOrders() {
        showTable("zapisi", "zapis_id");
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
                    case "clients":
                        row.put("client_id", rs.getInt("client_id"));
                        row.put("name", rs.getString("name"));
                        row.put("phone", rs.getString("phone"));
                        row.put("vin", rs.getString("vin"));
                        row.put("auto_mark", rs.getString("auto_mark"));
                        row.put("auto_model", rs.getString("auto_model"));
                        row.put("gos_number", rs.getString("gos_number"));
                        break;
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
                        row.put("optPrice", rs.getInt("optPrice")); // Убедитесь, что имя совпадает
                        break;
                    case "zapisi":
                        row.put("zapis_id", rs.getInt("zapis_id"));
                        row.put("name", rs.getString("name"));
                        row.put("phone", rs.getString("phone"));
                        row.put("vin", rs.getString("vin"));
                        row.put("auto_mark", rs.getString("auto_mark"));
                        row.put("auto_model", rs.getString("auto_model"));
                        row.put("gos_number", rs.getString("gos_number"));
                        row.put("date_zapisi", rs.getString("date_zapisi"));
                        row.put("time_zapisi", rs.getString("time_zapisi"));
                        row.put("usluga", rs.getString("usluga"));
                        row.put("zapchast", rs.getString("zapchast"));
                        row.put("price", rs.getInt("price"));
                        break;
                    case "sotrudniki":
                        row.put("sotrudnik_id", rs.getInt("sotrudnik_id"));
                        row.put("name", rs.getString("name"));
                        row.put("phone", rs.getString("phone"));
                        row.put("date_birthday", rs.getString("date_birthday"));
                        row.put("date_work", rs.getString("date_work"));
                        row.put("post", rs.getString("post"));
                        break;
                }
                data.add(row);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Задаем столбцы в нужном порядке для каждой таблицы
        switch (tableName) {
            case "clients":
                addTableColumn("client_id", "ID");
                addTableColumn("name", "Фамилия, имя и отчество");
                addTableColumn("phone", "Номер телефона");
                addTableColumn("vin", "ВИН-код автомобиля");
                addTableColumn("auto_mark", "Марка автомобиля");
                addTableColumn("auto_model", "Модель автомобиля");
                addTableColumn("gos_number", "Государственный номер");
                break;
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
                addTableColumn("optPrice", "Закупочная цена"); // Убедитесь, что имя совпадает
                break;
            case "zapisi":
                addTableColumn("zapis_id", "ID");
                addTableColumn("name", "Фамилия, имя и отчество");
                addTableColumn("phone", "Номер телефона");
                addTableColumn("vin", "ВИН-код автомобиля");
                addTableColumn("auto_mark", "Марка автомобиля");
                addTableColumn("auto_model", "Модель автомобиля");
                addTableColumn("gos_number", "Государственный номер");
                addTableColumn("date_zapisi", "Дата записи на ТО");
                addTableColumn("time_zapisi", "Время записи на ТО");
                addTableColumn("usluga", "Выбранные услуги");
                addTableColumn("zapchast", "Выбранные запчасти");
                addTableColumn("price", "Итоговая цена");
                break;
            case "sotrudniki":
                addTableColumn("sotrudnik_id", "ID");
                addTableColumn("name", "Фамилия, имя и отчество");
                addTableColumn("phone", "Номер телефона");
                addTableColumn("date_birthday", "Дата рождения");
                addTableColumn("date_work", "Дата начала работы");
                addTableColumn("post", "Должность");
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
