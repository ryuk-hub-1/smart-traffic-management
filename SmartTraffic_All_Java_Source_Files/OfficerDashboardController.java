package com.smarttraffic.controller.officer;

import com.smarttraffic.model.Attendance;
import com.smarttraffic.model.Challan;
import com.smarttraffic.model.DutyAssignment;
import com.smarttraffic.model.Officer;
import com.smarttraffic.model.User;
import com.smarttraffic.service.AuthService;
import com.smarttraffic.service.ChallanService;
import com.smarttraffic.service.DutyService;
import com.smarttraffic.util.DateUtil;
import com.smarttraffic.util.ThreadPoolManager;
import com.smarttraffic.view.ChallanReceiptDialog;
import com.smarttraffic.view.UIComponents;
import com.smarttraffic.view.ViewNavigator;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Optional;

/**
 * Controller for Officer Personal Dashboard.
 * Displays officer duty status, quick shift check-in/out, and activity metrics.
 */
public class OfficerDashboardController {
    private final VBox root = new VBox(20);
    private final DutyService dutyService = DutyService.getInstance();
    private final ChallanService challanService = ChallanService.getInstance();

    private final TableView<Challan> recentTable = new TableView<>();

    public OfficerDashboardController() {
        buildUI();
    }

    public Node getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(10, 10, 30, 10));

        User user = AuthService.getInstance().getCurrentUser();
        Officer officer = (user instanceof Officer o) ? o : null;

        String offName = officer != null ? officer.getRankAndName() : "Officer";
        String jName = officer != null && officer.getAssignedJunctionName() != null ? officer.getAssignedJunctionName() : "Unassigned Junction";

        VBox header = UIComponents.createViewHeader(
                "Officer Operational Console: " + offName,
                "Assigned Post: " + jName + " • " + (officer != null ? officer.getPoliceStation() : "")
        );

        // 1. KPI & Attendance Quick Action Cards
        HBox kpiRow = new HBox(15);
        kpiRow.setAlignment(Pos.CENTER);

        VBox cardJunction = UIComponents.createKpiCard("Deployment Post", jName, "Click to view duties", "#2563eb");
        VBox cardShift = UIComponents.createKpiCard("Today's Shift", "...", "Click to view roster", "#059669");
        VBox cardAttendance = UIComponents.createKpiCard("Attendance Status", "...", "Daily check-in", "#f59e0b");
        VBox cardViolations = UIComponents.createKpiCard("Challans Issued", "...", "Click to view history", "#8b5cf6");

        cardJunction.setStyle(cardJunction.getStyle() + "; -fx-cursor: hand;");
        cardJunction.setOnMouseClicked(e -> ViewNavigator.loadMainContent(new OfficerDutyController().getView(), "Shift"));

        cardShift.setStyle(cardShift.getStyle() + "; -fx-cursor: hand;");
        cardShift.setOnMouseClicked(e -> ViewNavigator.loadMainContent(new OfficerDutyController().getView(), "Shift"));

        cardAttendance.setStyle(cardAttendance.getStyle() + "; -fx-cursor: hand;");
        cardAttendance.setOnMouseClicked(e -> ViewNavigator.loadMainContent(new OfficerDutyController().getView(), "Shift"));

        cardViolations.setStyle(cardViolations.getStyle() + "; -fx-cursor: hand;");
        cardViolations.setOnMouseClicked(e -> ViewNavigator.loadMainContent(new OfficerChallansController().getView(), "Issued Challans"));

        kpiRow.getChildren().addAll(cardJunction, cardShift, cardAttendance, cardViolations);

        // Attendance Quick Actions Bar
        HBox attBar = new HBox(15);
        attBar.getStyleClass().add("content-card");
        attBar.setAlignment(Pos.CENTER_LEFT);

        Label attPrompt = new Label("Shift Attendance Control:");
        attPrompt.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");

        Button checkInBtn = new Button("🟢 Mark Shift Check-In");
        checkInBtn.getStyleClass().add("btn-success");

        Button checkOutBtn = new Button("🔴 Mark Shift Check-Out");
        checkOutBtn.getStyleClass().add("btn-danger");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusNotice = new Label("Ready to record shift check-in");
        statusNotice.setStyle("-fx-text-fill: #64748b; -fx-font-style: italic;");

        attBar.getChildren().addAll(attPrompt, checkInBtn, checkOutBtn, spacer, statusNotice);

        // Recent Challans Issued By Officer
        VBox recentBox = new VBox(10);
        recentBox.getStyleClass().add("content-card");

        HBox tblHeader = new HBox(10);
        tblHeader.setAlignment(Pos.CENTER_LEFT);
        Label tblTitle = new Label("Your Recently Issued Challans");
        tblTitle.getStyleClass().add("section-title");

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        Button newChallanBtn = new Button("⚡ Issue New Challan");
        newChallanBtn.getStyleClass().add("btn-primary");
        newChallanBtn.setOnAction(e -> ViewNavigator.loadMainContent(new LogViolationController().getView(), "Log Violation"));

        tblHeader.getChildren().addAll(tblTitle, spacer2, newChallanBtn);

        recentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        recentTable.setPrefHeight(300);

        TableColumn<Challan, String> colNo = new TableColumn<>("Challan No");
        colNo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getChallanNumber()));

        TableColumn<Challan, String> colDate = new TableColumn<>("Date & Time");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(DateUtil.formatDateTime(d.getValue().getViolationDate())));

        TableColumn<Challan, String> colVeh = new TableColumn<>("Vehicle Number");
        colVeh.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getVehicleNumber()));

        TableColumn<Challan, String> colOffense = new TableColumn<>("Violation Category");
        colOffense.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getViolationTitle()));

        TableColumn<Challan, String> colFine = new TableColumn<>("Fine");
        colFine.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedFine()));

        TableColumn<Challan, String> colStatus = new TableColumn<>("Payment Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPaymentStatus().getLabel()));

        TableColumn<Challan, Void> colAction = new TableColumn<>("Receipt");
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Print Slip");
            {
                btn.getStyleClass().addAll("btn-secondary", "btn-small");
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

        recentTable.getColumns().addAll(colNo, colDate, colVeh, colOffense, colFine, colStatus, colAction);
        recentBox.getChildren().addAll(tblHeader, recentTable);

        root.getChildren().addAll(header, kpiRow, attBar, recentBox);

        // Async Loader
        Runnable loadData = () -> {
            if (officer == null) return;
            try {
                Optional<DutyAssignment> activeDutyOpt = dutyService.getTodaysActiveDutyForOfficer(officer.getOfficerId());
                Optional<Attendance> todayAttOpt = dutyService.getTodayAttendanceForOfficer(officer.getOfficerId());
                List<Challan> myChallans = challanService.getChallansByOfficer(officer.getOfficerId());

                Platform.runLater(() -> {
                    // Update Shift Card
                    if (activeDutyOpt.isPresent()) {
                        ((Label) cardShift.getChildren().get(1)).setText(activeDutyOpt.get().getShift().getTitle());
                    } else {
                        ((Label) cardShift.getChildren().get(1)).setText("General Shift");
                    }

                    // Update Attendance Card & Buttons
                    if (todayAttOpt.isPresent()) {
                        Attendance att = todayAttOpt.get();
                        String st = att.getStatus().getLabel();
                        if (att.getCheckInTime() != null) st += " (" + DateUtil.formatTime(att.getCheckInTime()) + ")";
                        ((Label) cardAttendance.getChildren().get(1)).setText(st);

                        boolean checkedOut = att.getCheckOutTime() != null;
                        checkInBtn.setDisable(true);
                        checkOutBtn.setDisable(checkedOut);
                        statusNotice.setText(checkedOut ? "Shift completed for today (Checked out)" : "Active on duty since " + DateUtil.formatTime(att.getCheckInTime()));
                    } else {
                        ((Label) cardAttendance.getChildren().get(1)).setText("Not Checked In");
                        checkInBtn.setDisable(false);
                        checkOutBtn.setDisable(true);
                        statusNotice.setText("Please check in to register today's attendance");
                    }

                    // Update Violations Card
                    ((Label) cardViolations.getChildren().get(1)).setText(String.valueOf(myChallans.size()));

                    // Update Table
                    recentTable.setItems(FXCollections.observableArrayList(myChallans.stream().limit(8).toList()));
                });
            } catch (Exception e) {
                Platform.runLater(() -> UIComponents.showErrorAlert("Error", "Could not load officer data: " + e.getMessage()));
            }
        };

        checkInBtn.setOnAction(e -> {
            if (officer == null) return;
            try {
                dutyService.markCheckIn(officer.getOfficerId(), null, "Officer self check-in via terminal");
                UIComponents.showInfoAlert("Checked In", "Duty check-in registered successfully!");
                ThreadPoolManager.getInstance().execute(loadData);
            } catch (Exception ex) {
                UIComponents.showWarningAlert("Attendance", ex.getMessage());
            }
        });

        checkOutBtn.setOnAction(e -> {
            if (officer == null) return;
            try {
                Optional<Attendance> todayAttOpt = dutyService.getTodayAttendanceForOfficer(officer.getOfficerId());
                if (todayAttOpt.isPresent()) {
                    dutyService.markCheckOut(todayAttOpt.get().getId(), todayAttOpt.get().getDutyId());
                    UIComponents.showInfoAlert("Checked Out", "Duty check-out recorded. Have a safe day!");
                    ThreadPoolManager.getInstance().execute(loadData);
                }
            } catch (Exception ex) {
                UIComponents.showWarningAlert("Attendance", ex.getMessage());
            }
        });

        ThreadPoolManager.getInstance().execute(loadData);
    }
}
