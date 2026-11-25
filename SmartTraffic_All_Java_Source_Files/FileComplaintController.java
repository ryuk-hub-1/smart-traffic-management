package com.smarttraffic.controller.citizen;

import com.smarttraffic.exception.ValidationException;
import com.smarttraffic.model.Citizen;
import com.smarttraffic.model.Complaint;
import com.smarttraffic.model.Junction;
import com.smarttraffic.model.enums.ComplaintStatus;
import com.smarttraffic.service.AuthService;
import com.smarttraffic.service.ComplaintService;
import com.smarttraffic.service.JunctionService;
import com.smarttraffic.view.UIComponents;
import com.smarttraffic.view.ViewNavigator;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
 * Controller for Citizens filing traffic complaints and safety reports.
 * Features modern interactive visual category tiles instead of flat dropdown lists.
 */
public class FileComplaintController {
    private final VBox root = new VBox(15);
    private final ComplaintService complaintService = ComplaintService.getInstance();
    private final JunctionService junctionService = JunctionService.getInstance();

    private final StringProperty selectedCategory = new SimpleStringProperty("Illegal Parking / Footpath Obstruction");
    private final List<VBox> categoryTiles = new ArrayList<>();

    private final TextField vehicleNumField = new TextField();
    private final ComboBox<Junction> junctionCombo = new ComboBox<>();
    private final TextField evidencePathField = new TextField();
    private final TextArea descriptionArea = new TextArea();

    public FileComplaintController() {
        buildUI();
    }

