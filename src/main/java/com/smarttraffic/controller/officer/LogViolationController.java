package com.smarttraffic.controller.officer;

import com.smarttraffic.exception.ValidationException;
import com.smarttraffic.model.Challan;
import com.smarttraffic.model.Junction;
import com.smarttraffic.model.Officer;
import com.smarttraffic.model.ViolationType;
import com.smarttraffic.model.enums.ChallanStatus;
import com.smarttraffic.model.enums.VehicleType;
import com.smarttraffic.service.AuthService;
import com.smarttraffic.service.ChallanService;
import com.smarttraffic.service.JunctionService;
import com.smarttraffic.view.ChallanReceiptDialog;
import com.smarttraffic.view.UIComponents;
import com.smarttraffic.view.ViewNavigator;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Recording Traffic Violations and Generating Digital e-Challans.
 * Features Visual Vehicle Selectors, One-Click Infraction Presets, and Live License Plate Previews.
 */
public class LogViolationController {
    private final VBox root = new VBox(15);
    private final ChallanService challanService = ChallanService.getInstance();
    private final JunctionService junctionService = JunctionService.getInstance();

    private final ObjectProperty<VehicleType> selectedVehicleType = new SimpleObjectProperty<>(VehicleType.FOUR_WHEELER_CAR);
    private final ObjectProperty<ViolationType> selectedViolationType = new SimpleObjectProperty<>();
    private final List<VBox> vehicleTypeTiles = new ArrayList<>();
    private final List<VBox> infractionTiles = new ArrayList<>();

    private final TextField vehicleNumField = new TextField();
    private final Label previewPlateText = new Label("KA-01-XX-0000");
    private final ComboBox<ViolationType> violationTypeCombo = new ComboBox<>();
    private final TextField fineField = new TextField();
    private final TextField violatorNameField = new TextField();
    private final TextField violatorPhoneField = new TextField();
    private final ComboBox<Junction> junctionCombo = new ComboBox<>();
    private final TextField evidencePathField = new TextField();
    private final TextArea remarksArea = new TextArea();

    public LogViolationController() {
        buildUI();
    }

