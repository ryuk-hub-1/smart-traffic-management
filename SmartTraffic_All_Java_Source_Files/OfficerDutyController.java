package com.smarttraffic.controller.officer;

import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.model.DutyAssignment;
import com.smarttraffic.model.Officer;
import com.smarttraffic.model.enums.DutyStatus;
import com.smarttraffic.model.enums.ShiftType;
import com.smarttraffic.service.AuthService;
import com.smarttraffic.service.DutyService;
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
 * Modernized Controller for Officer Personal Shift & Duty Roster.
 * Features Visual Shift Cards, Status Badges, Beat Telemetry, and Tabular Toggle.
 */
public class OfficerDutyController {
    private final VBox root = new VBox(14);
    private final DutyService dutyService = DutyService.getInstance();

    private final ObservableList<DutyAssignment> masterList = FXCollections.observableArrayList();
    private FilteredList<DutyAssignment> filteredList;

    private final FlowPane cardsContainer = new FlowPane();
    private final TableView<DutyAssignment> table = new TableView<>();
    private final StackPane viewContainer = new StackPane();
    private final HBox statsBanner = new HBox(12);

    private final TextField searchField = UIComponents.createSearchField("Search junction, shift, notes...");

    public OfficerDutyController() {
        buildUI();
        loadData();
    }

