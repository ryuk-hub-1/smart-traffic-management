package com.smarttraffic.controller.admin;

import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.model.Challan;
import com.smarttraffic.model.enums.ChallanStatus;
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
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Controller for Admin Challans & Violations: Visual Citation Slip Grid & Fine Recovery.
 */
public class AdminChallansController {
    private final VBox root = new VBox(15);
    private final ChallanService challanService = ChallanService.getInstance();

    private final ObservableList<Challan> masterList = FXCollections.observableArrayList();
    private final FilteredList<Challan> filteredList = new FilteredList<>(masterList, p -> true);

    private final FlowPane cardsPane = new FlowPane();
    private final TableView<Challan> table = new TableView<>();
    private final StackPane viewContainer = new StackPane();

    private final TextField searchField = UIComponents.createSearchField("Search vehicle plate, challan no, offender...");
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final HBox statsBanner = new HBox(12);

    public AdminChallansController() {
        buildUI();
        loadData();
    }

    public Node getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(10, 10, 30, 10));

        VBox header = UIComponents.createViewHeader(
                "Digital e-Challan Registry & Penalty Recovery",
                "Audit photographic violation notices, review vehicle infraction histories, and process penalty receipts"
        );

        statsBanner.setAlignment(Pos.CENTER_LEFT);
        updateStatsBanner();

        HBox controlBar = new HBox(12);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        statusFilter.getItems().addAll("All Statuses", "PENDING", "PAID", "OVERDUE");
        statusFilter.setValue("All Statuses");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox viewToggle = UIComponents.createViewToggle("Citation Slips", "Tabular View", isCardView -> {
            viewContainer.getChildren().clear();
            if (isCardView) {
                viewContainer.getChildren().add(new ScrollPane(cardsPane));
            } else {
                viewContainer.getChildren().add(new VBox(table));
            }
        });

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> loadData());

        controlBar.getChildren().addAll(searchField, statusFilter, spacer, viewToggle, refreshBtn);

        // --- Cards Grid Layout ---
        cardsPane.setHgap(16);
        cardsPane.setVgap(16);
        cardsPane.setPadding(new Insets(8));
        cardsPane.setStyle("-fx-background-color: transparent;");

        ScrollPane scrollCardPane = new ScrollPane(cardsPane);
        scrollCardPane.setFitToWidth(true);
        scrollCardPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        // --- Table View Layout ---
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(520);

        TableColumn<Challan, String> colNo = new TableColumn<>("Challan No");
        colNo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getChallanNumber()));
        colNo.setPrefWidth(120);

        TableColumn<Challan, String> colDate = new TableColumn<>("Date & Time");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(DateUtil.formatDateTime(d.getValue().getViolationDate())));

        TableColumn<Challan, String> colVeh = new TableColumn<>("Vehicle Number");
        colVeh.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getVehicleNumber()));

        TableColumn<Challan, String> colType = new TableColumn<>("Vehicle Type");
        colType.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getVehicleType() != null ? d.getValue().getVehicleType().getLabel() : "N/A"
        ));

        TableColumn<Challan, String> colOffense = new TableColumn<>("Violation Category");
        colOffense.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getViolationTitle()));

        TableColumn<Challan, String> colFine = new TableColumn<>("Fine");
        colFine.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedFine()));

        TableColumn<Challan, String> colOfficer = new TableColumn<>("Issuing Officer");
        colOfficer.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getOfficerName() != null ? d.getValue().getOfficerName() : "Patrol Squad"
        ));

        TableColumn<Challan, String> colJunction = new TableColumn<>("Junction Location");
        colJunction.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getJunctionName()));

        TableColumn<Challan, String> colStatus = new TableColumn<>("Payment Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPaymentStatus().getLabel()));

        TableColumn<Challan, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(180);
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button receiptBtn = new Button("📄 Slip");
            private final Button payBtn = new Button("💳 Settle");
            private final Button delBtn = new Button("🗑️ Void");
            private final HBox pane = new HBox(5, receiptBtn, payBtn, delBtn);
            {
                receiptBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                payBtn.getStyleClass().addAll("btn-success", "btn-small");
                delBtn.getStyleClass().addAll("btn-danger", "btn-small");

                receiptBtn.setOnAction(e -> {
                    Challan c = getTableView().getItems().get(getIndex());
                    ChallanReceiptDialog.show(c, ViewNavigator.getPrimaryStage());
                });

                payBtn.setOnAction(e -> {
                    Challan c = getTableView().getItems().get(getIndex());
                    handleSettleChallan(c);
                });

                delBtn.setOnAction(e -> {
                    Challan c = getTableView().getItems().get(getIndex());
                    handleDeleteChallan(c);
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

        table.getColumns().addAll(colNo, colDate, colVeh, colType, colOffense, colFine, colOfficer, colJunction, colStatus, colActions);
        table.setItems(filteredList);

        Runnable applyFilter = () -> {
            String query = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
            String stat = statusFilter.getValue();

            filteredList.setPredicate(c -> {
                boolean matchesStat = stat == null || stat.equals("All Statuses") || c.getPaymentStatus().name().equalsIgnoreCase(stat);
                boolean matchesText = query.isEmpty() ||
                        c.getChallanNumber().toLowerCase().contains(query) ||
                        c.getVehicleNumber().toLowerCase().contains(query) ||
                        (c.getViolatorName() != null && c.getViolatorName().toLowerCase().contains(query)) ||
                        c.getViolationTitle().toLowerCase().contains(query) ||
                        c.getJunctionName().toLowerCase().contains(query);
                return matchesStat && matchesText;
            });
            renderCardsGrid();
        };

        searchField.textProperty().addListener((obs, o, n) -> applyFilter.run());
        statusFilter.valueProperty().addListener((obs, o, n) -> applyFilter.run());

        viewContainer.getChildren().add(scrollCardPane);

        root.getChildren().addAll(header, statsBanner, controlBar, viewContainer);
    }

    private void renderCardsGrid() {
        cardsPane.getChildren().clear();

        if (filteredList.isEmpty()) {
            VBox emptyBox = new VBox(10);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40));
            Label emptyLbl = new Label("No e-Challans found.");
            emptyLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");
            emptyBox.getChildren().add(emptyLbl);
            cardsPane.getChildren().add(emptyBox);
            return;
        }

        for (Challan c : filteredList) {
            cardsPane.getChildren().add(createChallanCard(c));
        }
    }

    private VBox createChallanCard(Challan c) {
        VBox card = new VBox(10);
        card.getStyleClass().add("challan-card");

        boolean isPaid = c.getPaymentStatus() == ChallanStatus.PAID;
        boolean isOverdue = c.getPaymentStatus() == ChallanStatus.OVERDUE;
        String borderAccent = isPaid ? "#10b981" : isOverdue ? "#ef4444" : "#f59e0b";
        card.setStyle(card.getStyle() + "; -fx-border-left-color: " + borderAccent + "; -fx-border-left-width: 4px;");

        // Top Row: Number Plate + Status Badge
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label plate = UIComponents.createLicensePlate(c.getVehicleNumber());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String badgeStyle = isPaid ? "badge-success" : isOverdue ? "badge-danger" : "badge-warning";
        Label statusBadge = UIComponents.createBadge(c.getPaymentStatus().getLabel(), badgeStyle);

        topRow.getChildren().addAll(plate, spacer, statusBadge);

        // Challan Number & Date
        HBox numDateRow = new HBox(8);
        numDateRow.setAlignment(Pos.CENTER_LEFT);
        Label numLbl = new Label(c.getChallanNumber());
        numLbl.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-weight: 800; -fx-font-size: 11px; -fx-text-fill: #2563eb;");
        Label dot = new Label("•");
        dot.setStyle("-fx-text-fill: #94a3b8;");
        Label dateLbl = new Label(DateUtil.formatDateTime(c.getViolationDate()));
        dateLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        numDateRow.getChildren().addAll(numLbl, dot, dateLbl);

        // Offense Title & Fine Amount
        VBox offenseBox = new VBox(2);
        Label offTitle = new Label(c.getViolationTitle());
        offTitle.setStyle("-fx-font-weight: 900; -fx-font-size: 14px; -fx-text-fill: #0f172a;");
        offTitle.setWrapText(true);

        Label fineLbl = new Label("Statutory Fine: " + c.getFormattedFine());
        fineLbl.setStyle("-fx-font-weight: 900; -fx-font-size: 13px; -fx-text-fill: " + (isPaid ? "#059669" : "#dc2626") + ";");
        offenseBox.getChildren().addAll(offTitle, fineLbl);

        // Junction & Officer Details
        VBox locationBox = new VBox(2);
        Label juncLbl = new Label("📍 " + c.getJunctionName());
        juncLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #334155;");

        String offName = c.getOfficerName() != null ? c.getOfficerName() : "Patrol Squad";
        Label officerLbl = new Label("👮 " + offName + " (Badge: " + (c.getOfficerBadge() != null ? c.getOfficerBadge() : "N/A") + ")");
        officerLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        locationBox.getChildren().addAll(juncLbl, officerLbl);

        // Evidence Indicator if present
        if (c.getEvidenceFilePath() != null && !c.getEvidenceFilePath().isEmpty()) {
            Label evLbl = new Label("📷 Photographic Evidence Captured");
            evLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #0891b2; -fx-font-weight: 700;");
            locationBox.getChildren().add(evLbl);
        }

        // Action Buttons Row
        HBox actRow = new HBox(8);
        actRow.setAlignment(Pos.CENTER_RIGHT);
        actRow.setPadding(new Insets(4, 0, 0, 0));

        Button printBtn = new Button("📄 Official Slip");
        printBtn.getStyleClass().addAll("btn-secondary", "btn-small");
        printBtn.setOnAction(e -> ChallanReceiptDialog.show(c, ViewNavigator.getPrimaryStage()));

        Button payBtn = new Button("💳 Settle Fine");
        payBtn.getStyleClass().addAll("btn-success", "btn-small");
        payBtn.setDisable(isPaid);
        payBtn.setOnAction(e -> handleSettleChallan(c));

        Button delBtn = new Button("🗑️ Void");
        delBtn.getStyleClass().addAll("btn-danger", "btn-small");
        delBtn.setOnAction(e -> handleDeleteChallan(c));

        actRow.getChildren().addAll(printBtn, payBtn, delBtn);

        card.getChildren().addAll(topRow, numDateRow, offenseBox, locationBox, actRow);
        return card;
    }

    private void updateStatsBanner() {
        statsBanner.getChildren().clear();
        int total = masterList.size();
        long paid = masterList.stream().filter(c -> c.getPaymentStatus() == ChallanStatus.PAID).count();
        long pending = masterList.stream().filter(c -> c.getPaymentStatus() == ChallanStatus.PENDING).count();
        long overdue = masterList.stream().filter(c -> c.getPaymentStatus() == ChallanStatus.OVERDUE).count();

        double collectedFine = masterList.stream().filter(c -> c.getPaymentStatus() == ChallanStatus.PAID).mapToDouble(Challan::getFineAmount).sum();
        double pendingFine = masterList.stream().filter(c -> c.getPaymentStatus() != ChallanStatus.PAID).mapToDouble(Challan::getFineAmount).sum();

        statsBanner.getChildren().addAll(
                UIComponents.createStatChip("📋", "TOTAL CITATIONS", String.valueOf(total), "#ffffff"),
                UIComponents.createStatChip("💰", "COLLECTED REVENUE", String.format("₹ %,.0f", collectedFine), "#f0fdf4"),
                UIComponents.createStatChip("⏳", "PENDING FINES", String.format("₹ %,.0f (%d)", pendingFine, pending), "#fffbeb"),
                UIComponents.createStatChip("⚠️", "OVERDUE CITATIONS", String.valueOf(overdue), "#fef2f2")
        );
    }

    private void loadData() {
        ThreadPoolManager.getInstance().execute(() -> {
            try {
                List<Challan> list = challanService.getAllChallans();
                Platform.runLater(() -> {
                    masterList.setAll(list);
                    updateStatsBanner();
                    renderCardsGrid();
                });
            } catch (DatabaseException e) {
                Platform.runLater(() -> UIComponents.showErrorAlert("Database Error", "Failed to load challans: " + e.getMessage()));
            }
        });
    }

    private void handleSettleChallan(Challan c) {
        TextInputDialog dialog = new TextInputDialog("COUNTER-CASH-" + System.currentTimeMillis() % 10000);
        dialog.setTitle("Counter Payment Settle");
        dialog.setHeaderText("Settle Challan " + c.getChallanNumber() + " (" + c.getFormattedFine() + ")");
        dialog.setContentText("Enter Bank/Counter Reference Number:");
        dialog.showAndWait().ifPresent(ref -> {
            try {
                challanService.payChallan(c.getId(), ref);
                UIComponents.showInfoAlert("Payment Recorded", "Payment of " + c.getFormattedFine() + " settled successfully!");
                loadData();
            } catch (Exception ex) {
                UIComponents.showErrorAlert("Payment Error", ex.getMessage());
            }
        });
    }

    private void handleDeleteChallan(Challan c) {
        if (UIComponents.showConfirmDialog("Confirm Void Challan",
                "Are you sure you want to dismiss and delete Challan #" + c.getChallanNumber() + " (" + c.getVehicleNumber() + ")?")) {
            try {
                challanService.deleteChallan(c.getId());
                UIComponents.showInfoAlert("Dismissed", "Challan #" + c.getChallanNumber() + " removed from records.");
                loadData();
            } catch (Exception ex) {
                UIComponents.showErrorAlert("Error", ex.getMessage());
            }
        }
    }
}
