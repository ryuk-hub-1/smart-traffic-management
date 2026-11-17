package com.smarttraffic.controller.citizen;

import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.model.Citizen;
import com.smarttraffic.model.Complaint;
import com.smarttraffic.model.enums.ComplaintStatus;
import com.smarttraffic.service.AuthService;
import com.smarttraffic.service.ComplaintService;
import com.smarttraffic.util.DateUtil;
import com.smarttraffic.util.ThreadPoolManager;
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
 * Modernized Controller for Citizen Grievance History and Resolution Tracking.
 * Features Visual Tracking Cards with Investigation Notes, Status Badges, and Tabular Toggle.
 */
public class CitizenComplaintsHistoryController {
    private final VBox root = new VBox(14);
    private final ComplaintService complaintService = ComplaintService.getInstance();

    private final ObservableList<Complaint> masterList = FXCollections.observableArrayList();
    private FilteredList<Complaint> filteredList;

    private final FlowPane cardsContainer = new FlowPane();
    private final TableView<Complaint> table = new TableView<>();
    private final StackPane viewContainer = new StackPane();
    private final HBox statsBanner = new HBox(12);

    public CitizenComplaintsHistoryController() {
        buildUI();
        loadData();
    }

    public Node getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(10, 10, 30, 10));

        VBox header = UIComponents.createViewHeader(
                "My Grievance Redressal History",
                "Monitor real-time progress of submitted traffic complaints and police field investigation remarks"
        );

        // Stats Banner
        statsBanner.setAlignment(Pos.CENTER_LEFT);
        updateStatsBanner();

        // Control Bar
        HBox controlBar = new HBox(12);
        controlBar.setAlignment(Pos.CENTER_RIGHT);

        HBox viewToggle = UIComponents.createViewToggle("Status Cards", "Tabular History", isCardView -> {
            if (isCardView) {
                showCardsView();
            } else {
                showTableView();
            }
        });

        Button newBtn = new Button("📝 File New Grievance");
        newBtn.getStyleClass().add("btn-primary");
        newBtn.setOnAction(e -> ViewNavigator.loadMainContent(new FileComplaintController().getView(), "File"));

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> loadData());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        controlBar.getChildren().addAll(spacer, viewToggle, refreshBtn, newBtn);

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

        filteredList = new FilteredList<>(masterList, p -> true);
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

        TableColumn<Complaint, String> colNo = new TableColumn<>("Complaint No");
        colNo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getComplaintNumber()));
        colNo.setPrefWidth(120);

        TableColumn<Complaint, String> colDate = new TableColumn<>("Date Filed");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(DateUtil.formatDateTime(d.getValue().getCreatedAt())));

        TableColumn<Complaint, String> colType = new TableColumn<>("Category");
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getComplaintType()));

        TableColumn<Complaint, String> colVeh = new TableColumn<>("Vehicle Reported");
        colVeh.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getVehicleNumber() != null ? d.getValue().getVehicleNumber() : "N/A"
        ));

        TableColumn<Complaint, String> colJunction = new TableColumn<>("Junction");
        colJunction.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getJunctionName() != null ? d.getValue().getJunctionName() : "General Zone"
        ));

        TableColumn<Complaint, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().getLabel()));

        TableColumn<Complaint, String> colNotes = new TableColumn<>("Police Resolution Remarks");
        colNotes.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getOfficerActionNotes() != null && !d.getValue().getOfficerActionNotes().isEmpty()
                        ? d.getValue().getOfficerActionNotes()
                        : "Awaiting field officer review"
        ));

        TableColumn<Complaint, Void> colAction = new TableColumn<>("Action");
        colAction.setPrefWidth(120);
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button withdrawBtn = new Button("✕ Withdraw");
            {
                withdrawBtn.getStyleClass().addAll("btn-danger", "btn-small");
                withdrawBtn.setOnAction(e -> {
                    Complaint c = getTableView().getItems().get(getIndex());
                    handleWithdrawComplaint(c);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Complaint c = getTableView().getItems().get(getIndex());
                    withdrawBtn.setDisable(c.getStatus() != ComplaintStatus.PENDING);
                    setGraphic(c.getStatus() == ComplaintStatus.PENDING ? withdrawBtn : null);
                }
            }
        });

        table.getColumns().addAll(colNo, colDate, colType, colVeh, colJunction, colStatus, colNotes, colAction);
    }

    private void renderCards() {
        cardsContainer.getChildren().clear();

        if (filteredList.isEmpty()) {
            VBox emptyBox = new VBox(10);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40));
            Label emptyLbl = new Label("No complaints lodged yet. Use 'File New Grievance' to report infractions.");
            emptyLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");
            emptyBox.getChildren().add(emptyLbl);
            cardsContainer.getChildren().add(emptyBox);
            return;
        }

        for (Complaint c : filteredList) {
            VBox card = new VBox(10);
            card.getStyleClass().add("complaint-card");
            card.setPrefWidth(350);

            // Header: Complaint Number + Status Badge
            HBox topRow = new HBox(8);
            topRow.setAlignment(Pos.CENTER_LEFT);

            Label noLbl = new Label(c.getComplaintNumber());
            noLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b;");

            Region topSpacer = new Region();
            HBox.setHgrow(topSpacer, Priority.ALWAYS);

            Label statusBadge = new Label(c.getStatus().getLabel());
            switch (c.getStatus()) {
                case PENDING -> statusBadge.getStyleClass().addAll("badge", "badge-warning");
                case INVESTIGATING -> statusBadge.getStyleClass().addAll("badge", "badge-primary");
                case RESOLVED -> statusBadge.getStyleClass().addAll("badge", "badge-success");
                case REJECTED -> statusBadge.getStyleClass().addAll("badge", "badge-danger");
            }

            topRow.getChildren().addAll(noLbl, topSpacer, statusBadge);

            // Category & Filing Info
            HBox catRow = new HBox(8);
            catRow.setAlignment(Pos.CENTER_LEFT);

            String icon = getCategoryIcon(c.getComplaintType());
            Label typeLbl = new Label(icon + " " + c.getComplaintType());
            typeLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a; -fx-font-size: 13px;");

            Region catSpacer = new Region();
            HBox.setHgrow(catSpacer, Priority.ALWAYS);

            Label dateLbl = new Label(DateUtil.formatDateTime(c.getCreatedAt()));
            dateLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

            catRow.getChildren().addAll(typeLbl, catSpacer, dateLbl);

            // Location & Vehicle
            VBox detailsBox = new VBox(4);
            String loc = c.getJunctionName() != null ? c.getJunctionName() : "General Roadway";
            Label locLbl = new Label("📍 Location: " + loc);
            locLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
            detailsBox.getChildren().add(locLbl);

            if (c.getVehicleNumber() != null && !c.getVehicleNumber().trim().isEmpty() && !c.getVehicleNumber().equalsIgnoreCase("N/A")) {
                HBox plateBox = new HBox(6);
                plateBox.setAlignment(Pos.CENTER_LEFT);
                Label reportedTag = new Label("Reported Vehicle:");
                reportedTag.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
                Label plate = UIComponents.createLicensePlate(c.getVehicleNumber());
                plateBox.getChildren().addAll(reportedTag, plate);
                detailsBox.getChildren().add(plateBox);
            }

            // Description Quote Bubble
            VBox quoteBox = new VBox(4);
            quoteBox.getStyleClass().add("quote-bubble");
            Label quoteTitle = new Label("Your Submitted Incident Details:");
            quoteTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
            Label quoteText = new Label("\"" + c.getDescription() + "\"");
            quoteText.setWrapText(true);
            quoteBox.getChildren().addAll(quoteTitle, quoteText);

            // Police Action / Status Bubble
            VBox actBox = new VBox(4);
            if (c.getOfficerActionNotes() != null && !c.getOfficerActionNotes().trim().isEmpty()) {
                actBox.getStyleClass().add("resolution-bubble");
                Label actTitle = new Label("Police Resolution & Action Taken:");
                actTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #047857;");
                Label actText = new Label(c.getOfficerActionNotes());
                actText.setWrapText(true);
                actBox.getChildren().addAll(actTitle, actText);
            } else {
                actBox.setStyle("-fx-background-color: #fefce8; -fx-border-color: #fef08a; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 8px 12px;");
                Label actTitle = new Label("Investigation Status:");
                actTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #a16207;");
                Label actText = new Label("Your report has been assigned to the junction patrol division for on-site verification.");
                actText.setStyle("-fx-font-size: 11px; -fx-text-fill: #854d0e;");
                actText.setWrapText(true);
                actBox.getChildren().addAll(actTitle, actText);
            }

            HBox bottomRow = new HBox(8);
            bottomRow.setAlignment(Pos.CENTER_RIGHT);
            if (c.getStatus() == ComplaintStatus.PENDING) {
                Button withdrawBtn = new Button("✕ Withdraw Complaint");
                withdrawBtn.getStyleClass().addAll("btn-danger", "btn-small");
                withdrawBtn.setOnAction(e -> handleWithdrawComplaint(c));
                bottomRow.getChildren().add(withdrawBtn);
            }

            if (!bottomRow.getChildren().isEmpty()) {
                card.getChildren().addAll(topRow, catRow, detailsBox, quoteBox, actBox, bottomRow);
            } else {
                card.getChildren().addAll(topRow, catRow, detailsBox, quoteBox, actBox);
            }
            cardsContainer.getChildren().add(card);
        }
    }

    private String getCategoryIcon(String type) {
        if (type == null) return "📌";
        String lower = type.toLowerCase();
        if (lower.contains("parking")) return "🚗";
        if (lower.contains("signal")) return "🚦";
        if (lower.contains("reckless") || lower.contains("speed")) return "🏎️";
        if (lower.contains("obstruction") || lower.contains("hazard")) return "🚧";
        return "⚠️";
    }

    private void updateStatsBanner() {
        statsBanner.getChildren().clear();
        int total = masterList.size();
        long pending = masterList.stream().filter(c -> c.getStatus() == ComplaintStatus.PENDING).count();
        long investigating = masterList.stream().filter(c -> c.getStatus() == ComplaintStatus.INVESTIGATING).count();
        long resolved = masterList.stream().filter(c -> c.getStatus() == ComplaintStatus.RESOLVED).count();

        statsBanner.getChildren().addAll(
                UIComponents.createStatChip("📋", "TOTAL FILED", String.valueOf(total), "#ffffff"),
                UIComponents.createStatChip("⏳", "PENDING REVIEW", String.valueOf(pending), "#fffbeb"),
                UIComponents.createStatChip("🔍", "UNDER INVESTIGATION", String.valueOf(investigating), "#f0f9ff"),
                UIComponents.createStatChip("✅", "RESOLVED & CLOSED", String.valueOf(resolved), "#f0fdf4")
        );
    }

    private void loadData() {
        Citizen citizen = (AuthService.getInstance().getCurrentUser() instanceof Citizen c) ? c : null;
        if (citizen == null) return;

        ThreadPoolManager.getInstance().execute(() -> {
            try {
                List<Complaint> list = complaintService.getComplaintsByCitizen(citizen.getId());
                Platform.runLater(() -> {
                    masterList.setAll(list);
                    updateStatsBanner();
                    renderCards();
                });
            } catch (DatabaseException e) {
                Platform.runLater(() -> UIComponents.showErrorAlert("Database Error", "Failed to load complaints: " + e.getMessage()));
            }
        });
    }

    private void handleWithdrawComplaint(Complaint c) {
        if (UIComponents.showConfirmDialog("Withdraw Grievance",
                "Are you sure you want to withdraw and cancel complaint #" + c.getComplaintNumber() + "?")) {
            try {
                complaintService.deleteComplaint(c.getId());
                UIComponents.showInfoAlert("Withdrawn", "Grievance #" + c.getComplaintNumber() + " has been withdrawn.");
                loadData();
            } catch (Exception ex) {
                UIComponents.showErrorAlert("Error", ex.getMessage());
            }
        }
    }
}
