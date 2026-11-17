package com.smarttraffic.controller.citizen;

import com.smarttraffic.model.Challan;
import com.smarttraffic.model.Citizen;
import com.smarttraffic.model.Complaint;
import com.smarttraffic.model.User;
import com.smarttraffic.model.enums.ChallanStatus;
import com.smarttraffic.service.AuthService;
import com.smarttraffic.service.ChallanService;
import com.smarttraffic.service.ComplaintService;
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
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

/**
 * Modernized Controller for Citizen Overview Dashboard.
 * Features Interactive Navigation KPI Cards, Live Dues Telemetry, and Instant Online Fine Settlement.
 */
public class CitizenDashboardController {
    private final VBox root = new VBox(20);
    private final ChallanService challanService = ChallanService.getInstance();
    private final ComplaintService complaintService = ComplaintService.getInstance();

    private final TableView<Challan> recentTable = new TableView<>();
    private final VBox cardPending = UIComponents.createKpiCard("Pending Challans", "...", "Click to view", "#ef4444");
    private final VBox cardDue = UIComponents.createKpiCard("Total Dues", "...", "Click to settle", "#f59e0b");
    private final VBox cardSettled = UIComponents.createKpiCard("Settled Citations", "...", "Paid in full", "#10b981");
    private final VBox cardComplaints = UIComponents.createKpiCard("Filed Complaints", "...", "Click for status", "#2563eb");

    private Runnable reloadAction;

    public CitizenDashboardController() {
        buildUI();
    }