    public Node getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(10, 10, 30, 10));

        VBox header = UIComponents.createViewHeader(
                "Digital e-Challan Issuance",
                "Log motor vehicle traffic infractions, calculate statutory fines, capture photographic evidence, and issue receipts"
        );

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        VBox contentBox = new VBox(20);
        contentBox.setMaxWidth(880);

        // Section 1: Vehicle Category Selector (Visual Tiles)
        VBox vehSection = new VBox(10);
        vehSection.getStyleClass().add("content-card");

        Label vehTitle = new Label("1. Vehicle Classification");
        vehTitle.getStyleClass().add("section-title");

        FlowPane vehTilesPane = new FlowPane();
        vehTilesPane.setHgap(12);
        vehTilesPane.setVgap(12);

        addVehicleTile(vehTilesPane, "🏍️", "Two-Wheeler", "Motorcycles, Scooters & Mopeds", VehicleType.TWO_WHEELER);
        addVehicleTile(vehTilesPane, "🛺", "Commercial Auto", "Auto-Rickshaws & Taxis", VehicleType.COMMERCIAL_AUTO);
        addVehicleTile(vehTilesPane, "🚗", "Four-Wheeler Car", "Sedans, SUVs, Hatchbacks", VehicleType.FOUR_WHEELER_CAR);
        addVehicleTile(vehTilesPane, "🚛", "Heavy Vehicle", "Buses, Freight Trucks & Tankers", VehicleType.HEAVY_VEHICLE);

        vehSection.getChildren().addAll(vehTitle, vehTilesPane);

        // Section 2: Quick Infraction Presets (Visual Tiles)
        VBox offenseSection = new VBox(10);
        offenseSection.getStyleClass().add("content-card");

        Label offenseTitle = new Label("2. Infraction Offense & Statutory Fine");
        offenseTitle.getStyleClass().add("section-title");

        Label offenseSubtitle = new Label("Select an offense card or use the dropdown selector below:");
        offenseSubtitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");

        FlowPane offenseTilesPane = new FlowPane();
        offenseTilesPane.setHgap(12);
        offenseTilesPane.setVgap(12);

        try {
            List<ViolationType> violations = challanService.getAllViolationTypes();
            violationTypeCombo.getItems().addAll(violations);

            for (ViolationType vt : violations) {
                String emoji = getOffenseEmoji(vt.getViolationCode());
                addInfractionTile(offenseTilesPane, emoji, vt);
            }

            if (!violations.isEmpty()) {
                selectedViolationType.set(violations.get(0));
                violationTypeCombo.setValue(violations.get(0));
                fineField.setText(String.format("%.2f", violations.get(0).getStandardFineAmount()));
            }

            List<Junction> junctions = junctionService.getAllJunctions();
            junctionCombo.getItems().addAll(junctions);

            Officer currentOfficer = (AuthService.getInstance().getCurrentUser() instanceof Officer o) ? o : null;
            if (currentOfficer != null && currentOfficer.getAssignedJunctionId() != null) {
                for (Junction j : junctions) {
                    if (j.getId() == currentOfficer.getAssignedJunctionId()) {
                        junctionCombo.setValue(j);
                        break;
                    }
                }
            } else if (!junctions.isEmpty()) {
                junctionCombo.setValue(junctions.get(0));
            }
        } catch (Exception ignored) {
        }

        // Dropdown fallback / fine override
        HBox fallbackRow = new HBox(12);
        fallbackRow.setAlignment(Pos.CENTER_LEFT);
        fallbackRow.setStyle("-fx-background-color: #f8fafc; -fx-padding: 10px 14px; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-radius: 8px;");

        Label comboLbl = new Label("Selected Code:");
        comboLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");

        violationTypeCombo.setPrefWidth(320);
        violationTypeCombo.setOnAction(e -> {
            ViolationType selected = violationTypeCombo.getValue();
            if (selected != null) {
                selectedViolationType.set(selected);
                fineField.setText(String.format("%.2f", selected.getStandardFineAmount()));
                highlightInfractionTile(selected);
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label finePrompt = new Label("Statutory Fine (₹):");
        finePrompt.setStyle("-fx-font-weight: bold; -fx-text-fill: #b91c1c;");
        fineField.setPrefWidth(120);

        fallbackRow.getChildren().addAll(comboLbl, violationTypeCombo, spacer, finePrompt, fineField);

        offenseSection.getChildren().addAll(offenseTitle, offenseSubtitle, offenseTilesPane, fallbackRow);

        // Section 3: Violator Particulars & Evidence
        VBox formCard = new VBox(16);
        formCard.getStyleClass().add("content-card");

        Label formTitle = new Label("3. Vehicle Particulars & Location");
        formTitle.getStyleClass().add("section-title");

        // Live License Plate Graphic
        HBox platePreviewBox = new HBox(14);
        platePreviewBox.setAlignment(Pos.CENTER_LEFT);
        platePreviewBox.setStyle("-fx-background-color: #f1f5f9; -fx-padding: 10px 16px; -fx-background-radius: 8px;");

        Label platePrompt = new Label("Live Citation Plate:");
        platePrompt.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");

        HBox plateGraphic = new HBox(6);
        plateGraphic.getStyleClass().add("license-plate");
        Label ind = new Label("IND");
        ind.getStyleClass().add("license-plate-ind");
        previewPlateText.getStyleClass().add("license-plate-text");
        plateGraphic.getChildren().addAll(ind, previewPlateText);

        platePreviewBox.getChildren().addAll(platePrompt, plateGraphic);

        vehicleNumField.setPromptText("e.g. KA-01-AB-1234");
        vehicleNumField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                String upper = newV.toUpperCase();
                vehicleNumField.setText(upper);
                previewPlateText.setText(upper.isEmpty() ? "KA-01-XX-0000" : upper);
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);

        grid.add(createField("Vehicle Registration Number *", vehicleNumField), 0, 0);
        grid.add(createField("Enforcement Junction Location *", junctionCombo), 1, 0);

        grid.add(createField("Violator / Driver Name", violatorNameField), 0, 1);
        grid.add(createField("Contact Mobile Phone", violatorPhoneField), 1, 1);

        // Evidence Picker
        HBox evidenceBox = new HBox(8);
        evidencePathField.setPromptText("Photo evidence file path");
        evidencePathField.setPrefWidth(260);
        Button browseBtn = new Button("📁 Browse");
        browseBtn.getStyleClass().add("btn-secondary");
        browseBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Photographic Evidence");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files (*.jpg, *.png)", "*.jpg", "*.png", "*.jpeg")
            );
            File f = fc.showOpenDialog(ViewNavigator.getPrimaryStage());
            if (f != null) {
                evidencePathField.setText(f.getAbsolutePath());
            }
        });

        Button sampleEvidenceBtn = new Button("📷 Sample Photo");
        sampleEvidenceBtn.getStyleClass().add("btn-secondary");
        sampleEvidenceBtn.setOnAction(e -> {
            evidencePathField.setText("camera_capture_junc_" + (System.currentTimeMillis() % 1000) + ".jpg");
        });

        evidenceBox.getChildren().addAll(evidencePathField, browseBtn, sampleEvidenceBtn);
        grid.add(createField("Photographic Evidence", evidenceBox), 0, 2, 2, 1);

        remarksArea.setPrefRowCount(3);
        remarksArea.setPromptText("Enter officer field notes, radar speed reading, breathalyzer BAC, or special remarks");
        grid.add(createField("Officer Enforcement Notes", remarksArea), 0, 3, 2, 1);

        // Action Buttons
        HBox btnBar = new HBox(12);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(10, 0, 0, 0));

        Button resetBtn = new Button("Clear Form");
        resetBtn.getStyleClass().add("btn-secondary");
        resetBtn.setOnAction(e -> clearForm());

        Button submitBtn = new Button("⚡ Issue e-Challan & Print Slip ➔");
        submitBtn.getStyleClass().add("btn-primary");
        submitBtn.setStyle("-fx-padding: 10px 22px; -fx-font-size: 14px; -fx-font-weight: bold;");
        submitBtn.setOnAction(e -> handleIssueChallan());

        btnBar.getChildren().addAll(resetBtn, submitBtn);

        formCard.getChildren().addAll(formTitle, platePreviewBox, grid, btnBar);

        contentBox.getChildren().addAll(vehSection, offenseSection, formCard);
        scrollPane.setContent(contentBox);

        root.getChildren().addAll(header, scrollPane);
    }

    private void addVehicleTile(FlowPane container, String emoji, String title, String subtitle, VehicleType type) {
        VBox tile = new VBox(4);
        tile.getStyleClass().add("category-tile");
        tile.setPrefWidth(200);

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(emoji);
        iconLbl.setStyle("-fx-font-size: 22px;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b;");

        topRow.getChildren().addAll(iconLbl, titleLbl);

        Label descLbl = new Label(subtitle);
        descLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        descLbl.setWrapText(true);

        tile.getChildren().addAll(topRow, descLbl);

        if (type == selectedVehicleType.get()) {
            tile.getStyleClass().add("category-tile-selected");
        }

        tile.setOnMouseClicked(e -> {
            for (VBox t : vehicleTypeTiles) {
                t.getStyleClass().remove("category-tile-selected");
            }
            tile.getStyleClass().add("category-tile-selected");
            selectedVehicleType.set(type);
        });

        vehicleTypeTiles.add(tile);
        container.getChildren().add(tile);
    }

    private void addInfractionTile(FlowPane container, String emoji, ViolationType vt) {
        VBox tile = new VBox(4);
        tile.getStyleClass().add("category-tile");
        tile.setPrefWidth(260);

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(emoji);
        iconLbl.setStyle("-fx-font-size: 20px;");

        Label codeLbl = new Label(vt.getViolationCode());
        codeLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #2563eb;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label fineBadge = new Label(String.format("₹ %,.0f", vt.getStandardFineAmount()));
        fineBadge.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #dc2626; -fx-background-color: #fee2e2; -fx-padding: 2px 6px; -fx-background-radius: 4px;");

        topRow.getChildren().addAll(iconLbl, codeLbl, spacer, fineBadge);

        Label titleLbl = new Label(vt.getTitle());
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #0f172a;");
        titleLbl.setWrapText(true);

        tile.getChildren().addAll(topRow, titleLbl);
        tile.setUserData(vt);

        if (selectedViolationType.get() != null && selectedViolationType.get().getId() == vt.getId()) {
            tile.getStyleClass().add("category-tile-selected");
        }

        tile.setOnMouseClicked(e -> {
            for (VBox t : infractionTiles) {
                t.getStyleClass().remove("category-tile-selected");
            }
            tile.getStyleClass().add("category-tile-selected");
            selectedViolationType.set(vt);
            violationTypeCombo.setValue(vt);
            fineField.setText(String.format("%.2f", vt.getStandardFineAmount()));
        });

        infractionTiles.add(tile);
        container.getChildren().add(tile);
    }

    private void highlightInfractionTile(ViolationType vt) {
        for (VBox t : infractionTiles) {
            t.getStyleClass().remove("category-tile-selected");
            if (t.getUserData() instanceof ViolationType current && current.getId() == vt.getId()) {
                t.getStyleClass().add("category-tile-selected");
            }
        }
    }

    private String getOffenseEmoji(String code) {
        if (code == null) return "⚠️";
        if (code.contains("SIG")) return "🚦";
        if (code.contains("HELM")) return "🪖";
        if (code.contains("SPD")) return "🏎️";
        if (code.contains("PHN")) return "📱";
        if (code.contains("SEAT")) return "💺";
        if (code.contains("DRK")) return "🍷";
        if (code.contains("PRK")) return "🛑";
        if (code.contains("TRIP")) return "👥";
        return "⚠️";
    }

    private VBox createField(String labelText, Node control) {
        VBox box = new VBox(4);
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("form-label");
        box.getChildren().addAll(lbl, control);
        return box;
    }

    private void handleIssueChallan() {
        try {
            Officer officer = (AuthService.getInstance().getCurrentUser() instanceof Officer o) ? o : null;
            int officerId = officer != null ? officer.getOfficerId() : 1;

            String vehNo = vehicleNumField.getText();
            VehicleType vType = selectedVehicleType.get();
            ViolationType vTypeObj = selectedViolationType.get();
            Junction junction = junctionCombo.getValue();

            if (vehNo == null || vehNo.trim().isEmpty()) {
                throw new ValidationException("Vehicle Registration Number is required.");
            }
            if (vTypeObj == null) {
                throw new ValidationException("Violation type must be selected.");
            }
            if (junction == null) {
                throw new ValidationException("Junction must be selected.");
            }

            double fine = Double.parseDouble(fineField.getText().trim());

            Challan challan = new Challan(
                    0, null, vehNo.trim().toUpperCase(), vType,
                    violatorNameField.getText().trim(),
                    violatorPhoneField.getText().trim(),
                    officerId,
                    junction.getId(),
                    vTypeObj.getId(),
                    LocalDateTime.now(),
                    fine,
                    ChallanStatus.PENDING,
                    null, null,
                    evidencePathField.getText().trim(),
                    remarksArea.getText().trim()
            );

            Challan saved = challanService.issueChallan(challan);

            UIComponents.showInfoAlert("e-Challan Issued",
                    "Traffic Violation Notice #" + saved.getChallanNumber() + " successfully generated!\nVehicle: " + saved.getVehicleNumber());

            // Pop up the official printable receipt
            ChallanReceiptDialog.show(saved, ViewNavigator.getPrimaryStage());

            clearForm();

        } catch (NumberFormatException ex) {
            UIComponents.showWarningAlert("Input Error", "Fine amount must be a valid number.");
        } catch (ValidationException ex) {
            UIComponents.showWarningAlert("Validation Error", ex.getMessage());
        } catch (Exception ex) {
            UIComponents.showErrorAlert("System Error", "Could not issue challan: " + ex.getMessage());
        }
    }

    private void clearForm() {
        vehicleNumField.clear();
        previewPlateText.setText("KA-01-XX-0000");
        violatorNameField.clear();
        violatorPhoneField.clear();
        evidencePathField.clear();
        remarksArea.clear();
    }
}
