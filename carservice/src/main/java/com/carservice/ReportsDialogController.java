package com.carservice;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;

public class ReportsDialogController {
    @FXML
    private ComboBox<String> reportTypeComboBox;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private void initialize() {
        reportTypeComboBox.getItems().addAll("Отчет по доходам и расходам", "Отчет по сотрудникам", "Отчет по клиентам", "Отчет по запчастям", "Отчет по заканчивающимся запчастям");
    }

    public String getSelectedReportType() {
        return reportTypeComboBox.getValue();
    }

    public String getStartDate() {
        return startDatePicker.getValue() != null ? startDatePicker.getValue().toString() : "";
    }

    public String getEndDate() {
        return endDatePicker.getValue() != null ? endDatePicker.getValue().toString() : "";
    }

}