    public Node getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(10, 10, 30, 10));

        User user = AuthService.getInstance().getCurrentUser();
        Citizen citizen = (user instanceof Citizen c) ? c : null;

        String citName = citizen != null ? citizen.getFullName() : "Citizen";
        String vehNo = citizen != null && citizen.getRegisteredVehicleNumber() != null && !citizen.getRegisteredVehicleNumber().isEmpty()
                ? citizen.getRegisteredVehicleNumber()
                : "No Primary Vehicle Registered";

        VBox header = UIComponents.createViewHeader(
                "Citizen Traffic Services Portal: " + citName,
                "Registered Primary Vehicle: " + vehNo + " • Driving License: " + (citizen != null && citizen.getDrivingLicenseNumber() != null ? citizen.getDrivingLicenseNumber() : "N/A")
        );

        // 1. KPI Cards Row (Interactive)
        HBox kpiRow = new HBox(15);
        kpiRow.setAlignment(Pos.CENTER);

        cardPending.setStyle(cardPending.getStyle() + "; -fx-cursor: hand;");
        cardPending.setOnMouseClicked(e -> ViewNavigator.loadMainContent(new CitizenChallansController().getView(), "Challans"));

        cardDue.setStyle(cardDue.getStyle() + "; -fx-cursor: hand;");
        cardDue.setOnMouseClicked(e -> ViewNavigator.loadMainContent(new CitizenChallansController().getView(), "Challans"));

        cardSettled.setStyle(cardSettled.getStyle() + "; -fx-cursor: hand;");
        cardSettled.setOnMouseClicked(e -> ViewNavigator.loadMainContent(new CitizenChallansController().getView(), "Challans"));

        cardComplaints.setStyle(cardComplaints.getStyle() + "; -fx-cursor: hand;");
        cardComplaints.setOnMouseClicked(e -> ViewNavigator.loadMainContent(new CitizenComplaintsHistoryController().getView(), "History"));

        kpiRow.getChildren().addAll(cardPending, cardDue, cardSettled, cardComplaints);

        // Quick Actions Banner
        HBox actionBanner = new HBox(15);
        actionBanner.getStyleClass().add("content-card");
        actionBanner.setAlignment(Pos.CENTER_LEFT);

        VBox actionTexts = new VBox(3);
        Label bannerTitle = new Label("Quick Citizen Actions");
        bannerTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");
        Label bannerSub = new Label("Settle outstanding citations securely or report urban road violations");
        bannerSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        actionTexts.getChildren().addAll(bannerTitle, bannerSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button payChallansBtn = new Button("💳 View & Settle Challans");
        payChallansBtn.getStyleClass().add("btn-primary");
        payChallansBtn.setOnAction(e -> ViewNavigator.loadMainContent(new CitizenChallansController().getView(), "Challans"));

        Button reportIssueBtn = new Button("📝 Report Traffic Issue");
        reportIssueBtn.getStyleClass().add("btn-secondary");
        reportIssueBtn.setOnAction(e -> ViewNavigator.loadMainContent(new FileComplaintController().getView(), "File"));

        actionBanner.getChildren().addAll(actionTexts, spacer, payChallansBtn, reportIssueBtn);

        // Recent Violations Table for this citizen's vehicle
        VBox recentBox = new VBox(10);
        recentBox.getStyleClass().add("content-card");

        HBox tblHeader = new HBox(10);
        tblHeader.setAlignment(Pos.CENTER_LEFT);
        Label tblTitle = new Label("Active Traffic Citations for Your Vehicle (" + vehNo + ")");
        tblTitle.getStyleClass().add("section-title");

        Region tblSpacer = new Region();
        HBox.setHgrow(tblSpacer, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> {
            if (reloadAction != null) ThreadPoolManager.getInstance().execute(reloadAction);
        });

        tblHeader.getChildren().addAll(tblTitle, tblSpacer, refreshBtn);

        recentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        recentTable.setPrefHeight(280);

        TableColumn<Challan, String> colNo = new TableColumn<>("Challan No");
        colNo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getChallanNumber()));
        colNo.setPrefWidth(120);

        TableColumn<Challan, String> colDate = new TableColumn<>("Date & Time");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(DateUtil.formatDateTime(d.getValue().getViolationDate())));

        TableColumn<Challan, String> colOffense = new TableColumn<>("Violation Category");
        colOffense.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getViolationTitle()));

        TableColumn<Challan, String> colJunction = new TableColumn<>("Junction Point");
        colJunction.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getJunctionName()));

        TableColumn<Challan, String> colFine = new TableColumn<>("Fine");
        colFine.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedFine()));

        TableColumn<Challan, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPaymentStatus().getLabel()));

        TableColumn<Challan, Void> colAction = new TableColumn<>("Actions");
        colAction.setPrefWidth(200);
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button receiptBtn = new Button("📄 Slip");
            private final Button payBtn = new Button("💳 Pay Online");
            private final HBox pane = new HBox(6, receiptBtn, payBtn);
            {
                receiptBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                payBtn.getStyleClass().addAll("btn-success", "btn-small");

                receiptBtn.setOnAction(e -> {
                    Challan c = getTableView().getItems().get(getIndex());
                    ChallanReceiptDialog.show(c, ViewNavigator.getPrimaryStage());
                });

                payBtn.setOnAction(e -> {
                    Challan c = getTableView().getItems().get(getIndex());
                    showOnlinePaymentDialog(c);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Challan c = getTableView().getItems().get(getIndex());
                    payBtn.setDisable(c.getPaymentStatus() == ChallanStatus.PAID);
                    setGraphic(pane);
                }
            }
        });

        recentTable.getColumns().addAll(colNo, colDate, colOffense, colJunction, colFine, colStatus, colAction);
        recentBox.getChildren().addAll(tblHeader, recentTable);

        root.getChildren().addAll(header, kpiRow, actionBanner, recentBox);

        // Async Data Loader
        reloadAction = () -> {
            if (citizen == null) return;
            try {
                String myVeh = citizen.getRegisteredVehicleNumber();
                List<Challan> myChallans = (myVeh != null && !myVeh.isEmpty())
                        ? challanService.getChallansForVehicle(myVeh)
                        : List.of();

                List<Complaint> myComplaints = complaintService.getComplaintsByCitizen(citizen.getId());

                long pendingCount = myChallans.stream().filter(c -> c.getPaymentStatus() == ChallanStatus.PENDING || c.getPaymentStatus() == ChallanStatus.OVERDUE).count();
                double totalDue = myChallans.stream().filter(c -> c.getPaymentStatus() == ChallanStatus.PENDING || c.getPaymentStatus() == ChallanStatus.OVERDUE).mapToDouble(Challan::getFineAmount).sum();
                long settledCount = myChallans.stream().filter(c -> c.getPaymentStatus() == ChallanStatus.PAID).count();

                Platform.runLater(() -> {
                    ((Label) cardPending.getChildren().get(1)).setText(String.valueOf(pendingCount));
                    ((Label) cardDue.getChildren().get(1)).setText(String.format("₹ %,.2f", totalDue));
                    ((Label) cardSettled.getChildren().get(1)).setText(String.valueOf(settledCount));
                    ((Label) cardComplaints.getChildren().get(1)).setText(String.valueOf(myComplaints.size()));

                    recentTable.setItems(FXCollections.observableArrayList(myChallans));
                });
            } catch (Exception ex) {
                Platform.runLater(() -> UIComponents.showErrorAlert("Error", "Could not load citizen data: " + ex.getMessage()));
            }
        };

        ThreadPoolManager.getInstance().execute(reloadAction);
    }

    private void showOnlinePaymentDialog(Challan c) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(ViewNavigator.getPrimaryStage());
        dialog.setTitle("Digital Payment Gateway - " + c.getChallanNumber());

        VBox form = new VBox(15);
        form.setPadding(new Insets(20));
        form.setPrefWidth(460);

        Label title = new Label("Secure Fine Settlement Portal");
        title.getStyleClass().add("section-title");

        VBox billSummary = new VBox(4);
        billSummary.setStyle("-fx-background-color: #f1f5f9; -fx-padding: 12px; -fx-background-radius: 8px;");
        billSummary.getChildren().addAll(
                new Label("Challan ID : " + c.getChallanNumber()),
                new Label("Vehicle No : " + c.getVehicleNumber()),
                new Label("Infraction : " + c.getViolationTitle()),
                new Label("Payable Fine: " + c.getFormattedFine())
        );
        ((Label) billSummary.getChildren().get(3)).setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #15803d;");

        ComboBox<String> methodCombo = new ComboBox<>();
        methodCombo.getItems().addAll("UPI / QR Payment", "Credit / Debit Card", "Net Banking", "Official Traffic Wallet");
        methodCombo.setValue("UPI / QR Payment");

        TextField refField = new TextField("UPI-TXN-" + (System.currentTimeMillis() % 1000000));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        grid.add(new Label("Payment Method"), 0, 0);
        grid.add(methodCombo, 1, 0);
        grid.add(new Label("Transaction / Reference ID"), 0, 1);
        grid.add(refField, 1, 1);

        HBox btnBar = new HBox(10);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> dialog.close());

        Button payNowBtn = new Button("Authorize Payment of " + c.getFormattedFine());
        payNowBtn.getStyleClass().add("btn-success");
        payNowBtn.setOnAction(e -> {
            try {
                challanService.payChallan(c.getId(), refField.getText());
                UIComponents.showInfoAlert("Payment Succeeded",
                        "Fine settled successfully! Transaction Reference: " + refField.getText() + "\nA receipt copy has been generated.");
                dialog.close();

                // Reload citizen dashboard numbers
                if (reloadAction != null) {
                    ThreadPoolManager.getInstance().execute(reloadAction);
                }

                // Show receipt
                challanService.getChallanById(c.getId()).ifPresent(updated -> {
                    ChallanReceiptDialog.show(updated, ViewNavigator.getPrimaryStage());
                });

            } catch (Exception ex) {
                UIComponents.showErrorAlert("Payment Failed", ex.getMessage());
            }
        });

        btnBar.getChildren().addAll(cancelBtn, payNowBtn);
        form.getChildren().addAll(title, billSummary, grid, btnBar);

        Scene scene = new Scene(form);
        ViewNavigator.applyStyles(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
