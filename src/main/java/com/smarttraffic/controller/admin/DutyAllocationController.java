package com.smarttraffic.controller.admin;

import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.exception.ValidationException;
import com.smarttraffic.model.DutyAssignment;
import com.smarttraffic.model.Junction;
import com.smarttraffic.model.Officer;
import com.smarttraffic.model.enums.DutyStatus;
import com.smarttraffic.model.enums.ShiftType;
import com.smarttraffic.service.DutyService;
import com.smarttraffic.service.JunctionService;
import com.smarttraffic.service.OfficerService;
import com.smarttraffic.util.DateUtil;
import com.smarttraffic.util.ThreadPoolManager;
import com.smarttraffic.view.UIComponents;
import com.smarttraffic.view.ViewNavigator;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Controller for Duty Allocation: Visual 3-Shift Board & Timetable.
 */
public class DutyAllocationController {
    private final VBox root = new VBox(15);
    private final DutyService dutyService = DutyService.getInstance();
    private final OfficerService officerService = OfficerService.getInstance();
    private final JunctionService junctionService = JunctionService.getInstance();

    private final ObservableList<DutyAssignment> masterList = FXCollections.observableArrayList();

    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final StackPane viewContainer = new StackPane();
    private final HBox shiftBoard = new HBox(16);
    private final TableView<DutyAssignment> table = new TableView<>();

    public DutyAllocationController() {
        buildUI();
        loadData();
    }

