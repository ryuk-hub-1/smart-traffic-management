package com.smarttraffic.controller.admin;

import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.exception.ValidationException;
import com.smarttraffic.model.Junction;
import com.smarttraffic.model.Officer;
import com.smarttraffic.model.enums.RiskLevel;
import com.smarttraffic.service.JunctionService;
import com.smarttraffic.service.OfficerService;
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
 * Controller for Admin Junction Management: Interactive Monitoring Nodes & Telemetry Grid.
 */
public class JunctionManagementController {
    private final VBox root = new VBox(15);
    private final JunctionService junctionService = JunctionService.getInstance();
    private final OfficerService officerService = OfficerService.getInstance();

    private final ObservableList<Junction> masterList = FXCollections.observableArrayList();
    private final FilteredList<Junction> filteredList = new FilteredList<>(masterList, p -> true);

    private final FlowPane cardsPane = new FlowPane();
    private final TableView<Junction> table = new TableView<>();
    private final StackPane viewContainer = new StackPane();

    private final TextField searchField = UIComponents.createSearchField("Search junction code, name, area, landmark...");
    private final HBox statsBanner = new HBox(12);

    public JunctionManagementController() {
        buildUI();
        loadData();
    }

    public Node getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(10, 10, 30, 10));

        VBox header = UIComponents.createViewHeader(
                "Traffic Junction Monitoring & Control Hub",
                "Monitor city intersections, CCTV camera telemetry feeds, congestion hazard ratings, and police deployment"
        );

        statsBanner.setAlignment(Pos.CENTER_LEFT);
        updateStatsBanner();

        HBox controlBar = new HBox(12);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox viewToggle = UIComponents.createViewToggle("Monitoring Nodes", "Tabular List", isCardView -> {
            viewContainer.getChildren().clear();
            if (isCardView) {
                viewContainer.getChildren().add(new ScrollPane(cardsPane));
            } else {
                viewContainer.getChildren().add(new VBox(table));
            }
        });

        Button addBtn = new Button("➕ Add New Junction");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> showJunctionDialog(null));

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> loadData());

        controlBar.getChildren().addAll(searchField, spacer, viewToggle, refreshBtn, addBtn);

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

        TableColumn<Junction, String> colCode = new TableColumn<>("Junction Code");
        colCode.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getJunctionCode()));
        colCode.setPrefWidth(110);

        TableColumn<Junction, String> colName = new TableColumn<>("Junction Name");
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));

        TableColumn<Junction, String> colArea = new TableColumn<>("Zone / Area");
        colArea.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getArea()));

        TableColumn<Junction, String> colLandmark = new TableColumn<>("Landmark");
        colLandmark.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getLandmark()));

        TableColumn<Junction, String> colCoords = new TableColumn<>("GPS Coordinates");
        colCoords.setCellValueFactory(d -> new SimpleStringProperty(
                String.format("%.4f, %.4f", d.getValue().getLatitude(), d.getValue().getLongitude())
        ));

        TableColumn<Junction, String> colRisk = new TableColumn<>("Congestion / Risk");
        colRisk.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRiskLevel().name()));

        TableColumn<Junction, String> colOfficers = new TableColumn<>("Assigned Officers");
        colOfficers.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getActiveOfficersCount() + " Officers"));

        TableColumn<Junction, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(230);
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button signalBtn = new Button("🚦 Relay");
            private final Button editBtn = new Button("✏️ Edit");
            private final Button delBtn = new Button("🗑️ Delete");
            private final HBox pane = new HBox(6, signalBtn, editBtn, delBtn);
            {
                signalBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                editBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                delBtn.getStyleClass().addAll("btn-danger", "btn-small");

                signalBtn.setOnAction(e -> {
                    Junction j = getTableView().getItems().get(getIndex());
                    showSignalControlDialog(j);
                });

                editBtn.setOnAction(e -> {
                    Junction j = getTableView().getItems().get(getIndex());
                    showJunctionDialog(j);
                });

                delBtn.setOnAction(e -> {
                    Junction j = getTableView().getItems().get(getIndex());
                    handleDeleteJunction(j);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        table.getColumns().addAll(colCode, colName, colArea, colLandmark, colCoords, colRisk, colOfficers, colActions);
        table.setItems(filteredList);

        searchField.textProperty().addListener((obs, oldV, newV) -> {
            filteredList.setPredicate(j -> {
                if (newV == null || newV.trim().isEmpty()) return true;
                String lower = newV.trim().toLowerCase();
                return j.getJunctionCode().toLowerCase().contains(lower) ||
                       j.getName().toLowerCase().contains(lower) ||
                       j.getArea().toLowerCase().contains(lower) ||
                       (j.getLandmark() != null && j.getLandmark().toLowerCase().contains(lower));
            });
            renderCardsGrid();
        });

        viewContainer.getChildren().add(scrollCardPane);

        root.getChildren().addAll(header, statsBanner, controlBar, viewContainer);
    }

    private void renderCardsGrid() {
        cardsPane.getChildren().clear();

        if (filteredList.isEmpty()) {
            VBox emptyBox = new VBox(10);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40));
            Label emptyLbl = new Label("No junctions found matching search criteria.");
            emptyLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");
            emptyBox.getChildren().add(emptyLbl);
            cardsPane.getChildren().add(emptyBox);
            return;
        }

        for (Junction j : filteredList) {
            cardsPane.getChildren().add(createJunctionCard(j));
        }
    }

    private VBox createJunctionCard(Junction j) {
        VBox card = new VBox(12);
        card.getStyleClass().add("junction-card");

        // Top Row: Code Badge + Risk Badge
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label codeBadge = new Label(j.getJunctionCode());
        codeBadge.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #38bdf8; -fx-font-weight: 800; -fx-font-size: 11px; -fx-padding: 3px 8px; -fx-background-radius: 4px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        RiskLevel risk = j.getRiskLevel() != null ? j.getRiskLevel() : RiskLevel.MEDIUM;
        String riskStyle = risk == RiskLevel.HIGH ? "badge-danger" : risk == RiskLevel.MEDIUM ? "badge-warning" : "badge-success";
        String riskText = risk == RiskLevel.HIGH ? "🚨 CRITICAL RISK" : risk == RiskLevel.MEDIUM ? "⚠️ MODERATE RISK" : "🟢 LOW CONGESTION";
        Label riskBadge = UIComponents.createBadge(riskText, riskStyle);

        topRow.getChildren().addAll(codeBadge, spacer, riskBadge);

        // Junction Title & Area
        VBox titleBox = new VBox(2);
        Label nameLbl = new Label(j.getName());
        nameLbl.setStyle("-fx-font-weight: 900; -fx-font-size: 15px; -fx-text-fill: #0f172a;");
        nameLbl.setWrapText(true);

        Label areaLbl = new Label("📍 " + j.getArea() + (j.getLandmark() != null && !j.getLandmark().isEmpty() ? " • " + j.getLandmark() : ""));
        areaLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        areaLbl.setWrapText(true);
        titleBox.getChildren().addAll(nameLbl, areaLbl);

        // Telemetry Monitoring Box
        VBox telemetry = new VBox(6);
        telemetry.getStyleClass().add("telemetry-box");

        HBox tRow1 = new HBox(10);
        Label camStatus = new Label("📹 4/4 CCTV Feeds Active 🟢");
        camStatus.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #15803d;");
        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);
        Label signalStatus = new Label("🚦 Signal Relay Synced 🟢");
        signalStatus.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #15803d;");
        tRow1.getChildren().addAll(camStatus, sp2, signalStatus);

        HBox tRow2 = new HBox(8);
        tRow2.setAlignment(Pos.CENTER_LEFT);
        Label coordLbl = new Label(String.format("🌐 GPS: %.4f° N, %.4f° E", j.getLatitude(), j.getLongitude()));
        coordLbl.setStyle("-fx-font-size: 10px; -fx-font-family: 'Consolas', monospace; -fx-text-fill: #64748b;");
        tRow2.getChildren().add(coordLbl);

        telemetry.getChildren().addAll(tRow1, tRow2);

        // Deployed Officers Chip
        HBox officerChip = new HBox(8);
        officerChip.setAlignment(Pos.CENTER_LEFT);
        officerChip.setStyle("-fx-background-color: #eff6ff; -fx-padding: 6px 10px; -fx-background-radius: 6px;");
        Label offCount = new Label("👮 Assigned Force: " + j.getActiveOfficersCount() + " Active Officers");
        offCount.setStyle("-fx-font-weight: 800; -fx-font-size: 11px; -fx-text-fill: #1d4ed8;");
        officerChip.getChildren().add(offCount);

        // Action Buttons Row
        HBox actionRow = new HBox(8);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        actionRow.setPadding(new Insets(4, 0, 0, 0));

        Button signalBtn = new Button("🚦 Relay");
        signalBtn.getStyleClass().addAll("btn-secondary", "btn-small");
        signalBtn.setOnAction(e -> showSignalControlDialog(j));

        Button editBtn = new Button("✏️ Edit Node");
        editBtn.getStyleClass().addAll("btn-secondary", "btn-small");
        editBtn.setOnAction(e -> showJunctionDialog(j));

        Button delBtn = new Button("🗑️ Delete");
        delBtn.getStyleClass().addAll("btn-danger", "btn-small");
        delBtn.setOnAction(e -> handleDeleteJunction(j));

        actionRow.getChildren().addAll(signalBtn, editBtn, delBtn);

        card.getChildren().addAll(topRow, titleBox, telemetry, officerChip, actionRow);
        return card;
    }

    private void updateStatsBanner() {
        statsBanner.getChildren().clear();
        int total = masterList.size();
        long high = masterList.stream().filter(j -> j.getRiskLevel() == RiskLevel.HIGH).count();
        long med = masterList.stream().filter(j -> j.getRiskLevel() == RiskLevel.MEDIUM).count();
        long low = masterList.stream().filter(j -> j.getRiskLevel() == RiskLevel.LOW).count();

        statsBanner.getChildren().addAll(
                UIComponents.createStatChip("📍", "TOTAL INTERSECTIONS", String.valueOf(total), "#ffffff"),
                UIComponents.createStatChip("🚨", "HIGH CONGESTION", String.valueOf(high), "#fef2f2"),
                UIComponents.createStatChip("⚠️", "MODERATE FLOW", String.valueOf(med), "#fffbeb"),
                UIComponents.createStatChip("🟢", "OPTIMAL FLOW", String.valueOf(low), "#f0fdf4")
        );
    }

    private void loadData() {
        ThreadPoolManager.getInstance().execute(() -> {
            try {
                List<Junction> list = junctionService.getAllJunctions();
                Platform.runLater(() -> {
                    masterList.setAll(list);
                    updateStatsBanner();
                    renderCardsGrid();
                });
            } catch (DatabaseException e) {
                Platform.runLater(() -> UIComponents.showErrorAlert("Database Error", "Failed to load junctions: " + e.getMessage()));
            }
        });
    }

    private void showJunctionDialog(Junction existing) {
        boolean isEdit = existing != null;
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(ViewNavigator.getPrimaryStage());
        dialog.setTitle(isEdit ? "Edit Junction - " + existing.getJunctionCode() : "Create Traffic Junction");

        VBox form = new VBox(12);
        form.setPadding(new Insets(20));
        form.setPrefWidth(450);

        Label title = new Label(isEdit ? "Update Junction Information" : "Add New Traffic Junction");
        title.getStyleClass().add("section-title");

        TextField codeField = new TextField();
        TextField nameField = new TextField();
        TextField areaField = new TextField();
        TextField landmarkField = new TextField();
        TextField latField = new TextField("12.9716");
        TextField lonField = new TextField("77.5946");
        ComboBox<RiskLevel> riskCombo = new ComboBox<>();
        riskCombo.getItems().addAll(RiskLevel.values());
        riskCombo.setValue(RiskLevel.MEDIUM);

        if (isEdit) {
            codeField.setText(existing.getJunctionCode());
            codeField.setDisable(true);
            nameField.setText(existing.getName());
            areaField.setText(existing.getArea());
            landmarkField.setText(existing.getLandmark());
            latField.setText(String.valueOf(existing.getLatitude()));
            lonField.setText(String.valueOf(existing.getLongitude()));
            riskCombo.setValue(existing.getRiskLevel());
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        grid.add(new Label("Junction Code *"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label("Junction Name *"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Area / Division *"), 0, 2);
        grid.add(areaField, 1, 2);
        grid.add(new Label("Landmark"), 0, 3);
        grid.add(landmarkField, 1, 3);
        grid.add(new Label("Latitude"), 0, 4);
        grid.add(latField, 1, 4);
        grid.add(new Label("Longitude"), 0, 5);
        grid.add(lonField, 1, 5);
        grid.add(new Label("Risk Level"), 0, 6);
        grid.add(riskCombo, 1, 6);

        HBox btnBar = new HBox(10);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button(isEdit ? "Save Changes" : "Create Junction");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            try {
                double lat = Double.parseDouble(latField.getText().trim());
                double lon = Double.parseDouble(lonField.getText().trim());

                if (isEdit) {
                    existing.setName(nameField.getText().trim());
                    existing.setArea(areaField.getText().trim());
                    existing.setLandmark(landmarkField.getText().trim());
                    existing.setLatitude(lat);
                    existing.setLongitude(lon);
                    existing.setRiskLevel(riskCombo.getValue());
                    junctionService.updateJunction(existing);
                    UIComponents.showInfoAlert("Success", "Junction updated successfully.");
                } else {
                    Junction j = new Junction(0, codeField.getText().trim(), nameField.getText().trim(),
                            areaField.getText().trim(), landmarkField.getText().trim(), lat, lon, riskCombo.getValue());
                    junctionService.addJunction(j);
                    UIComponents.showInfoAlert("Success", "New Junction created successfully.");
                }
                dialog.close();
                loadData();
            } catch (NumberFormatException ex) {
                UIComponents.showWarningAlert("Input Error", "Latitude and Longitude must be valid decimal numbers.");
            } catch (ValidationException ex) {
                UIComponents.showWarningAlert("Validation Error", ex.getMessage());
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

    private void handleDeleteJunction(Junction j) {
        if (UIComponents.showConfirmDialog("Confirm Delete Junction",
                "Are you sure you want to delete junction " + j.getName() + " [" + j.getJunctionCode() + "]?\nThis will also remove beat assignments and clear related telemetry.")) {
            try {
                junctionService.deleteJunction(j.getId());
                UIComponents.showInfoAlert("Deleted", "Junction removed successfully.");
                loadData();
            } catch (Exception e) {
                UIComponents.showErrorAlert("Delete Error", "Could not delete junction: " + e.getMessage());
            }
        }
    }

    private void showSignalControlDialog(Junction j) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(ViewNavigator.getPrimaryStage());
        dialog.setTitle("Smart Signal Control - " + j.getJunctionCode());

        VBox form = new VBox(14);
        form.setPadding(new Insets(20));
        form.setPrefWidth(480);

        Label title = new Label("🚦 Intersection Signal Relay Control");
        title.getStyleClass().add("section-title");

        Label subLbl = new Label("Override signal timings, activate emergency green waves, or set flashing caution mode for " + j.getName());
        subLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        subLbl.setWrapText(true);

        ComboBox<String> modeCombo = new ComboBox<>();
        modeCombo.getItems().addAll(
                "🟢 AUTO-SYNC (Green Wave Coordinated - Normal Sensor Mode)",
                "🟡 FLASHING CAUTION (Night / Low Congestion Priority Road)",
                "🔴 ALL-RED EMERGENCY HOLD (Clear intersection for ambulance)",
                "⏱️ PEAK HOUR EXTENSION (90s Green Split for congestion relief)"
        );
        modeCombo.setValue(modeCombo.getItems().get(0));

        Spinner<Integer> timerSpinner = new Spinner<>(15, 180, 60);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Select Signal Mode *"), 0, 0);
        grid.add(modeCombo, 1, 0);
        grid.add(new Label("Green Split Timer (Seconds)"), 0, 1);
        grid.add(timerSpinner, 1, 1);

        HBox btnBar = new HBox(10);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> dialog.close());

        Button applyBtn = new Button("Apply Signal Relay Command ➔");
        applyBtn.getStyleClass().add("btn-primary");
        applyBtn.setOnAction(e -> {
            String selected = modeCombo.getValue();
            int timer = timerSpinner.getValue();
            String modeShort = selected.split(" ")[0] + " " + selected.split(" ")[1];
            UIComponents.showInfoAlert("Signal Command Broadcasted",
                    "Signal telemetry command sent to " + j.getName() + " (" + j.getJunctionCode() + "):\n" +
                            "• Mode: " + modeShort + "\n" +
                            "• Split Duration: " + timer + " seconds\n" +
                            "• Telemetry Status: Synced with City Traffic Control Network.");
            dialog.close();
        });

        btnBar.getChildren().addAll(cancelBtn, applyBtn);
        form.getChildren().addAll(title, subLbl, grid, btnBar);

        Scene scene = new Scene(form);
        ViewNavigator.applyStyles(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
