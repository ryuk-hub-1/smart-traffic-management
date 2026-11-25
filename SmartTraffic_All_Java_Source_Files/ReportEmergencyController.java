package com.smarttraffic.controller.officer;

import com.smarttraffic.exception.ValidationException;
import com.smarttraffic.model.EmergencyReport;
import com.smarttraffic.model.Junction;
import com.smarttraffic.model.Officer;
import com.smarttraffic.model.enums.EmergencySeverity;
import com.smarttraffic.model.enums.EmergencyStatus;
import com.smarttraffic.service.AuthService;
import com.smarttraffic.service.EmergencyService;
import com.smarttraffic.service.JunctionService;
import com.smarttraffic.view.UIComponents;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Officer Field Emergency & Accident Incident Reporting.
 * Features Visual Severity Selection Tiles, Rapid Incident Type Chips, and Priority Logistics.
 */
public class ReportEmergencyController {
    private final VBox root = new VBox(15);
    private final EmergencyService emergencyService = EmergencyService.getInstance();
    private final JunctionService junctionService = JunctionService.getInstance();

    private final ObjectProperty<EmergencySeverity> selectedSeverity = new SimpleObjectProperty<>(EmergencySeverity.MAJOR);
    private final StringProperty incidentType = new SimpleStringProperty("Vehicular Collision");

    private final List<VBox> severityTiles = new ArrayList<>();
    private final List<Button> incidentChips = new ArrayList<>();

    private final ComboBox<Junction> junctionCombo = new ComboBox<>();
    private final TextField incidentTypeField = new TextField("Vehicular Collision");
    private final Spinner<Integer> vehiclesSpinner = new Spinner<>(1, 20, 2);
    private final Spinner<Integer> casualtiesSpinner = new Spinner<>(0, 50, 0);
    private final CheckBox ambulanceCheck = new CheckBox("🚑 Immediate Ambulance & Paramedics Required");
    private final CheckBox towTruckCheck = new CheckBox("🚜 Heavy Tow Truck / Crane Required");
    private final CheckBox roadBlockedCheck = new CheckBox("⛔ Total Traffic Gridlock / Lane Blockage");
    private final TextArea notesArea = new TextArea();

    public ReportEmergencyController() {
        buildUI();
    }