    public Node getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(10, 10, 30, 10));

        VBox header = UIComponents.createViewHeader(
                "Officer Shift Roster & Beat Allocation",
                "Visual shift scheduling board across Morning, Evening, and Night duty cycles"
        );

        // Date & Shift Navigator Bar
        HBox navBar = new HBox(12);
        navBar.setAlignment(Pos.CENTER_LEFT);
        navBar.getStyleClass().add("content-card");
        navBar.setPadding(new Insets(12, 16, 12, 16));

        Button prevDayBtn = new Button("◀ Previous Day");
        prevDayBtn.getStyleClass().add("btn-secondary");
        prevDayBtn.setOnAction(e -> {
            datePicker.setValue(datePicker.getValue().minusDays(1));
            renderShiftBoard();
        });

        Button todayBtn = new Button("Today");
        todayBtn.getStyleClass().add("btn-secondary");
        todayBtn.setOnAction(e -> {
            datePicker.setValue(LocalDate.now());
            renderShiftBoard();
        });

        Button nextDayBtn = new Button("Next Day ▶");
        nextDayBtn.getStyleClass().add("btn-secondary");
        nextDayBtn.setOnAction(e -> {
            datePicker.setValue(datePicker.getValue().plusDays(1));
            renderShiftBoard();
        });

        datePicker.setOnAction(e -> renderShiftBoard());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox viewToggle = UIComponents.createViewToggle("Shift Board", "Tabular View", isBoard -> {
            viewContainer.getChildren().clear();
            if (isBoard) {
                viewContainer.getChildren().add(new ScrollPane(shiftBoard));
            } else {
                viewContainer.getChildren().add(new VBox(table));
            }
        });

        Button scheduleBtn = new Button("➕ Allocate Shift");
        scheduleBtn.getStyleClass().add("btn-primary");
        scheduleBtn.setOnAction(e -> showScheduleDialog(null));

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> loadData());

        navBar.getChildren().addAll(prevDayBtn, datePicker, todayBtn, nextDayBtn, spacer, viewToggle, refreshBtn, scheduleBtn);

        // --- 1. Visual Shift Board (3 Columns) ---
        shiftBoard.setAlignment(Pos.TOP_LEFT);
        shiftBoard.setPadding(new Insets(6));
        shiftBoard.setStyle("-fx-background-color: transparent;");

        ScrollPane scrollBoard = new ScrollPane(shiftBoard);
        scrollBoard.setFitToWidth(true);
        scrollBoard.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        // --- 2. Tabular Fallback ---
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(520);

        TableColumn<DutyAssignment, String> colDate = new TableColumn<>("Duty Date");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(DateUtil.formatDate(d.getValue().getDutyDate())));

        TableColumn<DutyAssignment, String> colShift = new TableColumn<>("Shift");
        colShift.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getShift().getTitle()));

        TableColumn<DutyAssignment, String> colOfficer = new TableColumn<>("Officer In-Charge");
        colOfficer.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getOfficerName() + " [" + d.getValue().getBadgeNumber() + "]"
        ));

        TableColumn<DutyAssignment, String> colJunction = new TableColumn<>("Junction Deployed");
        colJunction.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getJunctionName()));

        TableColumn<DutyAssignment, String> colTime = new TableColumn<>("Timing");
        colTime.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getStartTime() + " - " + d.getValue().getEndTime()
        ));

        TableColumn<DutyAssignment, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().getLabel()));

        TableColumn<DutyAssignment, String> colNotes = new TableColumn<>("Operational Notes");
        colNotes.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNotes()));

        TableColumn<DutyAssignment, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(220);
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button compBtn = new Button("✓ Done");
            private final Button cancelBtn = new Button("✕ Cancel");
            private final Button delBtn = new Button("🗑️");
            private final HBox pane = new HBox(5, compBtn, cancelBtn, delBtn);
            {
                compBtn.getStyleClass().addAll("btn-success", "btn-small");
                cancelBtn.getStyleClass().addAll("btn-danger", "btn-small");
                delBtn.getStyleClass().addAll("btn-secondary", "btn-small");

                compBtn.setOnAction(e -> {
                    DutyAssignment d = getTableView().getItems().get(getIndex());
                    try {
                        dutyService.updateDutyStatus(d.getId(), DutyStatus.COMPLETED);
                        loadData();
                    } catch (Exception ex) {
                        UIComponents.showErrorAlert("Error", ex.getMessage());
                    }
                });

                cancelBtn.setOnAction(e -> {
                    DutyAssignment d = getTableView().getItems().get(getIndex());
                    try {
                        dutyService.updateDutyStatus(d.getId(), DutyStatus.CANCELLED);
                        loadData();
                    } catch (Exception ex) {
                        UIComponents.showErrorAlert("Error", ex.getMessage());
                    }
                });

                delBtn.setOnAction(e -> {
                    DutyAssignment d = getTableView().getItems().get(getIndex());
                    if (UIComponents.showConfirmDialog("Delete Shift", "Delete scheduled duty for " + d.getOfficerName() + "?")) {
                        try {
                            dutyService.deleteDuty(d.getId());
                            loadData();
                        } catch (Exception ex) {
                            UIComponents.showErrorAlert("Error", ex.getMessage());
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    DutyAssignment d = getTableView().getItems().get(getIndex());
                    boolean active = d.getStatus() == DutyStatus.SCHEDULED || d.getStatus() == DutyStatus.IN_PROGRESS;
                    compBtn.setDisable(!active);
                    cancelBtn.setDisable(!active);
                    setGraphic(pane);
                }
            }
        });

        table.getColumns().addAll(colDate, colShift, colOfficer, colJunction, colTime, colStatus, colNotes, colActions);
        table.setItems(masterList);

        viewContainer.getChildren().add(scrollBoard);

        root.getChildren().addAll(header, navBar, viewContainer);
    }

    private void renderShiftBoard() {
        shiftBoard.getChildren().clear();
        LocalDate targetDate = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();

        // 3 Shift Columns
        shiftBoard.getChildren().addAll(
                createShiftColumn("🌅 MORNING SHIFT", "06:00 AM - 02:00 PM", ShiftType.MORNING, targetDate),
                createShiftColumn("🌆 EVENING SHIFT", "02:00 PM - 10:00 PM", ShiftType.EVENING, targetDate),
                createShiftColumn("🌙 NIGHT SHIFT", "10:00 PM - 06:00 AM", ShiftType.NIGHT, targetDate)
        );
    }

    private VBox createShiftColumn(String shiftTitle, String timing, ShiftType shiftType, LocalDate date) {
        VBox col = new VBox(12);
        col.getStyleClass().add("shift-column");
        col.setPrefWidth(350);
        HBox.setHgrow(col, Priority.ALWAYS);

        // Column Header
        VBox colHeader = new VBox(2);
        Label titleLbl = new Label(shiftTitle);
        titleLbl.setStyle("-fx-font-weight: 900; -fx-font-size: 14px; -fx-text-fill: #0f172a;");

        Label timingLbl = new Label("⏱️ " + timing);
        timingLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        colHeader.getChildren().addAll(titleLbl, timingLbl);

        List<DutyAssignment> dutiesInShift = masterList.stream()
                .filter(d -> d.getDutyDate().equals(date) && d.getShift() == shiftType)
                .toList();

        Label countBadge = new Label(dutiesInShift.size() + " Officers Stationed");
        countBadge.setStyle("-fx-font-weight: 800; -fx-font-size: 11px; -fx-text-fill: #2563eb; -fx-background-color: #eff6ff; -fx-padding: 3px 8px; -fx-background-radius: 4px;");

        HBox topBar = new HBox(colHeader, countBadge);
        topBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(colHeader, Priority.ALWAYS);

        col.getChildren().add(topBar);

        // Duty Cards
        if (dutiesInShift.isEmpty()) {
            VBox emptySlot = new VBox(8);
            emptySlot.setAlignment(Pos.CENTER);
            emptySlot.setStyle("-fx-background-color: #ffffff; -fx-border-style: dashed; -fx-border-color: #cbd5e1; -fx-border-radius: 8px; -fx-padding: 24px 12px;");
            Label emptyLbl = new Label("No officers allocated for this shift.");
            emptyLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
            Button quickAdd = new Button("+ Allocate Beat");
            quickAdd.getStyleClass().addAll("btn-secondary", "btn-small");
            quickAdd.setOnAction(e -> showScheduleDialog(shiftType));
            emptySlot.getChildren().addAll(emptyLbl, quickAdd);
            col.getChildren().add(emptySlot);
        } else {
            for (DutyAssignment d : dutiesInShift) {
                col.getChildren().add(createDutyCard(d));
            }
        }

        return col;
    }

    private VBox createDutyCard(DutyAssignment d) {
        VBox card = new VBox(8);
        card.getStyleClass().add("shift-card");

        // Top Row: Officer Name & Status Pill
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLbl = new Label("👮 " + d.getOfficerName());
        nameLbl.setStyle("-fx-font-weight: 800; -fx-font-size: 13px; -fx-text-fill: #0f172a;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        boolean isDone = d.getStatus() == DutyStatus.COMPLETED;
        boolean isCancel = d.getStatus() == DutyStatus.CANCELLED;
        String badgeClass = isDone ? "badge-success" : isCancel ? "badge-danger" : "badge-info";
        Label statusLbl = UIComponents.createBadge(d.getStatus().getLabel(), badgeClass);

        topRow.getChildren().addAll(nameLbl, spacer, statusLbl);

        // Junction Pill
        HBox jRow = new HBox(6);
        jRow.setAlignment(Pos.CENTER_LEFT);
        jRow.setStyle("-fx-background-color: #f1f5f9; -fx-padding: 6px 10px; -fx-background-radius: 6px;");
        Label jIcon = new Label("📍");
        Label jText = new Label(d.getJunctionName());
        jText.setStyle("-fx-font-weight: 800; -fx-font-size: 12px; -fx-text-fill: #1e293b;");
        jRow.getChildren().addAll(jIcon, jText);

        // Notes
        if (d.getNotes() != null && !d.getNotes().isEmpty()) {
            Label notesLbl = new Label("📝 " + d.getNotes());
            notesLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
            notesLbl.setWrapText(true);
            card.getChildren().add(notesLbl);
        }

        // Action Buttons
        HBox actBar = new HBox(6);
        actBar.setAlignment(Pos.CENTER_RIGHT);

        Button compBtn = new Button("✓ Complete");
        compBtn.getStyleClass().addAll("btn-success", "btn-small");
        compBtn.setOnAction(e -> {
            try {
                dutyService.updateDutyStatus(d.getId(), DutyStatus.COMPLETED);
                loadData();
            } catch (Exception ex) {
                UIComponents.showErrorAlert("Error", ex.getMessage());
            }
        });

        Button cancelBtn = new Button("✕ Cancel");
        cancelBtn.getStyleClass().addAll("btn-danger", "btn-small");
        cancelBtn.setOnAction(e -> {
            try {
                dutyService.updateDutyStatus(d.getId(), DutyStatus.CANCELLED);
                loadData();
            } catch (Exception ex) {
                UIComponents.showErrorAlert("Error", ex.getMessage());
            }
        });

        Button delBtn = new Button("🗑️");
        delBtn.getStyleClass().addAll("btn-secondary", "btn-small");
        delBtn.setOnAction(e -> {
            if (UIComponents.showConfirmDialog("Delete Shift", "Delete scheduled duty for " + d.getOfficerName() + "?")) {
                try {
                    dutyService.deleteDuty(d.getId());
                    loadData();
                } catch (Exception ex) {
                    UIComponents.showErrorAlert("Error", ex.getMessage());
                }
            }
        });

        if (!isDone && !isCancel) {
            actBar.getChildren().addAll(compBtn, cancelBtn, delBtn);
        } else {
            actBar.getChildren().add(delBtn);
        }

        card.getChildren().addAll(topRow, jRow, actBar);
        return card;
    }

    private void loadData() {
        ThreadPoolManager.getInstance().execute(() -> {
            try {
                List<DutyAssignment> list = dutyService.getAllDuties();
                Platform.runLater(() -> {
                    masterList.setAll(list);
                    renderShiftBoard();
                });
            } catch (DatabaseException e) {
                Platform.runLater(() -> UIComponents.showErrorAlert("Database Error", "Failed to load duty assignments: " + e.getMessage()));
            }
        });
    }

    private void showScheduleDialog(ShiftType defaultShift) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(ViewNavigator.getPrimaryStage());
        dialog.setTitle("Schedule Officer Duty Assignment");

        VBox form = new VBox(12);
        form.setPadding(new Insets(20));
        form.setPrefWidth(480);

        Label title = new Label("Allocate Beat Duty Assignment");
        title.getStyleClass().add("section-title");

        ComboBox<Officer> officerCombo = new ComboBox<>();
        ComboBox<Junction> junctionCombo = new ComboBox<>();
        ComboBox<ShiftType> shiftCombo = new ComboBox<>();
        shiftCombo.getItems().addAll(ShiftType.values());
        shiftCombo.setValue(defaultShift != null ? defaultShift : ShiftType.MORNING);

        DatePicker dPicker = new DatePicker(datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now());
        TextField notesField = new TextField("Traffic flow regulation & enforcement");

        try {
            officerCombo.getItems().addAll(officerService.getAllOfficers());
            junctionCombo.getItems().addAll(junctionService.getAllJunctions());
            if (!officerCombo.getItems().isEmpty()) officerCombo.setValue(officerCombo.getItems().get(0));
            if (!junctionCombo.getItems().isEmpty()) junctionCombo.setValue(junctionCombo.getItems().get(0));
        } catch (Exception ignored) {
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        grid.add(new Label("Select Officer *"), 0, 0);
        grid.add(officerCombo, 1, 0);
        grid.add(new Label("Target Junction *"), 0, 1);
        grid.add(junctionCombo, 1, 1);
        grid.add(new Label("Shift Type *"), 0, 2);
        grid.add(shiftCombo, 1, 2);
        grid.add(new Label("Duty Date *"), 0, 3);
        grid.add(dPicker, 1, 3);
        grid.add(new Label("Operational Brief"), 0, 4);
        grid.add(notesField, 1, 4);

        HBox btnBar = new HBox(10);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button("Confirm Duty Schedule");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            try {
                Officer selectedOfficer = officerCombo.getValue();
                Junction selectedJunction = junctionCombo.getValue();
                ShiftType selectedShift = shiftCombo.getValue();
                LocalDate date = dPicker.getValue();

                if (selectedOfficer == null) throw new ValidationException("Officer must be selected.");
                if (selectedJunction == null) throw new ValidationException("Junction must be selected.");
                if (date == null) throw new ValidationException("Duty date must be selected.");

                LocalTime start = selectedShift == ShiftType.MORNING ? LocalTime.of(6, 0)
                        : selectedShift == ShiftType.EVENING ? LocalTime.of(14, 0) : LocalTime.of(22, 0);
                LocalTime end = selectedShift == ShiftType.MORNING ? LocalTime.of(14, 0)
                        : selectedShift == ShiftType.EVENING ? LocalTime.of(22, 0) : LocalTime.of(6, 0);

                DutyAssignment d = new DutyAssignment(0, selectedOfficer.getOfficerId(), selectedJunction.getId(),
                        selectedShift, date, start, end, DutyStatus.SCHEDULED, notesField.getText());

                dutyService.scheduleDuty(d);
                UIComponents.showInfoAlert("Duty Scheduled", "Duty allocation saved successfully.");
                dialog.close();
                loadData();
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
}
