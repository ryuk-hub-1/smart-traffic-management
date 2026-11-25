package com.smarttraffic.controller.citizen;

import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.model.Challan;
import com.smarttraffic.model.Citizen;
import com.smarttraffic.model.enums.ChallanStatus;
import com.smarttraffic.service.AuthService;
import com.smarttraffic.service.ChallanService;
import com.smarttraffic.util.DateUtil;
import com.smarttraffic.util.ThreadPoolManager;
import com.smarttraffic.view.ChallanReceiptDialog;
import com.smarttraffic.view.UIComponents;
import com.smarttraffic.view.ViewNavigator;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
 * Controller for Citizen e-Challan Inquiry and Online Fine Settlement.
 * Features Visual Citation Cards with License Plates, Payment Telemetry, and Tabular Toggle.
 */
public class CitizenChallansController {
    private final VBox root = new VBox(14);
    private final ChallanService challanService = ChallanService.getInstance();

    private final ObservableList<Challan> masterList = FXCollections.observableArrayList();
    private FilteredList<Challan> filteredList;

    private final FlowPane cardsContainer = new FlowPane();
    private final TableView<Challan> table = new TableView<>();
    private final StackPane viewContainer = new StackPane();

    private final TextField vehicleSearchField = UIComponents.createSearchField("Search vehicle plate or challan ID...");
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final HBox statsBanner = new HBox(12);

    public CitizenChallansController() {
        buildUI();
        loadData();
    }

