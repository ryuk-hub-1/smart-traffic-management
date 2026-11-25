package com.smarttraffic.controller.admin;

import com.smarttraffic.dao.*;
import com.smarttraffic.model.Challan;
import com.smarttraffic.model.enums.ChallanStatus;
import com.smarttraffic.model.enums.ComplaintStatus;
import com.smarttraffic.model.enums.DutyStatus;
import com.smarttraffic.model.enums.EmergencySeverity;
import com.smarttraffic.util.ThreadPoolManager;
import com.smarttraffic.view.ChallanReceiptDialog;
import com.smarttraffic.view.UIComponents;
import com.smarttraffic.view.ViewNavigator;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controller for the Main Admin Dashboard.
 * Displays real-time department statistics, visual charts, and recent activity logs.
 */
public class AdminDashboardController {
    private final VBox root = new VBox(20);

    private final OfficerDao officerDao = new OfficerDao();
    private final DutyDao dutyDao = new DutyDao();
    private final ChallanDao challanDao = new ChallanDao();
    private final ComplaintDao complaintDao = new ComplaintDao();
    private final EmergencyDao emergencyDao = new EmergencyDao();

    public AdminDashboardController() {
        buildUI();
    }

    public Node getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(10, 10, 30, 10));

        // Header
        VBox header = UIComponents.createViewHeader(
                "Department Operations Overview",
                "Real-time monitoring of traffic personnel, enforcement metrics, violations, and emergency alerts"
        );

        // 1. KPI Cards Row
        HBox kpiRow = new HBox(15);
        kpiRow.setAlignment(Pos.CENTER);

        // We fetch counts asynchronously to keep UI responsive
        VBox cardOfficers = UIComponents.createKpiCard("Active Officers", "...", "Click to view roster", "#2563eb");
        VBox cardDuties = UIComponents.createKpiCard("Today's Duties", "...", "Click to view shifts", "#059669");
        VBox cardViolations = UIComponents.createKpiCard("Total Violations", "...", "Click to view challans", "#f59e0b");
        VBox cardRevenue = UIComponents.createKpiCard("Collected Fines", "...", "Click for revenue", "#10b981");
        VBox cardComplaints = UIComponents.createKpiCard("Pending Grievances", "...", "Click to review", "#8b5cf6");
        VBox cardAccidents = UIComponents.createKpiCard("Critical Alerts", "...", "Click for dispatch", "#ef4444");

        cardOfficers.setStyle(cardOfficers.getStyle() + "; -fx-cursor: hand;");
        cardOfficers.setOnMouseClicked(e -> ViewNavigator.loadMainContent(new OfficerManagementController().getView(), "Officer"));

        cardDuties.setStyle(cardDuties.getStyle() + "; -fx-cursor: hand;");
        cardDuties.setOnMouseClicked(e -> ViewNavigator.loadMainContent(new DutyAllocationController().getView(), "Duty"));

        cardViolations.setStyle(cardViolations.getStyle() + "; -fx-cursor: hand;");
        cardViolations.setOnMouseClicked(e -> ViewNavigator.loadMainContent(new AdminChallansController().getView(), "Challans"));

        cardRevenue.setStyle(cardRevenue.getStyle() + "; -fx-cursor: hand;");
        cardRevenue.setOnMouseClicked(e -> ViewNavigator.loadMainContent(new AdminChallansController().getView(), "Challans"));

        cardComplaints.setStyle(cardComplaints.getStyle() + "; -fx-cursor: hand;");
        cardComplaints.setOnMouseClicked(e -> ViewNavigator.loadMainContent(new AdminComplaintsController().getView(), "Complaints"));

        cardAccidents.setStyle(cardAccidents.getStyle() + "; -fx-cursor: hand;");
        cardAccidents.setOnMouseClicked(e -> ViewNavigator.loadMainContent(new AdminEmergencyController().getView(), "Emergency"));

        kpiRow.getChildren().addAll(cardOfficers, cardDuties, cardViolations, cardRevenue, cardComplaints, cardAccidents);

        // 2. Visual Charts Row
        HBox chartsRow = new HBox(20);
        chartsRow.setAlignment(Pos.CENTER);

        // Pie Chart: Violations by Category
        PieChart violationCategoryChart = new PieChart();
        violationCategoryChart.setTitle("Violations by Offense Category");
        violationCategoryChart.setLegendVisible(true);
        violationCategoryChart.setPrefHeight(320);

        VBox chartCard1 = new VBox(10, violationCategoryChart);
        chartCard1.getStyleClass().add("content-card");
        HBox.setHgrow(chartCard1, Priority.ALWAYS);

        // Bar Chart: Challans Payment Breakdown
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Payment Status");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Fine Amount (₹)");
        BarChart<String, Number> paymentBarChart = new BarChart<>(xAxis, yAxis);
        paymentBarChart.setTitle("Fine Value Breakdown (₹)");
        paymentBarChart.setLegendVisible(false);
        paymentBarChart.setPrefHeight(320);

        VBox chartCard2 = new VBox(10, paymentBarChart);
        chartCard2.getStyleClass().add("content-card");
        HBox.setHgrow(chartCard2, Priority.ALWAYS);

        chartsRow.getChildren().addAll(chartCard1, chartCard2);

        // 3. Recent Violations Table
        VBox recentCard = new VBox(10);
        recentCard.getStyleClass().add("content-card");

        HBox tblHeader = new HBox(10);
        tblHeader.setAlignment(Pos.CENTER_LEFT);
        Label tblTitle = new Label("Recent Traffic Challans Issued");
        tblTitle.getStyleClass().add("section-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button refreshBtn = new Button("🔄 Refresh Data");
        refreshBtn.getStyleClass().add("btn-secondary");

        Button viewAllBtn = new Button("View All Citations ➔");
        viewAllBtn.getStyleClass().add("btn-primary");
        viewAllBtn.setOnAction(e -> ViewNavigator.loadMainContent(new AdminChallansController().getView(), "Challans"));

        tblHeader.getChildren().addAll(tblTitle, spacer, refreshBtn, viewAllBtn);

        TableView<Challan> table = new TableView<>();
        table.setPrefHeight(250);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Challan, String> colChallanNo = new TableColumn<>("Challan No");
        colChallanNo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getChallanNumber()));

        TableColumn<Challan, String> colVehNo = new TableColumn<>("Vehicle Number");
        colVehNo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getVehicleNumber()));

        TableColumn<Challan, String> colOffense = new TableColumn<>("Violation Type");
        colOffense.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getViolationTitle()));

        TableColumn<Challan, String> colJunction = new TableColumn<>("Junction Location");
        colJunction.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getJunctionName()));

        TableColumn<Challan, String> colFine = new TableColumn<>("Fine Amount");
        colFine.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedFine()));

        TableColumn<Challan, String> colStatus = new TableColumn<>("Payment Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPaymentStatus().getLabel()));

        TableColumn<Challan, Void> colAction = new TableColumn<>("Action");
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("View Receipt");
            {
                btn.getStyleClass().add("btn-secondary");
                btn.getStyleClass().add("btn-small");
                btn.setOnAction(e -> {
                    Challan c = getTableView().getItems().get(getIndex());
                    ChallanReceiptDialog.show(c, ViewNavigator.getPrimaryStage());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        table.getColumns().addAll(colChallanNo, colVehNo, colOffense, colJunction, colFine, colStatus, colAction);
        recentCard.getChildren().addAll(tblHeader, table);

        root.getChildren().addAll(header, kpiRow, chartsRow, recentCard);

        // Async Data Loader Runnable
        Runnable loadData = () -> {
            try {
                int totalOfficers = officerDao.findAll().size();
                long activeDuties = dutyDao.findByDate(LocalDate.now()).stream()
                        .filter(d -> d.getStatus() == DutyStatus.IN_PROGRESS || d.getStatus() == DutyStatus.SCHEDULED)
                        .count();
                List<Challan> allChallans = challanDao.findAll();
                long totalViolations = allChallans.size();

                double collectedFine = allChallans.stream()
                        .filter(c -> c.getPaymentStatus() == ChallanStatus.PAID)
                        .mapToDouble(Challan::getFineAmount)
                        .sum();

                double pendingFine = allChallans.stream()
                        .filter(c -> c.getPaymentStatus() == ChallanStatus.PENDING || c.getPaymentStatus() == ChallanStatus.OVERDUE)
                        .mapToDouble(Challan::getFineAmount)
                        .sum();

                long openComplaints = complaintDao.findAll().stream()
                        .filter(c -> c.getStatus() == ComplaintStatus.PENDING || c.getStatus() == ComplaintStatus.INVESTIGATING)
                        .count();

                long criticalAccidents = emergencyDao.findAll().stream()
                        .filter(e -> e.getSeverity() == EmergencySeverity.CRITICAL || e.getSeverity() == EmergencySeverity.MAJOR)
                        .count();

                Map<String, Integer> categoryStats = challanDao.getViolationsByCategoryStats();

                Platform.runLater(() -> {
                    // Update KPI cards
                    ((Label) cardOfficers.getChildren().get(1)).setText(String.valueOf(totalOfficers));
                    ((Label) cardDuties.getChildren().get(1)).setText(String.valueOf(activeDuties));
                    ((Label) cardViolations.getChildren().get(1)).setText(String.valueOf(totalViolations));
                    ((Label) cardRevenue.getChildren().get(1)).setText(String.format("₹ %,.0f", collectedFine));
                    ((Label) cardComplaints.getChildren().get(1)).setText(String.valueOf(openComplaints));
                    ((Label) cardAccidents.getChildren().get(1)).setText(String.valueOf(criticalAccidents));

                    // Update Pie Chart
                    ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
                    categoryStats.forEach((cat, cnt) -> {
                        if (cnt > 0) {
                            pieData.add(new PieChart.Data(cat + " (" + cnt + ")", cnt));
                        }
                    });
                    if (pieData.isEmpty()) {
                        pieData.add(new PieChart.Data("No Violations", 1));
                    }
                    violationCategoryChart.setData(pieData);

                    // Update Bar Chart
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.getData().add(new XYChart.Data<>("Paid Revenue", collectedFine));
                    series.getData().add(new XYChart.Data<>("Outstanding Dues", pendingFine));
                    paymentBarChart.getData().clear();
                    paymentBarChart.getData().add(series);

                    // Update Recent Table (latest 10)
                    ObservableList<Challan> topList = FXCollections.observableArrayList(
                            allChallans.stream().limit(10).toList()
                    );
                    table.setItems(topList);
                });

            } catch (Exception e) {
                Platform.runLater(() -> UIComponents.showErrorAlert("Dashboard Error", "Failed to load dashboard data: " + e.getMessage()));
            }
        };

        refreshBtn.setOnAction(e -> ThreadPoolManager.getInstance().execute(loadData));

        // Initial async load
        ThreadPoolManager.getInstance().execute(loadData);
    }
}
