package com.smarttraffic.controller.admin;

import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.model.EmergencyReport;
import com.smarttraffic.model.Junction;
import com.smarttraffic.model.enums.EmergencySeverity;
import com.smarttraffic.model.enums.EmergencyStatus;
import com.smarttraffic.service.EmergencyService;
import com.smarttraffic.service.JunctionService;
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
 * Controller for Emergency & Accident Command Center: Real-time Incident Cards & Dispatch.
 */
public class AdminEmergencyController {
    private final VBox root = new VBox(15);
    private final EmergencyService emergencyService = EmergencyService.getInstance();
    private final JunctionService junctionService = JunctionService.getInstance();

    private final ObservableList<EmergencyReport> masterList = FXCollections.observableArrayList();
    private final FilteredList<EmergencyReport> filteredList = new FilteredList<>(masterList, p -> true);

    private final FlowPane cardsPane = new FlowPane();
    private final TableView<EmergencyReport> table = new TableView<>();
    private final StackPane viewContainer = new StackPane();

    private final TextField searchField = UIComponents.createSearchField("Search incident, junction, collision type...");
    private final ComboBox<String> severityFilter = new ComboBox<>();
    private final HBox statsBanner = new HBox(12);

    public AdminEmergencyController() {
        buildUI();
        loadData();
    }

