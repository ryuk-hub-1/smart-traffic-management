package com.smarttraffic.controller.officer;

import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.model.Complaint;
import com.smarttraffic.model.Officer;
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
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

/**
 * Modernized Controller for Officer Complaints Investigation.
 * Features Visual Grievance Cards, Investigation Notes, and Tabular Toggle.
 */
public class OfficerComplaintsController {
    private final VBox root = new VBox(14);
    private final ComplaintService complaintService = ComplaintService.getInstance();

    private final ObservableList<Complaint> masterList = FXCollections.observableArrayList();
    private FilteredList<Complaint> filteredList;

    private final FlowPane cardsContainer = new FlowPane();
    private final TableView<Complaint> table = new TableView<>();
    private final StackPane viewContainer = new StackPane();
    private final HBox statsBanner = new HBox(12);

    public OfficerComplaintsController() {
        buildUI();
        loadData();
    }

    public Node getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(10, 10, 30, 10));

        VBox header = UIComponents.createViewHeader(
                "My Assigned Citizen Complaints",
                "Investigate citizen traffic reports, take field enforcement actions, and update resolution logs"
        );

        // Stats Banner
        statsBanner.setAlignment(Pos.CENTER_LEFT);
        updateStatsBanner();

        // Control Bar
        HBox controlBar = new HBox(12);
        controlBar.setAlignment(Pos.CENTER_RIGHT);

        HBox viewToggle = UIComponents.createViewToggle("Grievance Cards", "Tabular Log", isCardView -> {
            if (isCardView) {
                showCardsView();
            } else {
                showTableView();
            }
        });

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> loadData());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        controlBar.getChildren().addAll(spacer, viewToggle, refreshBtn);

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

        TableColumn<Complaint, String> colDate = new TableColumn<>("Incident Date");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(DateUtil.formatDateTime(d.getValue().getIncidentDate())));

        TableColumn<Complaint, String> colType = new TableColumn<>("Issue Category");
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getComplaintType()));

        TableColumn<Complaint, String> colVeh = new TableColumn<>("Vehicle Reported");
        colVeh.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getVehicleNumber() != null ? d.getValue().getVehicleNumber() : "N/A"
        ));

        TableColumn<Complaint, String> colJunction = new TableColumn<>("Junction Location");
        colJunction.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getJunctionName() != null ? d.getValue().getJunctionName() : "General Zone"
        ));

        TableColumn<Complaint, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().getLabel()));

        TableColumn<Complaint, String> colNotes = new TableColumn<>("Action Notes");
        colNotes.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getOfficerActionNotes()));

        TableColumn<Complaint, Void> colAction = new TableColumn<>("Action");
        colAction.setPrefWidth(140);
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("✏️ Update Action");
            {
                btn.getStyleClass().addAll("btn-secondary", "btn-small");
                btn.setOnAction(e -> {
                    Complaint c = getTableView().getItems().get(getIndex());
                    showUpdateDialog(c);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
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
            Label emptyLbl = new Label("No citizen grievances currently assigned to your jurisdiction");
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

            // Citizen & Location details
            VBox detailsBox = new VBox(4);
            Label citizenLbl = new Label("👤 Filed by: " + (c.getCitizenName() != null ? c.getCitizenName() : "Anonymous Citizen"));
            citizenLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

            String locationText = c.getJunctionName() != null ? c.getJunctionName() : "General Zone";
            Label locLbl = new Label("📍 Location: " + locationText);
            locLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

            detailsBox.getChildren().addAll(citizenLbl, locLbl);

            if (c.getVehicleNumber() != null && !c.getVehicleNumber().trim().isEmpty() && !c.getVehicleNumber().equalsIgnoreCase("N/A")) {
                HBox plateBox = new HBox(6);
                plateBox.setAlignment(Pos.CENTER_LEFT);
                Label reportedTag = new Label("Reported Plate:");
                reportedTag.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
                Label plate = UIComponents.createLicensePlate(c.getVehicleNumber());
                plateBox.getChildren().addAll(reportedTag, plate);
                detailsBox.getChildren().add(plateBox);
            }

            // Description Quote Bubble
            VBox quoteBox = new VBox(4);
            quoteBox.getStyleClass().add("quote-bubble");
            Label quoteTitle = new Label("Citizen Report:");
            quoteTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
            Label quoteText = new Label("\"" + c.getDescription() + "\"");
            quoteText.setWrapText(true);
            quoteBox.getChildren().addAll(quoteTitle, quoteText);

            // Action notes bubble
            VBox resolutionBox = new VBox(4);
            if (c.getOfficerActionNotes() != null && !c.getOfficerActionNotes().trim().isEmpty()) {
                resolutionBox.getStyleClass().add("resolution-bubble");
                Label actTitle = new Label("My Logged Action Notes:");
                actTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #047857;");
                Label actText = new Label(c.getOfficerActionNotes());
                actText.setWrapText(true);
                resolutionBox.getChildren().addAll(actTitle, actText);
            }

            // Bottom Action Button
            HBox actionRow = new HBox(8);
            actionRow.setAlignment(Pos.CENTER_RIGHT);

            Button updateBtn = new Button("✏️ Update Investigation");
            updateBtn.getStyleClass().addAll("btn-primary", "btn-small");
            updateBtn.setOnAction(e -> showUpdateDialog(c));

            actionRow.getChildren().add(updateBtn);

            card.getChildren().addAll(topRow, catRow, detailsBox, quoteBox, resolutionBox, actionRow);
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
                UIComponents.createStatChip("📋", "ASSIGNED TO ME", String.valueOf(total), "#ffffff"),
                UIComponents.createStatChip("⏳", "PENDING REVIEW", String.valueOf(pending), "#fffbeb"),
                UIComponents.createStatChip("🔍", "INVESTIGATING", String.valueOf(investigating), "#f0f9ff"),
                UIComponents.createStatChip("✅", "RESOLVED & CLOSED", String.valueOf(resolved), "#f0fdf4")
        );
    }

    private void loadData() {
        Officer officer = (AuthService.getInstance().getCurrentUser() instanceof Officer o) ? o : null;
        if (officer == null) return;

        ThreadPoolManager.getInstance().execute(() -> {
            try {
                List<Complaint> list = complaintService.getComplaintsForOfficer(officer.getOfficerId());
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

    private void showUpdateDialog(Complaint c) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(ViewNavigator.getPrimaryStage());
        dialog.setTitle("Update Complaint - " + c.getComplaintNumber());

        VBox form = new VBox(12);
        form.setPadding(new Insets(20));
        form.setPrefWidth(500);

        Label title = new Label("Field Enforcement Resolution");
        title.getStyleClass().add("section-title");

        Label descLbl = new Label("Grievance: \"" + c.getDescription() + "\"");
        descLbl.setStyle("-fx-font-style: italic; -fx-text-fill: #475569; -fx-padding: 8px; -fx-background-color: #f1f5f9; -fx-background-radius: 6px;");
        descLbl.setWrapText(true);

        ComboBox<ComplaintStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(ComplaintStatus.INVESTIGATING, ComplaintStatus.RESOLVED, ComplaintStatus.REJECTED);
        statusCombo.setValue(c.getStatus() == ComplaintStatus.PENDING ? ComplaintStatus.INVESTIGATING : c.getStatus());

        TextArea notesArea = new TextArea(c.getOfficerActionNotes() != null ? c.getOfficerActionNotes() : "");
        notesArea.setPromptText("Enter field observations, towed vehicle details, signal repair reference, or fine issued");
        notesArea.setPrefRowCount(4);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        grid.add(new Label("Update Status *"), 0, 0);
        grid.add(statusCombo, 1, 0);
        grid.add(new Label("Field Action Notes *"), 0, 1);
        grid.add(notesArea, 1, 1);

        HBox btnBar = new HBox(10);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button("Save Field Action");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            try {
                complaintService.updateComplaintStatus(c.getId(), statusCombo.getValue(), c.getAssignedOfficerId(), notesArea.getText());
                UIComponents.showInfoAlert("Success", "Enforcement action recorded.");
                dialog.close();
                loadData();
            } catch (Exception ex) {
                UIComponents.showErrorAlert("Error", ex.getMessage());
            }
        });

        btnBar.getChildren().addAll(cancelBtn, saveBtn);
        form.getChildren().addAll(title, descLbl, grid, btnBar);

        Scene scene = new Scene(form);
        ViewNavigator.applyStyles(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
