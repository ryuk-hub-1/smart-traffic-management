package com.smarttraffic.controller.admin;

import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.model.Complaint;
import com.smarttraffic.model.Officer;
import com.smarttraffic.model.enums.ComplaintStatus;
import com.smarttraffic.service.ComplaintService;
import com.smarttraffic.service.OfficerService;
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
 * Modernized Controller for Admin Citizen Complaint & Grievance Redressal.
 * Features Visual Grievance Cards, Status Telemetry, Officer Dispatch, and Tabular Audit Log.
 */
public class AdminComplaintsController {
    private final VBox root = new VBox(14);
    private final ComplaintService complaintService = ComplaintService.getInstance();
    private final OfficerService officerService = OfficerService.getInstance();

    private final ObservableList<Complaint> masterList = FXCollections.observableArrayList();
    private FilteredList<Complaint> filteredList;

    private final FlowPane cardsContainer = new FlowPane();
    private final TableView<Complaint> table = new TableView<>();
    private final StackPane viewContainer = new StackPane();

    private final TextField searchField = UIComponents.createSearchField("Search complaint #, citizen, vehicle, issue...");
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final HBox statsBanner = new HBox(12);

    public AdminComplaintsController() {
        buildUI();
        loadData();
    }

    public Node getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(10, 10, 30, 10));

        VBox header = UIComponents.createViewHeader(
                "Citizen Grievance & Incident Redressal",
                "Review public complaints regarding illegal parking, broken signals, and hazardous driving; dispatch officers and record resolutions"
        );

        // Stats Banner
        statsBanner.setAlignment(Pos.CENTER_LEFT);
        updateStatsBanner();

        // Control Bar
        HBox controlBar = new HBox(12);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        statusFilter.getItems().addAll("All Statuses", "PENDING", "INVESTIGATING", "RESOLVED", "REJECTED");
        statusFilter.setValue("All Statuses");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox viewToggle = UIComponents.createViewToggle("Grievance Cards", "Audit Table", isCardView -> {
            if (isCardView) {
                showCardsView();
            } else {
                showTableView();
            }
        });

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> loadData());

        controlBar.getChildren().addAll(searchField, statusFilter, spacer, viewToggle, refreshBtn);

        // Cards View
        cardsContainer.setHgap(16);
        cardsContainer.setVgap(16);
        cardsContainer.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(cardsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        scrollPane.setPrefHeight(520);

        // Tabular View
        buildTableView();

        viewContainer.getChildren().addAll(scrollPane, table);
        showCardsView();

        // Filters Setup
        filteredList = new FilteredList<>(masterList, p -> true);

        Runnable applyFilter = () -> {
            String query = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
            String stat = statusFilter.getValue();

            filteredList.setPredicate(c -> {
                boolean matchesStat = stat == null || stat.equals("All Statuses") || c.getStatus().name().equalsIgnoreCase(stat);
                boolean matchesText = query.isEmpty() ||
                        c.getComplaintNumber().toLowerCase().contains(query) ||
                        c.getCitizenName().toLowerCase().contains(query) ||
                        c.getComplaintType().toLowerCase().contains(query) ||
                        (c.getVehicleNumber() != null && c.getVehicleNumber().toLowerCase().contains(query)) ||
                        c.getDescription().toLowerCase().contains(query) ||
                        (c.getJunctionName() != null && c.getJunctionName().toLowerCase().contains(query));
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

        TableColumn<Complaint, String> colNo = new TableColumn<>("Complaint No");
        colNo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getComplaintNumber()));
        colNo.setPrefWidth(120);

        TableColumn<Complaint, String> colDate = new TableColumn<>("Filed Date");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(DateUtil.formatDateTime(d.getValue().getCreatedAt())));

        TableColumn<Complaint, String> colCitizen = new TableColumn<>("Citizen Name");
        colCitizen.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCitizenName()));

        TableColumn<Complaint, String> colType = new TableColumn<>("Category / Issue");
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getComplaintType()));

        TableColumn<Complaint, String> colVeh = new TableColumn<>("Vehicle Reported");
        colVeh.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getVehicleNumber() != null ? d.getValue().getVehicleNumber() : "N/A"
        ));

        TableColumn<Complaint, String> colJunction = new TableColumn<>("Junction Location");
        colJunction.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getJunctionName() != null ? d.getValue().getJunctionName() : "General Zone"
        ));

        TableColumn<Complaint, String> colStatus = new TableColumn<>("Current Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().getLabel()));

        TableColumn<Complaint, String> colOfficer = new TableColumn<>("Assigned Officer");
        colOfficer.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getAssignedOfficerName() != null ? d.getValue().getAssignedOfficerName() : "Unassigned"
        ));

        TableColumn<Complaint, Void> colAction = new TableColumn<>("Action");
        colAction.setPrefWidth(210);
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button actBtn = new Button("⚙️ Review");
            private final Button delBtn = new Button("🗑️ Delete");
            private final HBox pane = new HBox(6, actBtn, delBtn);
            {
                actBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                delBtn.getStyleClass().addAll("btn-danger", "btn-small");

                actBtn.setOnAction(e -> {
                    Complaint c = getTableView().getItems().get(getIndex());
                    showResolutionDialog(c);
                });

                delBtn.setOnAction(e -> {
                    Complaint c = getTableView().getItems().get(getIndex());
                    handleDeleteComplaint(c);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        table.getColumns().addAll(colNo, colDate, colCitizen, colType, colVeh, colJunction, colStatus, colOfficer, colAction);
    }

    private void renderCards() {
        cardsContainer.getChildren().clear();

        if (filteredList.isEmpty()) {
            VBox emptyBox = new VBox(10);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40));
            Label emptyLbl = new Label("No grievance reports match your search criteria");
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
            Label citizenLbl = new Label("👤 Citizen: " + c.getCitizenName());
            citizenLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

            String locationText = c.getJunctionName() != null ? c.getJunctionName() : "General Zone";
            Label locLbl = new Label("📍 Location: " + locationText);
            locLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

            detailsBox.getChildren().addAll(citizenLbl, locLbl);

            // If a vehicle number is involved, render plate
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
            Label quoteTitle = new Label("Citizen Incident Report:");
            quoteTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
            Label quoteText = new Label("\"" + c.getDescription() + "\"");
            quoteText.setWrapText(true);
            quoteBox.getChildren().addAll(quoteTitle, quoteText);

            // Officer Assignment & Action Notes
            VBox resolutionBox = new VBox(4);
            if (c.getAssignedOfficerName() != null && !c.getAssignedOfficerName().isEmpty()) {
                Label offLbl = new Label("👮 Assigned: " + c.getAssignedOfficerName());
                offLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");
                resolutionBox.getChildren().add(offLbl);
            } else {
                Label unassignedLbl = new Label("⚠️ Officer Unassigned");
                unassignedLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #b45309;");
                resolutionBox.getChildren().add(unassignedLbl);
            }

            if (c.getOfficerActionNotes() != null && !c.getOfficerActionNotes().trim().isEmpty()) {
                VBox actNote = new VBox(2);
                actNote.getStyleClass().add("resolution-bubble");
                Label actTitle = new Label("Police Action & Findings:");
                actTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #047857;");
                Label actText = new Label(c.getOfficerActionNotes());
                actText.setWrapText(true);
                actNote.getChildren().addAll(actTitle, actText);
                resolutionBox.getChildren().add(actNote);
            }

            // Card Bottom Action
            HBox actionRow = new HBox(8);
            actionRow.setAlignment(Pos.CENTER_RIGHT);

            Button resolveBtn = new Button("⚙️ Review & Resolve");
            resolveBtn.getStyleClass().addAll("btn-primary", "btn-small");
            resolveBtn.setOnAction(e -> showResolutionDialog(c));

            Button delBtn = new Button("🗑️ Delete");
            delBtn.getStyleClass().addAll("btn-danger", "btn-small");
            delBtn.setOnAction(e -> handleDeleteComplaint(c));

            actionRow.getChildren().addAll(resolveBtn, delBtn);

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
                UIComponents.createStatChip("📋", "TOTAL GRIEVANCES", String.valueOf(total), "#ffffff"),
                UIComponents.createStatChip("⏳", "PENDING REVIEW", String.valueOf(pending), "#fffbeb"),
                UIComponents.createStatChip("🔍", "UNDER INVESTIGATION", String.valueOf(investigating), "#f0f9ff"),
                UIComponents.createStatChip("✅", "RESOLVED & CLOSED", String.valueOf(resolved), "#f0fdf4")
        );
    }

    private void loadData() {
        ThreadPoolManager.getInstance().execute(() -> {
            try {
                List<Complaint> list = complaintService.getAllComplaints();
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

    private void showResolutionDialog(Complaint c) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(ViewNavigator.getPrimaryStage());
        dialog.setTitle("Complaint Redressal - " + c.getComplaintNumber());

        VBox form = new VBox(12);
        form.setPadding(new Insets(20));
        form.setPrefWidth(540);

        Label title = new Label("Review & Resolve Grievance");
        title.getStyleClass().add("section-title");

        Label descLbl = new Label("Citizen Incident Report:\n\"" + c.getDescription() + "\"");
        descLbl.setStyle("-fx-font-style: italic; -fx-text-fill: #475569; -fx-padding: 10px; -fx-background-color: #f1f5f9; -fx-background-radius: 6px;");
        descLbl.setWrapText(true);

        ComboBox<Officer> officerCombo = new ComboBox<>();
        try {
            officerCombo.getItems().addAll(officerService.getAllOfficers());
            if (c.getAssignedOfficerId() != null) {
                for (Officer o : officerCombo.getItems()) {
                    if (o.getOfficerId() == c.getAssignedOfficerId()) {
                        officerCombo.setValue(o);
                        break;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        ComboBox<ComplaintStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(ComplaintStatus.values());
        statusCombo.setValue(c.getStatus());

        TextArea remarksArea = new TextArea(c.getOfficerActionNotes() != null ? c.getOfficerActionNotes() : "");
        remarksArea.setPromptText("Enter police action, dispatch details, or resolution remarks");
        remarksArea.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Assign Officer"), 0, 0);
        grid.add(officerCombo, 1, 0);
        grid.add(new Label("Update Status *"), 0, 1);
        grid.add(statusCombo, 1, 1);
        grid.add(new Label("Action Notes"), 0, 2);
        grid.add(remarksArea, 1, 2);

        HBox btnBar = new HBox(10);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button("Save Resolution");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            try {
                Officer selectedOff = officerCombo.getValue();
                Integer offId = selectedOff != null ? selectedOff.getOfficerId() : null;
                complaintService.updateComplaintStatus(c.getId(), statusCombo.getValue(), offId, remarksArea.getText());
                UIComponents.showInfoAlert("Updated", "Complaint status and action notes recorded.");
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

    private void handleDeleteComplaint(Complaint c) {
        if (UIComponents.showConfirmDialog("Confirm Delete Complaint",
                "Are you sure you want to permanently delete grievance #" + c.getComplaintNumber() + "?")) {
            try {
                complaintService.deleteComplaint(c.getId());
                UIComponents.showInfoAlert("Deleted", "Grievance #" + c.getComplaintNumber() + " removed successfully.");
                loadData();
            } catch (Exception ex) {
                UIComponents.showErrorAlert("Delete Error", ex.getMessage());
            }
        }
    }
}