    public Node getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(10, 10, 30, 10));

        VBox header = UIComponents.createViewHeader(
                "Lodge Citizen Traffic Grievance",
                "Report illegal parking obstruction, malfunctioning traffic signals, reckless driving, or road hazards"
        );

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        VBox contentBox = new VBox(20);
        contentBox.setMaxWidth(880);

        // Card 1: Interactive Category Selection
        VBox catCard = new VBox(12);
        catCard.getStyleClass().add("content-card");

        Label catSectionTitle = new Label("1. Select Infraction or Hazard Category");
        catSectionTitle.getStyleClass().add("section-title");

        Label catSectionDesc = new Label("Select the visual card that best matches the issue you encountered:");
        catSectionDesc.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");

        FlowPane tilesPane = new FlowPane();
        tilesPane.setHgap(12);
        tilesPane.setVgap(12);

        // Define Category options
        addCategoryTile(tilesPane, "🚗", "Illegal Parking", "Illegal Parking / Footpath Obstruction", "Blocking footpaths, driveways, bus bays, or no-parking zones");
        addCategoryTile(tilesPane, "🚦", "Signal Malfunction", "Broken Traffic Signal / Relay Failure", "Traffic lights dark, timer stuck, or conflicting signals");
        addCategoryTile(tilesPane, "🏎️", "Reckless Driving", "Dangerous / Rash Driving", "Overspeeding, wrong-way driving, drifting, or red-light jumping");
        addCategoryTile(tilesPane, "🚧", "Road Obstruction", "Road Obstruction / Construction Hazard", "Unmarked construction pits, spilled gravel, or abandoned wrecks");
        addCategoryTile(tilesPane, "🔊", "Illegal Exhaust / Horn", "Noise Pollution / Illegal Modified Silencer", "Loud custom exhausts, excessive decibel horns, strobe flashers");
        addCategoryTile(tilesPane, "🚸", "Pedestrian Hazard", "Pedestrian Safety & Crosswalk Infraction", "Failing to yield at zebra crossings or encroaching sidewalks");

        HBox currentChoiceBar = new HBox(8);
        currentChoiceBar.setAlignment(Pos.CENTER_LEFT);
        currentChoiceBar.setStyle("-fx-background-color: #f1f5f9; -fx-padding: 8px 14px; -fx-background-radius: 8px;");
        Label choicePrompt = new Label("Selected Category:");
        choicePrompt.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
        Label choiceVal = new Label();
        choiceVal.textProperty().bind(selectedCategory);
        choiceVal.setStyle("-fx-font-weight: bold; -fx-text-fill: #2563eb;");
        currentChoiceBar.getChildren().addAll(choicePrompt, choiceVal);

        catCard.getChildren().addAll(catSectionTitle, catSectionDesc, tilesPane, currentChoiceBar);

        // Card 2: Incident Particulars & Evidence
        VBox formCard = new VBox(16);
        formCard.getStyleClass().add("content-card");

        Label formTitle = new Label("2. Incident Particulars & Photographic Evidence");
        formTitle.getStyleClass().add("section-title");

        try {
            List<Junction> junctions = junctionService.getAllJunctions();
            junctionCombo.getItems().addAll(junctions);
            if (!junctions.isEmpty()) junctionCombo.setValue(junctions.get(0));
        } catch (Exception ignored) {
        }

        vehicleNumField.setPromptText("Optional: e.g. KA-04-XX-1122 or DL-08-CC-3344");
        vehicleNumField.textProperty().addListener((obs, o, n) -> {
            if (n != null) vehicleNumField.setText(n.toUpperCase());
        });

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);

        grid.add(createField("Incident Junction Location *", junctionCombo), 0, 0);
        grid.add(createField("Offending Vehicle Number (If Applicable)", vehicleNumField), 1, 0);

        // Evidence Picker
        HBox evidenceBox = new HBox(8);
        evidencePathField.setPromptText("Select photo / screenshot file");
        evidencePathField.setPrefWidth(260);
        Button browseBtn = new Button("📁 Browse File");
        browseBtn.getStyleClass().add("btn-secondary");
        browseBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Incident Photographic Evidence");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files (*.jpg, *.png)", "*.jpg", "*.png", "*.jpeg")
            );
            File f = fc.showOpenDialog(ViewNavigator.getPrimaryStage());
            if (f != null) {
                evidencePathField.setText(f.getAbsolutePath());
            }
        });

        Button sampleBtn = new Button("📷 Sample Photo");
        sampleBtn.getStyleClass().add("btn-secondary");
        sampleBtn.setOnAction(e -> evidencePathField.setText("citizen_report_snap_" + (System.currentTimeMillis() % 1000) + ".jpg"));

        evidenceBox.getChildren().addAll(evidencePathField, browseBtn, sampleBtn);
        grid.add(createField("Photo Proof / Incident Evidence", evidenceBox), 0, 1, 2, 1);

        descriptionArea.setPrefRowCount(4);
        descriptionArea.setPromptText("Provide comprehensive description of the incident, exact landmark, date/time, and safety hazard caused...");
        grid.add(createField("Detailed Description of Infraction *", descriptionArea), 0, 2, 2, 1);

        HBox btnBar = new HBox(12);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(10, 0, 0, 0));

        Button resetBtn = new Button("Clear Form");
        resetBtn.getStyleClass().add("btn-secondary");
        resetBtn.setOnAction(e -> clearForm());

        Button submitBtn = new Button("Submit Grievance to Police ➔");
        submitBtn.getStyleClass().add("btn-primary");
        submitBtn.setStyle("-fx-padding: 10px 22px; -fx-font-size: 14px; -fx-font-weight: bold;");
        submitBtn.setOnAction(e -> handleSubmitComplaint());

        btnBar.getChildren().addAll(resetBtn, submitBtn);

        formCard.getChildren().addAll(formTitle, grid, btnBar);

        contentBox.getChildren().addAll(catCard, formCard);
        scrollPane.setContent(contentBox);

        root.getChildren().addAll(header, scrollPane);
    }

    private void addCategoryTile(FlowPane container, String emoji, String shortTitle, String fullCategory, String description) {
        VBox tile = new VBox(4);
        tile.getStyleClass().add("category-tile");
        tile.setPrefWidth(260);

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(emoji);
        iconLbl.setStyle("-fx-font-size: 22px;");

        Label titleLbl = new Label(shortTitle);
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b;");

        topRow.getChildren().addAll(iconLbl, titleLbl);

        Label descLbl = new Label(description);
        descLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        descLbl.setWrapText(true);

        tile.getChildren().addAll(topRow, descLbl);

        // Pre-select if matches default
        if (fullCategory.equals(selectedCategory.get())) {
            tile.getStyleClass().add("category-tile-selected");
        }

        tile.setOnMouseClicked(e -> {
            for (VBox t : categoryTiles) {
                t.getStyleClass().remove("category-tile-selected");
            }
            tile.getStyleClass().add("category-tile-selected");
            selectedCategory.set(fullCategory);
        });

        categoryTiles.add(tile);
        container.getChildren().add(tile);
    }

    private VBox createField(String labelText, Node control) {
        VBox box = new VBox(4);
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("form-label");
        box.getChildren().addAll(lbl, control);
        return box;
    }

    private void handleSubmitComplaint() {
        try {
            Citizen citizen = (AuthService.getInstance().getCurrentUser() instanceof Citizen c) ? c : null;
            if (citizen == null) throw new ValidationException("Citizen session expired. Please log in again.");

            String desc = descriptionArea.getText();
            if (desc == null || desc.trim().length() < 10) {
                throw new ValidationException("Please provide a detailed description (at least 10 characters).");
            }

            Junction junction = junctionCombo.getValue();
            if (junction == null) {
                throw new ValidationException("Please select the incident junction location.");
            }

            Complaint complaint = new Complaint();
            complaint.setCitizenId(citizen.getId());
            complaint.setComplaintType(selectedCategory.get());
            complaint.setDescription(desc.trim());
            complaint.setIncidentDate(LocalDateTime.now());
            complaint.setJunctionId(junction.getId());
            complaint.setStatus(ComplaintStatus.PENDING);

            String veh = vehicleNumField.getText();
            if (veh != null && !veh.trim().isEmpty()) {
                complaint.setVehicleNumber(veh.trim().toUpperCase());
            }

            String ev = evidencePathField.getText();
            if (ev != null && !ev.trim().isEmpty()) {
                complaint.setEvidenceFilePath(ev.trim());
            }

            Complaint created = complaintService.fileComplaint(complaint);
            UIComponents.showInfoAlert("Grievance Submitted Successfully",
                    "Your complaint has been registered with tracking ID:\n" + created.getComplaintNumber() +
                            "\n\nThe Traffic Police dispatch team will investigate and take enforcement action.");

            clearForm();
            // Direct to tracking history
            ViewNavigator.loadMainContent(new CitizenComplaintsHistoryController().getView(), "History");

        } catch (ValidationException e) {
            UIComponents.showWarningAlert("Form Incomplete", e.getMessage());
        } catch (Exception e) {
            UIComponents.showErrorAlert("Submission Failed", "Could not submit complaint: " + e.getMessage());
        }
    }

    private void clearForm() {
        vehicleNumField.clear();
        evidencePathField.clear();
        descriptionArea.clear();
        if (!categoryTiles.isEmpty()) {
            for (VBox t : categoryTiles) t.getStyleClass().remove("category-tile-selected");
            categoryTiles.get(0).getStyleClass().add("category-tile-selected");
            selectedCategory.set("Illegal Parking / Footpath Obstruction");
        }
    }
}
