package com.smarttraffic.controller.officer;

import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.model.Challan;
import com.smarttraffic.model.Officer;
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
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Controller for Officer's Issued Challans History.
 * Features Visual Citation Cards with License Plates, Field Telemetry, and Tabular Toggle.
 */
public class OfficerChallansController {
    private final VBox root = new VBox(14);
    private final ChallanService challanService = ChallanService.getInstance();

    private final ObservableList<Challan> masterList = FXCollections.observableArrayList();
    private FilteredList<Challan> filteredList;

    private final FlowPane cardsContainer = new FlowPane();
    private final TableView<Challan> table = new TableView<>();
    private final StackPane viewContainer = new StackPane();

    private final TextField searchField = UIComponents.createSearchField("Search vehicle, challan no, offense...");
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final HBox statsBanner = new HBox(12);

    public OfficerChallansController() {
        buildUI();
        loadData();
    }

    public Node getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(10, 10, 30, 10));

        VBox header = UIComponents.createViewHeader(
                "My Issued Violations & Citations",
                "Review electronic citations logged during your patrol shifts and reprint official violation slips"
        );

        // Stats Banner
        statsBanner.setAlignment(Pos.CENTER_LEFT);
        updateStatsBanner();

        // Control Bar
        HBox controlBar = new HBox(12);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        statusFilter.getItems().addAll("All Statuses", "PENDING", "PAID", "OVERDUE");
        statusFilter.setValue("All Statuses");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox viewToggle = UIComponents.createViewToggle("Citation Cards", "Tabular Log", isCardView -> {
            if (isCardView) {
                showCardsView();
            } else {
                showTableView();
            }
        });

        Button newBtn = new Button("⚡ Log Violation");
        newBtn.getStyleClass().add("btn-primary");
        newBtn.setOnAction(e -> ViewNavigator.loadMainContent(new LogViolationController().getView()));

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> loadData());

        controlBar.getChildren().addAll(searchField, statusFilter, spacer, viewToggle, refreshBtn, newBtn);

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

        // Filter setup
        filteredList = new FilteredList<>(masterList, p -> true);

        Runnable applyFilter = () -> {
            String query = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
            String stat = statusFilter.getValue();

            filteredList.setPredicate(c -> {
                boolean matchesStat = stat == null || stat.equals("All Statuses") || c.getPaymentStatus().name().equalsIgnoreCase(stat);
                boolean matchesText = query.isEmpty() ||
                        c.getChallanNumber().toLowerCase().contains(query) ||
                        c.getVehicleNumber().toLowerCase().contains(query) ||
                        c.getViolationTitle().toLowerCase().contains(query);
                return matchesStat && matchesText;
            });

            renderCards();
        };

        searchField.textProperty().addListener((obs, o, n) -> applyFilter.run());
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

        TableColumn<Challan, String> colDate = new TableColumn<>("Date & Time");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(DateUtil.formatDateTime(d.getValue().getViolationDate())));

        TableColumn<Challan, String> colVeh = new TableColumn<>("Vehicle Number");
        colVeh.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getVehicleNumber()));

        TableColumn<Challan, String> colType = new TableColumn<>("Vehicle Type");
        colType.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getVehicleType() != null ? d.getValue().getVehicleType().getLabel() : "N/A"
        ));

        TableColumn<Challan, String> colOffense = new TableColumn<>("Offense");
        colOffense.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getViolationTitle()));

        TableColumn<Challan, String> colFine = new TableColumn<>("Fine");
        colFine.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedFine()));

        TableColumn<Challan, String> colJunction = new TableColumn<>("Junction");
        colJunction.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getJunctionName()));

        TableColumn<Challan, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPaymentStatus().getLabel()));

        TableColumn<Challan, Void> colAction = new TableColumn<>("Action");
        colAction.setPrefWidth(120);
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("📄 Slip");
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

        table.getColumns().addAll(colNo, colDate, colVeh, colType, colOffense, colFine, colJunction, colStatus, colAction);
    }

    private void renderCards() {
        cardsContainer.getChildren().clear();

        if (filteredList.isEmpty()) {
            VBox emptyBox = new VBox(10);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40));
            Label emptyLbl = new Label("No citations found matching your filter criteria");
            emptyLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");
            emptyBox.getChildren().add(emptyLbl);
            cardsContainer.getChildren().add(emptyBox);
            return;
        }

        for (Challan c : filteredList) {
            VBox card = new VBox(10);
            card.getStyleClass().add("challan-card");
            card.setPrefWidth(350);

            // Top Row: Plate + status badge
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

            // Details
            VBox detailsBox = new VBox(4);
            Label offLbl = new Label("⚠️ " + c.getViolationTitle());
            offLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0f172a;");
            offLbl.setWrapText(true);

            String vehCategory = c.getVehicleType() != null ? c.getVehicleType().getLabel() : "Motor Vehicle";
            Label vehLbl = new Label("🚗 Class: " + vehCategory);
            vehLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

            Label locLbl = new Label("📍 Location: " + (c.getJunctionName() != null ? c.getJunctionName() : "Patrol Junction"));
            locLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

            detailsBox.getChildren().addAll(offLbl, vehLbl, locLbl);

            // Fine Callout Box
            HBox fineBox = new HBox(8);
            fineBox.setAlignment(Pos.CENTER_LEFT);
            fineBox.setStyle("-fx-background-color: #f8fafc; -fx-padding: 8px 12px; -fx-background-radius: 6px; -fx-border-color: #e2e8f0; -fx-border-radius: 6px;");

            Label fineTitle = new Label("Fine:");
            fineTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

            Label fineVal = new Label(c.getFormattedFine());
            fineVal.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #b91c1c;");

            Region fineSpacer = new Region();
            HBox.setHgrow(fineSpacer, Priority.ALWAYS);

            Button receiptBtn = new Button("📄 Slip");
            receiptBtn.getStyleClass().addAll("btn-secondary", "btn-small");
            receiptBtn.setOnAction(e -> ChallanReceiptDialog.show(c, ViewNavigator.getPrimaryStage()));

            fineBox.getChildren().addAll(fineTitle, fineVal, fineSpacer, receiptBtn);

            card.getChildren().addAll(topRow, idDateRow, detailsBox, fineBox);
            cardsContainer.getChildren().add(card);
        }
    }

    private void updateStatsBanner() {
        statsBanner.getChildren().clear();
        int total = masterList.size();
        long pending = masterList.stream().filter(c -> c.getPaymentStatus() == ChallanStatus.PENDING).count();
        long paid = masterList.stream().filter(c -> c.getPaymentStatus() == ChallanStatus.PAID).count();
        double sum = masterList.stream().mapToDouble(Challan::getFineAmount).sum();

        statsBanner.getChildren().addAll(
                UIComponents.createStatChip("📑", "MY CITATIONS", String.valueOf(total), "#ffffff"),
                UIComponents.createStatChip("⏳", "PENDING FINES", String.valueOf(pending), "#fffbeb"),
                UIComponents.createStatChip("✅", "SETTLED FINES", String.valueOf(paid), "#f0fdf4"),
                UIComponents.createStatChip("💰", "TOTAL PENALTIES", String.format("₹ %,.0f", sum), "#f0f9ff")
        );
    }

    private void loadData() {
        Officer officer = (AuthService.getInstance().getCurrentUser() instanceof Officer o) ? o : null;
        if (officer == null) return;

        ThreadPoolManager.getInstance().execute(() -> {
            try {
                List<Challan> list = challanService.getChallansByOfficer(officer.getOfficerId());
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
}