    public Node getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(10, 10, 30, 10));

        Citizen citizen = (AuthService.getInstance().getCurrentUser() instanceof Citizen c) ? c : null;
        String myVeh = citizen != null ? citizen.getRegisteredVehicleNumber() : "";

        VBox header = UIComponents.createViewHeader(
                "My Vehicle Citations & Fine Settlement",
                "Verify traffic violation evidence, review statutory penalties, and make instant electronic payments"
        );

        // Stats Banner
        statsBanner.setAlignment(Pos.CENTER_LEFT);
        updateStatsBanner();

        // Control Bar
        HBox controlBar = new HBox(12);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        if (myVeh != null && !myVeh.isEmpty()) {
            vehicleSearchField.setText(myVeh);
        }

        statusFilter.getItems().addAll("All Statuses", "PENDING", "PAID", "OVERDUE");
        statusFilter.setValue("All Statuses");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox viewToggle = UIComponents.createViewToggle("Citation Slips", "Tabular View", isCardView -> {
            if (isCardView) {
                showCardsView();
            } else {
                showTableView();
            }
        });

        Button searchBtn = new Button("🔍 Search");
        searchBtn.getStyleClass().add("btn-primary");
        searchBtn.setOnAction(e -> loadData());

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> loadData());

        controlBar.getChildren().addAll(vehicleSearchField, searchBtn, statusFilter, spacer, viewToggle, refreshBtn);

        // Cards View
        cardsContainer.setHgap(16);
        cardsContainer.setVgap(16);
        cardsContainer.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(cardsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        scrollPane.setPrefHeight(520);

        // Table View
        buildTableView();

        viewContainer.getChildren().addAll(scrollPane, table);
        showCardsView();

        // Filter binding
        filteredList = new FilteredList<>(masterList, p -> true);

        Runnable applyFilter = () -> {
            String q = vehicleSearchField.getText() != null ? vehicleSearchField.getText().trim().toLowerCase() : "";
            String stat = statusFilter.getValue();

            filteredList.setPredicate(c -> {
                boolean matchesStat = stat == null || stat.equals("All Statuses") || c.getPaymentStatus().name().equalsIgnoreCase(stat);
                boolean matchesText = q.isEmpty() ||
                        c.getVehicleNumber().toLowerCase().contains(q) ||
                        c.getChallanNumber().toLowerCase().contains(q) ||
                        c.getViolationTitle().toLowerCase().contains(q);
                return matchesStat && matchesText;
            });

            renderCards();
        };

        vehicleSearchField.textProperty().addListener((obs, o, n) -> applyFilter.run());
        statusFilter.valueProperty().addListener((obs, o, n) -> applyFilter.run());

        table.setItems(filteredList);

        root.getChildren().addAll(header, statsBanner, controlBar, viewContainer);
    }

    private void showCardsView() {
        viewContainer.getChildren().get(0).setVisible(true);
        viewContainer.getChildren().get(0).setManaged(true);
        viewContainer.getChildren().get(1).setVisible(false);
        viewContainer.getChildren().get(1).setManaged(false);
    }

    private void showTableView() {
        viewContainer.getChildren().get(0).setVisible(false);
        viewContainer.getChildren().get(0).setManaged(false);
        viewContainer.getChildren().get(1).setVisible(true);
        viewContainer.getChildren().get(1).setManaged(true);
    }

    private void buildTableView() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(520);

        TableColumn<Challan, String> colNo = new TableColumn<>("Challan No");
        colNo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getChallanNumber()));
        colNo.setPrefWidth(120);

        TableColumn<Challan, String> colDate = new TableColumn<>("Violation Date");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(DateUtil.formatDateTime(d.getValue().getViolationDate())));

        TableColumn<Challan, String> colVeh = new TableColumn<>("Vehicle Number");
        colVeh.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getVehicleNumber()));

        TableColumn<Challan, String> colOffense = new TableColumn<>("Infraction Offense");
        colOffense.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getViolationTitle()));

        TableColumn<Challan, String> colJunction = new TableColumn<>("Enforcement Junction");
        colJunction.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getJunctionName()));

        TableColumn<Challan, String> colFine = new TableColumn<>("Fine (₹)");
        colFine.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedFine()));

        TableColumn<Challan, String> colStatus = new TableColumn<>("Payment Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPaymentStatus().getLabel()));

        TableColumn<Challan, Void> colActions = new TableColumn<>("Action");
        colActions.setPrefWidth(200);
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button payBtn = new Button("💳 Pay Online");
            private final Button receiptBtn = new Button("📄 Slip");
            private final HBox pane = new HBox(6, payBtn, receiptBtn);
            {
                payBtn.getStyleClass().addAll("btn-success", "btn-small");
                receiptBtn.getStyleClass().addAll("btn-secondary", "btn-small");

                payBtn.setOnAction(e -> {
                    Challan c = getTableView().getItems().get(getIndex());
                    showOnlinePaymentDialog(c);
                });

                receiptBtn.setOnAction(e -> {
                    Challan c = getTableView().getItems().get(getIndex());
                    ChallanReceiptDialog.show(c, ViewNavigator.getPrimaryStage());
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

        table.getColumns().addAll(colNo, colDate, colVeh, colOffense, colJunction, colFine, colStatus, colActions);
    }

    private void renderCards() {
        cardsContainer.getChildren().clear();

        if (filteredList.isEmpty()) {
            VBox emptyBox = new VBox(10);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40));
            Label emptyLbl = new Label("No traffic violation citations found for this vehicle search");
            emptyLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");
            emptyBox.getChildren().add(emptyLbl);
            cardsContainer.getChildren().add(emptyBox);
            return;
        }

        for (Challan c : filteredList) {
            VBox card = new VBox(10);
            card.getStyleClass().add("challan-card");
            card.setPrefWidth(350);

            // Top Row: License plate graphic + status badge
            HBox topRow = new HBox(10);
            topRow.setAlignment(Pos.CENTER_LEFT);

            Label plate = UIComponents.createLicensePlate(c.getVehicleNumber());
            Region topSpacer = new Region();
            HBox.setHgrow(topSpacer, Priority.ALWAYS);

            Label statusBadge = new Label(c.getPaymentStatus().getLabel());
            switch (c.getPaymentStatus()) {
                case PAID -> statusBadge.getStyleClass().addAll("badge", "badge-success");
                case PENDING -> statusBadge.getStyleClass().addAll("badge", "badge-warning");
                case OVERDUE -> statusBadge.getStyleClass().addAll("badge", "badge-danger");
            }

            topRow.getChildren().addAll(plate, topSpacer, statusBadge);

            // Challan ID & Date
            HBox idDateRow = new HBox(8);
            idDateRow.setAlignment(Pos.CENTER_LEFT);

            Label idLbl = new Label(c.getChallanNumber());
            idLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #2563eb;");

            Region idSpacer = new Region();
            HBox.setHgrow(idSpacer, Priority.ALWAYS);

            Label dateLbl = new Label("📅 " + DateUtil.formatDateTime(c.getViolationDate()));
            dateLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

            idDateRow.getChildren().addAll(idLbl, idSpacer, dateLbl);

            // Violation details
            VBox detailsBox = new VBox(4);
            Label offLbl = new Label("⚠️ " + c.getViolationTitle());
            offLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0f172a;");
            offLbl.setWrapText(true);

            Label locLbl = new Label("📍 Location: " + (c.getJunctionName() != null ? c.getJunctionName() : "Patrol Area"));
            locLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

            detailsBox.getChildren().addAll(offLbl, locLbl);

            // Fine Callout Box
            HBox fineBox = new HBox(8);
            fineBox.setAlignment(Pos.CENTER_LEFT);
            fineBox.setStyle("-fx-background-color: #f8fafc; -fx-padding: 8px 12px; -fx-background-radius: 6px; -fx-border-color: #e2e8f0; -fx-border-radius: 6px;");

            Label fineTitle = new Label("Fine Amount:");
            fineTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

            Label fineVal = new Label(c.getFormattedFine());
            fineVal.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #b91c1c;");

            Region fineSpacer = new Region();
            HBox.setHgrow(fineSpacer, Priority.ALWAYS);

            fineBox.getChildren().addAll(fineTitle, fineVal, fineSpacer);

            if (c.getPaymentStatus() == ChallanStatus.PAID) {
                Label paidTag = new Label("PAID IN FULL");
                paidTag.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #16a34a;");
                fineBox.getChildren().add(paidTag);
            }

            // Card Action Buttons
            HBox actionRow = new HBox(8);
            actionRow.setAlignment(Pos.CENTER_RIGHT);

            Button receiptBtn = new Button("📄 View Slip");
            receiptBtn.getStyleClass().addAll("btn-secondary", "btn-small");
            receiptBtn.setOnAction(e -> ChallanReceiptDialog.show(c, ViewNavigator.getPrimaryStage()));

            Button payBtn = new Button("💳 Pay Online");
            payBtn.getStyleClass().addAll("btn-success", "btn-small");
            payBtn.setDisable(c.getPaymentStatus() == ChallanStatus.PAID);
            payBtn.setOnAction(e -> showOnlinePaymentDialog(c));

            actionRow.getChildren().addAll(receiptBtn, payBtn);

            card.getChildren().addAll(topRow, idDateRow, detailsBox, fineBox, actionRow);
            cardsContainer.getChildren().add(card);
        }
    }

    private void updateStatsBanner() {
        statsBanner.getChildren().clear();
        int total = masterList.size();
        long pending = masterList.stream().filter(c -> c.getPaymentStatus() == ChallanStatus.PENDING).count();
        long paid = masterList.stream().filter(c -> c.getPaymentStatus() == ChallanStatus.PAID).count();
        double amountDue = masterList.stream()
                .filter(c -> c.getPaymentStatus() != ChallanStatus.PAID)
                .mapToDouble(Challan::getFineAmount)
                .sum();

        statsBanner.getChildren().addAll(
                UIComponents.createStatChip("📑", "TOTAL CITATIONS", String.valueOf(total), "#ffffff"),
                UIComponents.createStatChip("⏳", "PENDING FINES", String.valueOf(pending), "#fffbeb"),
                UIComponents.createStatChip("💰", "AMOUNT DUE", String.format("₹ %,.0f", amountDue), "#fef2f2"),
                UIComponents.createStatChip("✅", "SETTLED FINES", String.valueOf(paid), "#f0fdf4")
        );
    }

    private void loadData() {
        String veh = vehicleSearchField.getText();
        ThreadPoolManager.getInstance().execute(() -> {
            try {
                List<Challan> list;
                if (veh != null && !veh.trim().isEmpty()) {
                    list = challanService.getChallansForVehicle(veh.trim());
                } else {
                    list = challanService.getAllChallans();
                }
                Platform.runLater(() -> {
                    masterList.setAll(list);
                    updateStatsBanner();
                    renderCards();
                });
            } catch (DatabaseException e) {
                Platform.runLater(() -> UIComponents.showErrorAlert("Database Error", "Failed to load challans: " + e.getMessage()));
            }
        });
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
                loadData();

                // Refresh challan and show receipt
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