    public Node getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(10, 10, 30, 10));

        VBox header = UIComponents.createViewHeader(
                "My Duty Schedule & Assigned Shifts",
                "Track upcoming junction deployments, morning/evening/night shift assignments, and operational briefs"
        );

        // Stats Banner
        statsBanner.setAlignment(Pos.CENTER_LEFT);
        updateStatsBanner();

        // Control Bar
        HBox controlBar = new HBox(12);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox viewToggle = UIComponents.createViewToggle("Schedule Cards", "Tabular View", isCardView -> {
            if (isCardView) {
                showCardsView();
            } else {
                showTableView();
            }
        });

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> loadData());

        controlBar.getChildren().addAll(searchField, spacer, viewToggle, refreshBtn);

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

        // Filters Setup
        filteredList = new FilteredList<>(masterList, p -> true);

        Runnable applyFilter = () -> {
            String query = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
            filteredList.setPredicate(d -> {
                if (query.isEmpty()) return true;
                return d.getJunctionName().toLowerCase().contains(query) ||
                        d.getShift().getTitle().toLowerCase().contains(query) ||
                        d.getStatus().name().toLowerCase().contains(query) ||
                        (d.getNotes() != null && d.getNotes().toLowerCase().contains(query));
            });
            renderCards();
        };

        searchField.textProperty().addListener((obs, o, n) -> applyFilter.run());
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

        TableColumn<DutyAssignment, String> colDate = new TableColumn<>("Duty Date");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(DateUtil.formatDate(d.getValue().getDutyDate())));
        colDate.setPrefWidth(120);

        TableColumn<DutyAssignment, String> colShift = new TableColumn<>("Shift");
        colShift.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getShift().getTitle()));

        TableColumn<DutyAssignment, String> colJunction = new TableColumn<>("Assigned Junction");
        colJunction.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getJunctionName()));

        TableColumn<DutyAssignment, String> colTiming = new TableColumn<>("Shift Timings");
        colTiming.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getStartTime() + " - " + d.getValue().getEndTime()
        ));

        TableColumn<DutyAssignment, String> colStatus = new TableColumn<>("Duty Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().getLabel()));

        TableColumn<DutyAssignment, String> colNotes = new TableColumn<>("Operational Notes");
        colNotes.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNotes()));

        table.getColumns().addAll(colDate, colShift, colJunction, colTiming, colStatus, colNotes);
    }

    private void renderCards() {
        cardsContainer.getChildren().clear();

        if (filteredList.isEmpty()) {
            VBox emptyBox = new VBox(10);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40));
            Label emptyLbl = new Label("No duty assignments found matching search");
            emptyLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");
            emptyBox.getChildren().add(emptyLbl);
            cardsContainer.getChildren().add(emptyBox);
            return;
        }

        for (DutyAssignment d : filteredList) {
            VBox card = new VBox(10);
            card.getStyleClass().add("shift-card");
            card.setPrefWidth(320);

            // Header: Date & Status Badge
            HBox topRow = new HBox(8);
            topRow.setAlignment(Pos.CENTER_LEFT);

            Label dateLbl = new Label("📅 " + DateUtil.formatDate(d.getDutyDate()));
            dateLbl.setStyle("-fx-font-weight: 800; -fx-font-size: 13px; -fx-text-fill: #0f172a;");

            Region topSpacer = new Region();
            HBox.setHgrow(topSpacer, Priority.ALWAYS);

            Label statusBadge = new Label(d.getStatus().getLabel());
            switch (d.getStatus()) {
                case SCHEDULED -> statusBadge.getStyleClass().addAll("badge", "badge-warning");
                case IN_PROGRESS -> statusBadge.getStyleClass().addAll("badge", "badge-primary");
                case COMPLETED -> statusBadge.getStyleClass().addAll("badge", "badge-success");
                case CANCELLED -> statusBadge.getStyleClass().addAll("badge", "badge-danger");
            }

            topRow.getChildren().addAll(dateLbl, topSpacer, statusBadge);

            // Shift & Timing Row
            HBox shiftRow = new HBox(8);
            shiftRow.setAlignment(Pos.CENTER_LEFT);

            String shiftEmoji = switch (d.getShift()) {
                case MORNING -> "🌅";
                case EVENING -> "🌆";
                case NIGHT -> "🌙";
            };

            Label shiftLbl = new Label(shiftEmoji + " " + d.getShift().getTitle());
            shiftLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e3a8a;");

            Region shiftSpacer = new Region();
            HBox.setHgrow(shiftSpacer, Priority.ALWAYS);

            Label timeChip = new Label("🕒 " + d.getStartTime() + " - " + d.getEndTime());
            timeChip.setStyle("-fx-font-size: 11px; -fx-background-color: #f1f5f9; -fx-padding: 2px 6px; -fx-background-radius: 4px;");

            shiftRow.getChildren().addAll(shiftLbl, shiftSpacer, timeChip);

            // Junction Location
            HBox juncRow = new HBox(6);
            juncRow.setAlignment(Pos.CENTER_LEFT);
            juncRow.setStyle("-fx-background-color: #eff6ff; -fx-padding: 8px 10px; -fx-background-radius: 6px;");
            Label juncIcon = new Label("📍");
            Label juncText = new Label(d.getJunctionName());
            juncText.setStyle("-fx-font-weight: 800; -fx-font-size: 12px; -fx-text-fill: #1d4ed8;");
            juncRow.getChildren().addAll(juncIcon, juncText);

            // Operational Notes
            VBox notesBox = new VBox(2);
            if (d.getNotes() != null && !d.getNotes().trim().isEmpty()) {
                Label nTitle = new Label("Briefing & Directives:");
                nTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
                Label nVal = new Label(d.getNotes());
                nVal.setStyle("-fx-font-size: 11px; -fx-text-fill: #334155;");
                nVal.setWrapText(true);
                notesBox.getChildren().addAll(nTitle, nVal);
            }

            card.getChildren().addAll(topRow, shiftRow, juncRow, notesBox);
            cardsContainer.getChildren().add(card);
        }
    }

    private void updateStatsBanner() {
        statsBanner.getChildren().clear();
        int total = masterList.size();
        long morning = masterList.stream().filter(d -> d.getShift() == ShiftType.MORNING).count();
        long evening = masterList.stream().filter(d -> d.getShift() == ShiftType.EVENING).count();
        long night = masterList.stream().filter(d -> d.getShift() == ShiftType.NIGHT).count();

        statsBanner.getChildren().addAll(
                UIComponents.createStatChip("📋", "ASSIGNED SHIFTS", String.valueOf(total), "#ffffff"),
                UIComponents.createStatChip("🌅", "MORNING SHIFTS", String.valueOf(morning), "#fffbeb"),
                UIComponents.createStatChip("🌆", "EVENING SHIFTS", String.valueOf(evening), "#eff6ff"),
                UIComponents.createStatChip("🌙", "NIGHT SHIFTS", String.valueOf(night), "#f5f3ff")
        );
    }

    private void loadData() {
        Officer officer = (AuthService.getInstance().getCurrentUser() instanceof Officer o) ? o : null;
        if (officer == null) return;

        ThreadPoolManager.getInstance().execute(() -> {
            try {
                List<DutyAssignment> list = dutyService.getDutiesForOfficer(officer.getOfficerId());
                Platform.runLater(() -> {
                    masterList.setAll(list);
                    updateStatsBanner();
                    renderCards();
                });
            } catch (DatabaseException e) {
                Platform.runLater(() -> UIComponents.showErrorAlert("Database Error", "Failed to load duties: " + e.getMessage()));
            }
        });
    }
}