    public Node getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(10, 10, 30, 10));

        VBox header = UIComponents.createViewHeader(
                "Emergency Accident & Hazard Dispatch",
                "Broadcast immediate road accidents, casualty requests, and emergency logistics to Headquarters"
        );

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        VBox contentBox = new VBox(20);
        contentBox.setMaxWidth(880);

        // Card 1: Severity Matrix
        VBox sevCard = new VBox(10);
        sevCard.getStyleClass().add("content-card");

        Label sevTitle = new Label("1. Incident Urgency & Severity Classification");
        sevTitle.getStyleClass().add("section-title");

        FlowPane sevTilesPane = new FlowPane();
        sevTilesPane.setHgap(12);
        sevTilesPane.setVgap(12);

        addSeverityTile(sevTilesPane, "🟢", "Minor Incident", "Fender-bender, bumper scratch, no injuries", EmergencySeverity.MINOR);
        addSeverityTile(sevTilesPane, "🟠", "Major Emergency", "Severe vehicular damage, injuries, multiple vehicles", EmergencySeverity.MAJOR);
        addSeverityTile(sevTilesPane, "🔴", "Critical / Catastrophic", "Fatal collision, fire/hazmat spill, arterial shutdown", EmergencySeverity.CRITICAL);

        sevCard.getChildren().addAll(sevTitle, sevTilesPane);

        // Card 2: Incident Nature & Quick Chips
        VBox natureCard = new VBox(12);
        natureCard.getStyleClass().add("content-card");

        Label natureTitle = new Label("2. Incident Category & Nature");
        natureTitle.getStyleClass().add("section-title");

        Label natureDesc = new Label("Select quick preset or type custom incident:");
        natureDesc.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");

        FlowPane chipsPane = new FlowPane();
        chipsPane.setHgap(8);
        chipsPane.setVgap(8);

        addIncidentChip(chipsPane, "💥 Vehicular Collision");
        addIncidentChip(chipsPane, "🚸 Pedestrian Hit");
        addIncidentChip(chipsPane, "🧯 Vehicle Fire / Explosion");
        addIncidentChip(chipsPane, "🚛 Rollover / Cargo Spill");
        addIncidentChip(chipsPane, "⚡ Signal Pole / Divider Crash");
        addIncidentChip(chipsPane, "🌊 Oil / Chemical Spill on Road");

        incidentTypeField.textProperty().bindBidirectional(incidentType);

        natureCard.getChildren().addAll(natureTitle, natureDesc, chipsPane, createField("Accident Nature Description *", incidentTypeField));

        // Card 3: Location, Casualties, & Logistics
        VBox formCard = new VBox(16);
        formCard.getStyleClass().add("content-card");

        Label formTitle = new Label("3. Field Location & Rapid Logistics Request");
        formTitle.getStyleClass().add("section-title");

        try {
            List<Junction> junctions = junctionService.getAllJunctions();
            junctionCombo.getItems().addAll(junctions);
            Officer officer = (AuthService.getInstance().getCurrentUser() instanceof Officer o) ? o : null;
            if (officer != null && officer.getAssignedJunctionId() != null) {
                for (Junction j : junctions) {
                    if (j.getId() == officer.getAssignedJunctionId()) {
                        junctionCombo.setValue(j);
                        break;
                    }
                }
            } else if (!junctions.isEmpty()) {
                junctionCombo.setValue(junctions.get(0));
            }
        } catch (Exception ignored) {
        }

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);

        grid.add(createField("Incident Junction Location *", junctionCombo), 0, 0);

        HBox countsBox = new HBox(16);
        countsBox.getChildren().addAll(
                createField("Vehicles Involved", vehiclesSpinner),
                createField("Casualties / Injured Persons", casualtiesSpinner)
        );
        grid.add(countsBox, 1, 0);

        VBox checkboxes = new VBox(8, ambulanceCheck, towTruckCheck, roadBlockedCheck);
        checkboxes.setStyle("-fx-background-color: #fef2f2; -fx-padding: 12px; -fx-background-radius: 8px; -fx-border-color: #fecaca; -fx-border-radius: 8px;");
        grid.add(createField("High-Priority Logistics Response Needed", checkboxes), 0, 1, 2, 1);

        notesArea.setPrefRowCount(3);
        notesArea.setPromptText("Describe lane blockage details, hospital ETA, trapped persons, fuel spillage, police backup needed");
        grid.add(createField("Situation Report & Field Notes", notesArea), 0, 2, 2, 1);

        HBox btnBar = new HBox(12);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(10, 0, 0, 0));

        Button resetBtn = new Button("Clear Form");
        resetBtn.getStyleClass().add("btn-secondary");
        resetBtn.setOnAction(e -> clearForm());

        Button submitBtn = new Button("🚨 Dispatch Emergency Alert to HQ ➔");
        submitBtn.getStyleClass().add("btn-danger");
        submitBtn.setStyle("-fx-padding: 10px 22px; -fx-font-size: 14px; -fx-font-weight: bold;");
        submitBtn.setOnAction(e -> handleDispatchEmergency());

        btnBar.getChildren().addAll(resetBtn, submitBtn);

        formCard.getChildren().addAll(formTitle, grid, btnBar);

        contentBox.getChildren().addAll(sevCard, natureCard, formCard);
        scrollPane.setContent(contentBox);

        root.getChildren().addAll(header, scrollPane);
    }

    private void addSeverityTile(FlowPane container, String emoji, String title, String subtitle, EmergencySeverity sev) {
        VBox tile = new VBox(4);
        tile.getStyleClass().add("category-tile");
        tile.setPrefWidth(200);

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(emoji);
        iconLbl.setStyle("-fx-font-size: 20px;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b;");

        topRow.getChildren().addAll(iconLbl, titleLbl);

        Label descLbl = new Label(subtitle);
        descLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        descLbl.setWrapText(true);

        tile.getChildren().addAll(topRow, descLbl);

        if (sev == selectedSeverity.get()) {
            tile.getStyleClass().add("category-tile-selected");
        }

        tile.setOnMouseClicked(e -> {
            for (VBox t : severityTiles) {
                t.getStyleClass().remove("category-tile-selected");
            }
            tile.getStyleClass().add("category-tile-selected");
            selectedSeverity.set(sev);
        });

        severityTiles.add(tile);
        container.getChildren().add(tile);
    }

    private void addIncidentChip(FlowPane container, String text) {
        Button chip = new Button(text);
        chip.getStyleClass().add("btn-secondary");
        chip.setStyle("-fx-font-size: 12px; -fx-padding: 6px 12px; -fx-background-radius: 20px;");

        chip.setOnAction(e -> {
            incidentType.set(text.substring(text.indexOf(" ") + 1));
            for (Button b : incidentChips) {
                b.setStyle("-fx-font-size: 12px; -fx-padding: 6px 12px; -fx-background-radius: 20px;");
            }
            chip.setStyle("-fx-font-size: 12px; -fx-padding: 6px 12px; -fx-background-radius: 20px; -fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold;");
        });

        incidentChips.add(chip);
        container.getChildren().add(chip);
    }

    private VBox createField(String labelText, Node control) {
        VBox box = new VBox(4);
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("form-label");
        box.getChildren().addAll(lbl, control);
        return box;
    }

    private void handleDispatchEmergency() {
        try {
            Junction j = junctionCombo.getValue();
            if (j == null) throw new ValidationException("Junction location is required.");

            Officer officer = (AuthService.getInstance().getCurrentUser() instanceof Officer o) ? o : null;
            Integer offId = officer != null ? officer.getOfficerId() : null;

            EmergencyReport rep = new EmergencyReport(
                    0, null, offId, j.getId(), incidentType.get().trim(),
                    selectedSeverity.get(), vehiclesSpinner.getValue(),
                    casualtiesSpinner.getValue(), ambulanceCheck.isSelected(),
                    towTruckCheck.isSelected(), roadBlockedCheck.isSelected(),
                    null, EmergencyStatus.REPORTED, notesArea.getText().trim()
            );

            EmergencyReport saved = emergencyService.reportEmergency(rep);
            UIComponents.showInfoAlert("Emergency Dispatched",
                    "Emergency Incident Report #" + saved.getReportNumber() + " broadcast to Control Room!\nAmbulance/Tow logistics dispatch has been notified.");
            clearForm();
        } catch (ValidationException ex) {
            UIComponents.showWarningAlert("Validation Error", ex.getMessage());
        } catch (Exception ex) {
            UIComponents.showErrorAlert("System Error", "Failed to dispatch emergency: " + ex.getMessage());
        }
    }

    private void clearForm() {
        notesArea.clear();
        ambulanceCheck.setSelected(false);
        towTruckCheck.setSelected(false);
        roadBlockedCheck.setSelected(false);
        vehiclesSpinner.getValueFactory().setValue(2);
        casualtiesSpinner.getValueFactory().setValue(0);
    }
}
