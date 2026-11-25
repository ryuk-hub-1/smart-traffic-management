package com.smarttraffic.controller.admin;

import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.model.Attendance;
import com.smarttraffic.model.Officer;
import com.smarttraffic.model.enums.AttendanceStatus;
import com.smarttraffic.service.DutyService;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Controller for Admin Attendance Monitoring.
 * Features Visual Attendance Cards with Rank Avatars, Shift Telemetry, and Tabular Toggle.
 */
public class AttendanceMonitorController {
    private final VBox root = new VBox(14);
    private final DutyService dutyService = DutyService.getInstance();
    private final OfficerService officerService = OfficerService.getInstance();

    private final ObservableList<Attendance> masterList = FXCollections.observableArrayList();
    private FilteredList<Attendance> filteredList;

    private final FlowPane cardsContainer = new FlowPane();
    private final TableView<Attendance> table = new TableView<>();
    private final StackPane viewContainer = new StackPane();

    private final TextField searchField = UIComponents.createSearchField("Search officer name, badge, remarks...");
    private final DatePicker dateFilter = new DatePicker();
    private final HBox statsBanner = new HBox(12);

    public AttendanceMonitorController() {
        buildUI();
        loadData();
    }

    public Node getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(10, 10, 30, 10));

        VBox header = UIComponents.createViewHeader(
                "Officer Attendance & Shift Muster Roll",
                "Audit real-time daily shift check-ins, departures, punctuality logs, and active on-duty personnel status"
        );

        // Stats Banner
        statsBanner.setAlignment(Pos.CENTER_LEFT);
        updateStatsBanner();

        // Control Bar
        HBox controlBar = new HBox(12);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        dateFilter.setPromptText("Filter by Date");

        Button clearDateBtn = new Button("Clear Date");
        clearDateBtn.getStyleClass().add("btn-secondary");
        clearDateBtn.setOnAction(e -> dateFilter.setValue(null));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox viewToggle = UIComponents.createViewToggle("Attendance Cards", "Muster Table", isCardView -> {
            if (isCardView) {
                showCardsView();
            } else {
                showTableView();
            }
        });

        Button manualCheckInBtn = new Button("➕ Record Attendance");
        manualCheckInBtn.getStyleClass().add("btn-primary");
        manualCheckInBtn.setOnAction(e -> showManualAttendanceDialog());

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> loadData());

        controlBar.getChildren().addAll(searchField, dateFilter, clearDateBtn, spacer, viewToggle, refreshBtn, manualCheckInBtn);

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

        // Filters
        filteredList = new FilteredList<>(masterList, p -> true);

        Runnable applyFilters = () -> {
            String query = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
            LocalDate selectedDate = dateFilter.getValue();

            filteredList.setPredicate(a -> {
                boolean matchesDate = (selectedDate == null) || a.getAttendanceDate().equals(selectedDate);
                boolean matchesText = query.isEmpty() ||
                        a.getOfficerName().toLowerCase().contains(query) ||
                        a.getBadgeNumber().toLowerCase().contains(query) ||
                        (a.getRemarks() != null && a.getRemarks().toLowerCase().contains(query));
                return matchesDate && matchesText;
            });

            renderCards();
        };

        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters.run());
        dateFilter.valueProperty().addListener((obs, oldV, newV) -> applyFilters.run());

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

        TableColumn<Attendance, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(DateUtil.formatDate(d.getValue().getAttendanceDate())));
        colDate.setPrefWidth(110);

        TableColumn<Attendance, String> colOfficer = new TableColumn<>("Officer Name");
        colOfficer.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getOfficerName()));

        TableColumn<Attendance, String> colBadge = new TableColumn<>("Badge ID");
        colBadge.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBadgeNumber()));
        colBadge.setPrefWidth(100);

        TableColumn<Attendance, String> colIn = new TableColumn<>("Check-In Time");
        colIn.setCellValueFactory(d -> new SimpleStringProperty(DateUtil.formatTime(d.getValue().getCheckInTime())));

        TableColumn<Attendance, String> colOut = new TableColumn<>("Check-Out Time");
        colOut.setCellValueFactory(d -> new SimpleStringProperty(DateUtil.formatTime(d.getValue().getCheckOutTime())));

        TableColumn<Attendance, String> colStatus = new TableColumn<>("Attendance Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().getLabel()));

        TableColumn<Attendance, String> colRemarks = new TableColumn<>("Remarks");
        colRemarks.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRemarks()));

        TableColumn<Attendance, Void> colAction = new TableColumn<>("Action");
        colAction.setPrefWidth(100);
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button delBtn = new Button("🗑️ Delete");
            {
                delBtn.getStyleClass().addAll("btn-danger", "btn-small");
                delBtn.setOnAction(e -> {
                    Attendance a = getTableView().getItems().get(getIndex());
                    handleDeleteAttendance(a);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : delBtn);
            }
        });

        table.getColumns().addAll(colDate, colOfficer, colBadge, colIn, colOut, colStatus, colRemarks, colAction);
    }

    private void renderCards() {
        cardsContainer.getChildren().clear();

        if (filteredList.isEmpty()) {
            VBox emptyBox = new VBox(10);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40));
            Label emptyLbl = new Label("No attendance records matching query");
            emptyLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");
            emptyBox.getChildren().add(emptyLbl);
            cardsContainer.getChildren().add(emptyBox);
            return;
        }

        for (Attendance a : filteredList) {
            VBox card = new VBox(10);
            card.getStyleClass().add("officer-card");
            card.setPrefWidth(300);

            // Header: Avatar + Officer Name & Badge
            HBox topRow = new HBox(10);
            topRow.setAlignment(Pos.CENTER_LEFT);

            StackPane avatar = UIComponents.createRankAvatar("OFFICER");

            VBox nameBox = new VBox(2);
            Label nameLbl = new Label(a.getOfficerName());
            nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0f172a;");
            Label badgeLbl = new Label("Badge #" + a.getBadgeNumber());
            badgeLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
            nameBox.getChildren().addAll(nameLbl, badgeLbl);

            Region topSpacer = new Region();
            HBox.setHgrow(topSpacer, Priority.ALWAYS);

            Label statusBadge = new Label(a.getStatus().getLabel());
            switch (a.getStatus()) {
                case PRESENT -> statusBadge.getStyleClass().addAll("badge", "badge-success");
                case ON_DUTY -> statusBadge.getStyleClass().addAll("badge", "badge-primary");
                case LATE -> statusBadge.getStyleClass().addAll("badge", "badge-warning");
                case ABSENT, ON_LEAVE -> statusBadge.getStyleClass().addAll("badge", "badge-danger");
            }

            topRow.getChildren().addAll(avatar, nameBox, topSpacer, statusBadge);

            // Date
            HBox dateRow = new HBox(6);
            dateRow.setAlignment(Pos.CENTER_LEFT);
            Label dateLbl = new Label("📅 " + DateUtil.formatDate(a.getAttendanceDate()));
            dateLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #334155;");
            dateRow.getChildren().add(dateLbl);

            // Times Box
            HBox timesBox = new HBox(12);
            timesBox.setStyle("-fx-background-color: #f8fafc; -fx-padding: 8px 12px; -fx-background-radius: 6px; -fx-border-color: #e2e8f0; -fx-border-radius: 6px;");

            VBox inBox = new VBox(2);
            Label inTitle = new Label("Check-In");
            inTitle.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
            Label inVal = new Label(DateUtil.formatTime(a.getCheckInTime()));
            inVal.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #16a34a;");
            inBox.getChildren().addAll(inTitle, inVal);

            Region timeSpacer = new Region();
            HBox.setHgrow(timeSpacer, Priority.ALWAYS);

            VBox outBox = new VBox(2);
            Label outTitle = new Label("Check-Out");
            outTitle.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
            Label outVal = new Label(DateUtil.formatTime(a.getCheckOutTime()));
            outVal.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");
            outBox.getChildren().addAll(outTitle, outVal);

            timesBox.getChildren().addAll(inBox, timeSpacer, outBox);

            // Remarks
            VBox remBox = new VBox(2);
            if (a.getRemarks() != null && !a.getRemarks().trim().isEmpty()) {
                Label remTitle = new Label("Remarks / Verification:");
                remTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
                Label remVal = new Label(a.getRemarks());
                remVal.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
                remVal.setWrapText(true);
                remBox.getChildren().addAll(remTitle, remVal);
            }

            HBox actRow = new HBox(8);
            actRow.setAlignment(Pos.CENTER_RIGHT);
            Button delBtn = new Button("🗑️ Delete");
            delBtn.getStyleClass().addAll("btn-danger", "btn-small");
            delBtn.setOnAction(e -> handleDeleteAttendance(a));
            actRow.getChildren().add(delBtn);

            card.getChildren().addAll(topRow, dateRow, timesBox, remBox, actRow);
            cardsContainer.getChildren().add(card);
        }
    }

    private void updateStatsBanner() {
        statsBanner.getChildren().clear();
        int total = masterList.size();
        long present = masterList.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.ON_DUTY).count();
        long late = masterList.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
        long onLeave = masterList.stream().filter(a -> a.getStatus() == AttendanceStatus.ON_LEAVE || a.getStatus() == AttendanceStatus.ABSENT).count();

        statsBanner.getChildren().addAll(
                UIComponents.createStatChip("📋", "TOTAL LOGS", String.valueOf(total), "#ffffff"),
                UIComponents.createStatChip("🟢", "ON-DUTY / PRESENT", String.valueOf(present), "#f0fdf4"),
                UIComponents.createStatChip("🟡", "LATE ARRIVALS", String.valueOf(late), "#fffbeb"),
                UIComponents.createStatChip("🔴", "ABSENT / ON LEAVE", String.valueOf(onLeave), "#fef2f2")
        );
    }

    private void loadData() {
        ThreadPoolManager.getInstance().execute(() -> {
            try {
                List<Attendance> list = dutyService.getAllAttendance();
                Platform.runLater(() -> {
                    masterList.setAll(list);
                    updateStatsBanner();
                    renderCards();
                });
            } catch (DatabaseException e) {
                Platform.runLater(() -> UIComponents.showErrorAlert("Database Error", "Failed to load attendance logs: " + e.getMessage()));
            }
        });
    }

    private void showManualAttendanceDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(ViewNavigator.getPrimaryStage());
        dialog.setTitle("Administrative Shift Attendance Entry");

        VBox form = new VBox(15);
        form.setPadding(new Insets(20));
        form.setPrefWidth(420);

        Label title = new Label("Log Officer Shift Attendance");
        title.getStyleClass().add("section-title");

        ComboBox<Officer> officerCombo = new ComboBox<>();
        try {
            officerCombo.getItems().addAll(officerService.getAllOfficers());
            if (!officerCombo.getItems().isEmpty()) officerCombo.setValue(officerCombo.getItems().get(0));
        } catch (Exception ignored) {
        }

        DatePicker datePicker = new DatePicker(LocalDate.now());

        ComboBox<AttendanceStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(AttendanceStatus.values());
        statusCombo.setValue(AttendanceStatus.PRESENT);

        TextField checkInField = new TextField("08:00:00");
        TextField checkOutField = new TextField("16:00:00");
        TextField remarksField = new TextField("Verified on-post attendance");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Traffic Officer *"), 0, 0);
        grid.add(officerCombo, 1, 0);
        grid.add(new Label("Attendance Date *"), 0, 1);
        grid.add(datePicker, 1, 1);
        grid.add(new Label("Status *"), 0, 2);
        grid.add(statusCombo, 1, 2);
        grid.add(new Label("Check-In Time"), 0, 3);
        grid.add(checkInField, 1, 3);
        grid.add(new Label("Check-Out Time"), 0, 4);
        grid.add(checkOutField, 1, 4);
        grid.add(new Label("Remarks"), 0, 5);
        grid.add(remarksField, 1, 5);

        HBox btnBar = new HBox(10);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button("Record Attendance");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            try {
                Officer selOff = officerCombo.getValue();
                if (selOff == null) throw new Exception("Select an officer.");

                LocalTime inT = null;
                LocalTime outT = null;
                try {
                    if (!checkInField.getText().trim().isEmpty()) inT = LocalTime.parse(checkInField.getText().trim());
                    if (!checkOutField.getText().trim().isEmpty()) outT = LocalTime.parse(checkOutField.getText().trim());
                } catch (Exception ex) {
                    throw new Exception("Time must be in format HH:mm:ss");
                }

                Attendance att = new Attendance(
                        0, selOff.getOfficerId(), null, datePicker.getValue(), inT, outT,
                        statusCombo.getValue(), remarksField.getText().trim()
                );

                dutyService.recordAttendance(att);
                UIComponents.showInfoAlert("Recorded", "Shift attendance recorded for " + selOff.getFullName());
                dialog.close();
                loadData();
            } catch (Exception ex) {
                UIComponents.showErrorAlert("Validation Error", ex.getMessage());
            }
        });

        btnBar.getChildren().addAll(cancelBtn, saveBtn);
        form.getChildren().addAll(title, grid, btnBar);

        Scene scene = new Scene(form);
        ViewNavigator.applyStyles(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void handleDeleteAttendance(Attendance a) {
        if (UIComponents.showConfirmDialog("Confirm Delete Attendance",
                "Delete attendance record for " + a.getOfficerName() + " on " + DateUtil.formatDate(a.getAttendanceDate()) + "?")) {
            try {
                dutyService.deleteAttendance(a.getId());
                UIComponents.showInfoAlert("Deleted", "Attendance record deleted.");
                loadData();
            } catch (Exception ex) {
                UIComponents.showErrorAlert("Error", ex.getMessage());
            }
        }
    }
}