    public Node getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(10, 10, 30, 10));

        VBox header = UIComponents.createViewHeader(
                "Accident & Emergency Command Console",
                "Tactical dispatch coordination, road hazard alerts, casualty tracking, and ambulance logistics"
        );

        statsBanner.setAlignment(Pos.CENTER_LEFT);
        updateStatsBanner();

        HBox controlBar = new HBox(12);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        severityFilter.getItems().addAll("All Severities", "CRITICAL", "MAJOR", "MINOR");
        severityFilter.setValue("All Severities");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox viewToggle = UIComponents.createViewToggle("Incident Cards", "Tabular Log", isCardView -> {
            viewContainer.getChildren().clear();
            if (isCardView) {
                viewContainer.getChildren().add(new ScrollPane(cardsPane));
            } else {
                viewContainer.getChildren().add(new VBox(table));
            }
        });

        Button reportBtn = new Button("🚨 Log Incident");
        reportBtn.getStyleClass().add("btn-danger");
        reportBtn.setOnAction(e -> showLogEmergencyDialog());

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> loadData());

        controlBar.getChildren().addAll(searchField, severityFilter, spacer, viewToggle, refreshBtn, reportBtn);

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

        TableColumn<EmergencyReport, String> colNo = new TableColumn<>("Report No");
        colNo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getReportNumber()));
        colNo.setPrefWidth(120);

        TableColumn<EmergencyReport, String> colTime = new TableColumn<>("Incident Time");
        colTime.setCellValueFactory(d -> new SimpleStringProperty(DateUtil.formatDateTime(d.getValue().getReportTime())));

        TableColumn<EmergencyReport, String> colJunction = new TableColumn<>("Junction Location");
        colJunction.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getJunctionName()));

        TableColumn<EmergencyReport, String> colType = new TableColumn<>("Collision / Incident");
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAccidentType()));

        TableColumn<EmergencyReport, String> colSev = new TableColumn<>("Severity");
        colSev.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSeverity().name()));

        TableColumn<EmergencyReport, String> colVehicles = new TableColumn<>("Vehicles");
        colVehicles.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getVehiclesInvolved())));

        TableColumn<EmergencyReport, String> colCasualties = new TableColumn<>("Casualties");
        colCasualties.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getCasualtiesCount())));

        TableColumn<EmergencyReport, String> colStatus = new TableColumn<>("Incident Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().getLabel()));

        TableColumn<EmergencyReport, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(210);
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("⚙️ Dispatch");
            private final Button clearBtn = new Button("✓ Clear");
            private final Button delBtn = new Button("🗑️ Delete");
            private final HBox pane = new HBox(5, editBtn, clearBtn, delBtn);
            {
                editBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                clearBtn.getStyleClass().addAll("btn-success", "btn-small");
                delBtn.getStyleClass().addAll("btn-danger", "btn-small");

                editBtn.setOnAction(e -> {
                    EmergencyReport rep = getTableView().getItems().get(getIndex());
                    showDispatchDialog(rep);
                });

                clearBtn.setOnAction(e -> {
                    EmergencyReport rep = getTableView().getItems().get(getIndex());
                    try {
                        emergencyService.updateStatus(rep.getId(), EmergencyStatus.CLEARED, "Site cleared and traffic normal.");
                        loadData();
                    } catch (Exception ex) {
                        UIComponents.showErrorAlert("Error", ex.getMessage());
                    }
                });

                delBtn.setOnAction(e -> {
                    EmergencyReport rep = getTableView().getItems().get(getIndex());
                    handleDeleteEmergency(rep);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    EmergencyReport rep = getTableView().getItems().get(getIndex());
                    clearBtn.setDisable(rep.getStatus() == EmergencyStatus.CLEARED);
                    setGraphic(pane);
                }
            }
        });

        table.getColumns().addAll(colNo, colTime, colJunction, colType, colSev, colVehicles, colCasualties, colStatus, colActions);
        table.setItems(filteredList);

        Runnable applyFilter = () -> {
            String query = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
            String sev = severityFilter.getValue();

            filteredList.setPredicate(e -> {
                boolean matchesSev = sev == null || sev.equals("All Severities") || e.getSeverity().name().equalsIgnoreCase(sev);
                boolean matchesText = query.isEmpty() ||
                        e.getReportNumber().toLowerCase().contains(query) ||
                        e.getJunctionName().toLowerCase().contains(query) ||
                        e.getAccidentType().toLowerCase().contains(query) ||
                        (e.getNotes() != null && e.getNotes().toLowerCase().contains(query));
                return matchesSev && matchesText;
            });
            renderCardsGrid();
        };

        searchField.textProperty().addListener((obs, o, n) -> applyFilter.run());
        severityFilter.valueProperty().addListener((obs, o, n) -> applyFilter.run());

        viewContainer.getChildren().add(scrollCardPane);

        root.getChildren().addAll(header, statsBanner, controlBar, viewContainer);
    }

    private void renderCardsGrid() {
        cardsPane.getChildren().clear();

        if (filteredList.isEmpty()) {
            VBox emptyBox = new VBox(10);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40));
            Label emptyLbl = new Label("No emergency incidents found.");
            emptyLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");
            emptyBox.getChildren().add(emptyLbl);
            cardsPane.getChildren().add(emptyBox);
            return;
        }

        for (EmergencyReport rep : filteredList) {
            cardsPane.getChildren().add(createEmergencyCard(rep));
        }
    }

    private VBox createEmergencyCard(EmergencyReport rep) {
        VBox card = new VBox(10);
        card.getStyleClass().add("emergency-card");

        if (rep.getSeverity() == EmergencySeverity.CRITICAL) {
            card.getStyleClass().add("emergency-card-critical");
        } else if (rep.getSeverity() == EmergencySeverity.MAJOR) {
            card.getStyleClass().add("emergency-card-major");
        }

        // Top Row: Code + Time + Status Badge
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label rNo = new Label(rep.getReportNumber());
        rNo.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-weight: 900; -fx-text-fill: #b91c1c; -fx-background-color: #fee2e2; -fx-padding: 3px 8px; -fx-background-radius: 4px;");

        Label timeLbl = new Label("⏱️ " + DateUtil.formatDateTime(rep.getReportTime()));
        timeLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        boolean isCleared = rep.getStatus() == EmergencyStatus.CLEARED;
        String badgeStyle = isCleared ? "badge-success" : "badge-danger";
        Label statusBadge = UIComponents.createBadge(rep.getStatus().getLabel(), badgeStyle);

        topRow.getChildren().addAll(rNo, timeLbl, spacer, statusBadge);

        // Incident Title & Junction Location
        VBox titleBox = new VBox(2);
        Label typeLbl = new Label(rep.getAccidentType());
        typeLbl.setStyle("-fx-font-weight: 900; -fx-font-size: 15px; -fx-text-fill: #0f172a;");
        typeLbl.setWrapText(true);

        Label juncLbl = new Label("📍 " + rep.getJunctionName() + " (Reported by: " + rep.getOfficerName() + ")");
        juncLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #2563eb;");
        titleBox.getChildren().addAll(typeLbl, juncLbl);

        // Support Logistics Row
        HBox logisticsRow = new HBox(6);
        logisticsRow.setAlignment(Pos.CENTER_LEFT);

        Label vChip = new Label("🚗 " + rep.getVehiclesInvolved() + " Vehicles");
        vChip.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-background-color: #f1f5f9; -fx-padding: 4px 8px; -fx-background-radius: 4px;");

        Label cChip = new Label("🩸 " + rep.getCasualtiesCount() + " Casualties");
        cChip.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-background-color: " + (rep.getCasualtiesCount() > 0 ? "#fee2e2" : "#f1f5f9") + "; -fx-text-fill: " + (rep.getCasualtiesCount() > 0 ? "#b91c1c" : "#475569") + "; -fx-padding: 4px 8px; -fx-background-radius: 4px;");

        logisticsRow.getChildren().addAll(vChip, cChip);

        if (rep.isAmbulanceRequired()) {
            Label ambChip = new Label("🚑 Ambulance");
            ambChip.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-padding: 4px 8px; -fx-background-radius: 4px;");
            logisticsRow.getChildren().add(ambChip);
        }
        if (rep.isTowTruckRequired()) {
            Label towChip = new Label("🚜 Tow Needed");
            towChip.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4px 8px; -fx-background-radius: 4px;");
            logisticsRow.getChildren().add(towChip);
        }
        if (rep.isRoadBlocked()) {
            Label blockChip = new Label("⛔ Lane Obstructed");
            blockChip.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-padding: 4px 8px; -fx-background-radius: 4px;");
            logisticsRow.getChildren().add(blockChip);
        }

        // Notes Brief
        if (rep.getNotes() != null && !rep.getNotes().isEmpty()) {
            Label notesLbl = new Label("Log: \"" + rep.getNotes() + "\"");
            notesLbl.getStyleClass().add("quote-bubble");
            notesLbl.setWrapText(true);
            card.getChildren().add(notesLbl);
        }

        // Action Buttons Row
        HBox actionRow = new HBox(8);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        actionRow.setPadding(new Insets(4, 0, 0, 0));

        Button updateBtn = new Button("⚙️ Update Dispatch Logistics");
        updateBtn.getStyleClass().addAll("btn-secondary", "btn-small");
        updateBtn.setOnAction(e -> showDispatchDialog(rep));

        Button clearBtn = new Button("✓ Mark Cleared");
        clearBtn.getStyleClass().addAll("btn-success", "btn-small");
        clearBtn.setOnAction(e -> {
            try {
                emergencyService.updateStatus(rep.getId(), EmergencyStatus.CLEARED, "Site cleared and traffic normal.");
                loadData();
            } catch (Exception ex) {
                UIComponents.showErrorAlert("Error", ex.getMessage());
            }
        });

        Button delBtn = new Button("🗑️");
        delBtn.getStyleClass().addAll("btn-danger", "btn-small");
        delBtn.setOnAction(e -> handleDeleteEmergency(rep));

        if (!isCleared) {
            actionRow.getChildren().addAll(updateBtn, clearBtn, delBtn);
        } else {
            actionRow.getChildren().addAll(updateBtn, delBtn);
        }

        card.getChildren().addAll(topRow, titleBox, logisticsRow, actionRow);
        return card;
    }

    private void updateStatsBanner() {
        statsBanner.getChildren().clear();
        int total = masterList.size();
        long critical = masterList.stream().filter(e -> e.getSeverity() == EmergencySeverity.CRITICAL).count();
        long active = masterList.stream().filter(e -> e.getStatus() != EmergencyStatus.CLEARED).count();
        long ambulances = masterList.stream().filter(EmergencyReport::isAmbulanceRequired).count();

        statsBanner.getChildren().addAll(
                UIComponents.createStatChip("🚨", "TOTAL EMERGENCIES", String.valueOf(total), "#ffffff"),
                UIComponents.createStatChip("⚡", "ACTIVE INCIDENTS", String.valueOf(active), "#fef2f2"),
                UIComponents.createStatChip("🔥", "CRITICAL HAZARDS", String.valueOf(critical), "#fff1f2"),
                UIComponents.createStatChip("🚑", "AMBULANCE DISPATCHES", String.valueOf(ambulances), "#f0fdf4")
        );
    }

    private void loadData() {
        ThreadPoolManager.getInstance().execute(() -> {
            try {
                List<EmergencyReport> list = emergencyService.getAllEmergencies();
                Platform.runLater(() -> {
                    masterList.setAll(list);
                    updateStatsBanner();
                    renderCardsGrid();
                });
            } catch (DatabaseException e) {
                Platform.runLater(() -> UIComponents.showErrorAlert("Database Error", "Failed to load emergencies: " + e.getMessage()));
            }
        });
    }

    private void showDispatchDialog(EmergencyReport rep) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(ViewNavigator.getPrimaryStage());
        dialog.setTitle("Manage Emergency Response - " + rep.getReportNumber());

        VBox form = new VBox(12);
        form.setPadding(new Insets(20));
        form.setPrefWidth(480);

        Label title = new Label("Dispatch Coordination & Clearance");
        title.getStyleClass().add("section-title");

        ComboBox<EmergencyStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(EmergencyStatus.values());
        statusCombo.setValue(rep.getStatus());

        TextArea notesArea = new TextArea(rep.getNotes() != null ? rep.getNotes() : "");
        notesArea.setPromptText("Enter logistical updates, hospital coordination, and road clearing details");
        notesArea.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        grid.add(new Label("Update Status *"), 0, 0);
        grid.add(statusCombo, 1, 0);
        grid.add(new Label("Incident Log Notes"), 0, 1);
        grid.add(notesArea, 1, 1);

        HBox btnBar = new HBox(10);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button("Update Response Status");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            try {
                emergencyService.updateStatus(rep.getId(), statusCombo.getValue(), notesArea.getText());
                UIComponents.showInfoAlert("Updated", "Emergency status updated successfully.");
                dialog.close();
                loadData();
            } catch (Exception ex) {
                UIComponents.showErrorAlert("Error", ex.getMessage());
            }
        });

        btnBar.getChildren().addAll(cancelBtn, saveBtn);
        form.getChildren().addAll(title, grid, btnBar);

        Scene scene = new Scene(form);
        ViewNavigator.applyStyles(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showLogEmergencyDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(ViewNavigator.getPrimaryStage());
        dialog.setTitle("Log Traffic Accident / Emergency");

        VBox form = new VBox(12);
        form.setPadding(new Insets(20));
        form.setPrefWidth(500);

        Label title = new Label("New Incident / Collision Report");
        title.getStyleClass().add("section-title");

        ComboBox<Junction> junctionCombo = new ComboBox<>();
        try {
            junctionCombo.getItems().addAll(junctionService.getAllJunctions());
            if (!junctionCombo.getItems().isEmpty()) junctionCombo.setValue(junctionCombo.getItems().get(0));
        } catch (Exception ignored) {
        }

        TextField typeField = new TextField("Vehicular Collision");
        ComboBox<EmergencySeverity> sevCombo = new ComboBox<>();
        sevCombo.getItems().addAll(EmergencySeverity.values());
        sevCombo.setValue(EmergencySeverity.MINOR);

        Spinner<Integer> vehSpinner = new Spinner<>(1, 15, 2);
        Spinner<Integer> casSpinner = new Spinner<>(0, 20, 0);

        CheckBox ambCheck = new CheckBox("Ambulance Required");
        CheckBox towCheck = new CheckBox("Tow Crane Required");
        CheckBox blockCheck = new CheckBox("Road / Lane Obstructed");

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Enter situation details, initial casualties, and rescue needs");
        notesArea.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        grid.add(new Label("Junction Location *"), 0, 0);
        grid.add(junctionCombo, 1, 0);
        grid.add(new Label("Accident / Incident Type *"), 0, 1);
        grid.add(typeField, 1, 1);
        grid.add(new Label("Severity Level *"), 0, 2);
        grid.add(sevCombo, 1, 2);
        grid.add(new Label("Vehicles Involved"), 0, 3);
        grid.add(vehSpinner, 1, 3);
        grid.add(new Label("Casualties Count"), 0, 4);
        grid.add(casSpinner, 1, 4);

        VBox checks = new VBox(6, ambCheck, towCheck, blockCheck);
        grid.add(new Label("Emergency Support"), 0, 5);
        grid.add(checks, 1, 5);

        grid.add(new Label("Incident Description"), 0, 6);
        grid.add(notesArea, 1, 6);

        HBox btnBar = new HBox(10);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button("Submit Emergency Report");
        saveBtn.getStyleClass().add("btn-danger");
        saveBtn.setOnAction(e -> {
            try {
                Junction j = junctionCombo.getValue();
                if (j == null) return;

                EmergencyReport rep = new EmergencyReport(
                        0, null, null, j.getId(), typeField.getText(), sevCombo.getValue(),
                        vehSpinner.getValue(), casSpinner.getValue(), ambCheck.isSelected(),
                        towCheck.isSelected(), blockCheck.isSelected(), null,
                        EmergencyStatus.REPORTED, notesArea.getText()
                );
                emergencyService.reportEmergency(rep);
                UIComponents.showInfoAlert("Logged", "Emergency report dispatched.");
                dialog.close();
                loadData();
            } catch (Exception ex) {
                UIComponents.showErrorAlert("Error", ex.getMessage());
            }
        });

        btnBar.getChildren().addAll(cancelBtn, saveBtn);
        form.getChildren().addAll(title, grid, btnBar);

        Scene scene = new Scene(form);
        ViewNavigator.applyStyles(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void handleDeleteEmergency(EmergencyReport rep) {
        if (UIComponents.showConfirmDialog("Confirm Delete Incident",
                "Are you sure you want to permanently delete emergency incident #" + rep.getReportNumber() + "?")) {
            try {
                emergencyService.deleteEmergency(rep.getId());
                UIComponents.showInfoAlert("Deleted", "Incident #" + rep.getReportNumber() + " removed successfully.");
                loadData();
            } catch (Exception ex) {
                UIComponents.showErrorAlert("Delete Error", ex.getMessage());
            }
        }
    }
}
