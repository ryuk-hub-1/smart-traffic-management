package com.smarttraffic.controller.admin;

import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.exception.ValidationException;
import com.smarttraffic.model.Junction;
import com.smarttraffic.model.Officer;
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
 * Controller for Admin Officer Management: Interactive ID Card Grid & Roster.
 */
public class OfficerManagementController {
    private final VBox root = new VBox(15);
    private final OfficerService officerService = OfficerService.getInstance();
    private final JunctionService junctionService = JunctionService.getInstance();

    private final ObservableList<Officer> masterList = FXCollections.observableArrayList();
    private final FilteredList<Officer> filteredList = new FilteredList<>(masterList, p -> true);

    private final FlowPane cardsPane = new FlowPane();
    private final TableView<Officer> table = new TableView<>();
    private final StackPane viewContainer = new StackPane();

    private final TextField searchField = UIComponents.createSearchField("Search badge, name, rank, station, junction...");
    private final HBox statsBanner = new HBox(12);

    public OfficerManagementController() {
        buildUI();
        loadData();
    }

    public Node getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(10, 10, 30, 10));

        VBox header = UIComponents.createViewHeader(
                "Traffic Police Personnel Roster & Deployments",
                "Command and inspect active field officers, monitor junction beats, and update personnel credentials"
        );

        // Stats Banner
        statsBanner.setAlignment(Pos.CENTER_LEFT);
        updateStatsBanner();

        // Control Bar
        HBox controlBar = new HBox(12);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox viewToggle = UIComponents.createViewToggle("Personnel Cards", "Tabular List", isCardView -> {
            viewContainer.getChildren().clear();
            if (isCardView) {
                viewContainer.getChildren().add(new ScrollPane(cardsPane));
            } else {
                viewContainer.getChildren().add(new VBox(table));
            }
        });

        Button addBtn = new Button("➕ Add Officer");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> showOfficerDialog(null));

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

        TableColumn<Officer, String> colBadge = new TableColumn<>("Badge Number");
        colBadge.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBadgeNumber()));
        colBadge.setPrefWidth(120);

        TableColumn<Officer, String> colRank = new TableColumn<>("Rank");
        colRank.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRank()));

        TableColumn<Officer, String> colName = new TableColumn<>("Officer Name");
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));

        TableColumn<Officer, String> colStation = new TableColumn<>("Police Station");
        colStation.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPoliceStation()));

        TableColumn<Officer, String> colJunction = new TableColumn<>("Assigned Junction");
        colJunction.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getAssignedJunctionName() != null ? d.getValue().getAssignedJunctionName() : "Unassigned"
        ));

        TableColumn<Officer, String> colPhone = new TableColumn<>("Phone");
        colPhone.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPhone()));

        TableColumn<Officer, String> colStatus = new TableColumn<>("Duty Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));

        TableColumn<Officer, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(160);
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏️ Edit");
            private final Button delBtn = new Button("🗑️ Delete");
            private final HBox pane = new HBox(6, editBtn, delBtn);
            {
                editBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                delBtn.getStyleClass().addAll("btn-danger", "btn-small");

                editBtn.setOnAction(e -> {
                    Officer o = getTableView().getItems().get(getIndex());
                    showOfficerDialog(o);
                });

                delBtn.setOnAction(e -> {
                    Officer o = getTableView().getItems().get(getIndex());
                    handleDeleteOfficer(o);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        table.getColumns().addAll(colBadge, colRank, colName, colStation, colJunction, colPhone, colStatus, colActions);
        table.setItems(filteredList);

        // Search Filter Listener
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            filteredList.setPredicate(officer -> {
                if (newV == null || newV.trim().isEmpty()) return true;
                String lower = newV.trim().toLowerCase();
                return officer.getBadgeNumber().toLowerCase().contains(lower) ||
                       officer.getFullName().toLowerCase().contains(lower) ||
                       officer.getRank().toLowerCase().contains(lower) ||
                       officer.getPoliceStation().toLowerCase().contains(lower) ||
                       (officer.getAssignedJunctionName() != null && officer.getAssignedJunctionName().toLowerCase().contains(lower));
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
            Label emptyLbl = new Label("No officers found matching your criteria.");
            emptyLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");
            emptyBox.getChildren().add(emptyLbl);
            cardsPane.getChildren().add(emptyBox);
            return;
        }

        for (Officer o : filteredList) {
            cardsPane.getChildren().add(createOfficerCard(o));
        }
    }

    private VBox createOfficerCard(Officer o) {
        VBox card = new VBox(10);
        card.getStyleClass().add("officer-card");

        // Top Row: Avatar + Name/Rank + Status Pill
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = UIComponents.createRankAvatar(o.getRank());

        VBox nameCol = new VBox(2);
        Label nameLbl = new Label(o.getFullName());
        nameLbl.setStyle("-fx-font-weight: 800; -fx-font-size: 14px; -fx-text-fill: #0f172a;");

        HBox rankBadgeRow = new HBox(6);
        rankBadgeRow.setAlignment(Pos.CENTER_LEFT);
        Label rankLbl = new Label(o.getRank() != null ? o.getRank().toUpperCase() : "OFFICER");
        rankLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #2563eb;");
        Label badgePill = new Label("ID: " + o.getBadgeNumber());
        badgePill.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #64748b; -fx-background-color: #f1f5f9; -fx-padding: 2px 6px; -fx-background-radius: 4px;");
        rankBadgeRow.getChildren().addAll(rankLbl, badgePill);
        nameCol.getChildren().addAll(nameLbl, rankBadgeRow);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        boolean isActive = "ACTIVE".equalsIgnoreCase(o.getStatus());
        Label statusPill = UIComponents.createBadge(
                isActive ? "🟢 ON DUTY" : "⚪ " + o.getStatus(),
                isActive ? "badge-success" : "badge-neutral"
        );

        topRow.getChildren().addAll(avatar, nameCol, spacer, statusPill);

        // Junction Deployment Chip
        HBox junctionRow = new HBox(8);
        junctionRow.setAlignment(Pos.CENTER_LEFT);
        junctionRow.setStyle("-fx-background-color: #f8fafc; -fx-padding: 8px 12px; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-radius: 8px;");

        Label jIcon = new Label("📍");
        VBox jText = new VBox(1);
        Label jTitle = new Label("Assigned Beat Junction");
        jTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #64748b;");
        String jName = o.getAssignedJunctionName() != null ? o.getAssignedJunctionName() : "No Beat Assigned";
        Label jVal = new Label(jName);
        jVal.setStyle("-fx-font-weight: 800; -fx-font-size: 12px; -fx-text-fill: #1e293b;");
        jText.getChildren().addAll(jTitle, jVal);
        junctionRow.getChildren().addAll(jIcon, jText);

        // Details Row (Station & Contact)
        VBox detailsCol = new VBox(4);
        Label stationLbl = new Label("🏢 " + o.getPoliceStation());
        stationLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");

        Label contactLbl = new Label("📞 " + (o.getPhone() != null ? o.getPhone() : "N/A") + "  |  ✉️ " + (o.getEmail() != null ? o.getEmail() : "N/A"));
        contactLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        detailsCol.getChildren().addAll(stationLbl, contactLbl);

        // Action Buttons Row
        HBox actionRow = new HBox(8);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        actionRow.setPadding(new Insets(4, 0, 0, 0));

        Button editBtn = new Button("✏️ Edit Profile");
        editBtn.getStyleClass().addAll("btn-secondary", "btn-small");
        editBtn.setOnAction(e -> showOfficerDialog(o));

        Button delBtn = new Button("🗑️ Delete");
        delBtn.getStyleClass().addAll("btn-danger", "btn-small");
        delBtn.setOnAction(e -> handleDeleteOfficer(o));

        actionRow.getChildren().addAll(editBtn, delBtn);

        card.getChildren().addAll(topRow, junctionRow, detailsCol, actionRow);
        return card;
    }

    private void updateStatsBanner() {
        statsBanner.getChildren().clear();
        int total = masterList.size();
        long active = masterList.stream().filter(o -> "ACTIVE".equalsIgnoreCase(o.getStatus())).count();
        long onLeave = masterList.stream().filter(o -> !"ACTIVE".equalsIgnoreCase(o.getStatus())).count();
        long assigned = masterList.stream().filter(o -> o.getAssignedJunctionId() != null && o.getAssignedJunctionId() > 0).count();

        statsBanner.getChildren().addAll(
                UIComponents.createStatChip("👮", "TOTAL FORCE", String.valueOf(total), "#ffffff"),
                UIComponents.createStatChip("🟢", "ACTIVE ON BEAT", String.valueOf(active), "#f0fdf4"),
                UIComponents.createStatChip("📍", "DEPLOYED TO JUNCTIONS", String.valueOf(assigned), "#eff6ff"),
                UIComponents.createStatChip("🏖️", "ON LEAVE / RESERVE", String.valueOf(onLeave), "#fefce8")
        );
    }

    private void loadData() {
        ThreadPoolManager.getInstance().execute(() -> {
            try {
                List<Officer> list = officerService.getAllOfficers();
                Platform.runLater(() -> {
                    masterList.setAll(list);
                    updateStatsBanner();
                    renderCardsGrid();
                });
            } catch (DatabaseException e) {
                Platform.runLater(() -> UIComponents.showErrorAlert("Database Error", "Failed to load officers: " + e.getMessage()));
            }
        });
    }

    private void showOfficerDialog(Officer existing) {
        boolean isEdit = existing != null;
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(ViewNavigator.getPrimaryStage());
        dialog.setTitle(isEdit ? "Edit Officer - " + existing.getBadgeNumber() : "Register New Traffic Officer");

        VBox form = new VBox(12);
        form.setPadding(new Insets(20));
        form.setPrefWidth(500);

        Label title = new Label(isEdit ? "Update Officer Record" : "Add New Officer Account");
        title.getStyleClass().add("section-title");

        TextField uField = new TextField();
        PasswordField pField = new PasswordField();
        TextField nameField = new TextField();
        TextField emailField = new TextField();
        TextField phoneField = new TextField();
        TextField badgeField = new TextField();
        ComboBox<String> rankCombo = new ComboBox<>();
        rankCombo.getItems().addAll("Constable", "Head Constable", "Assistant Sub-Inspector", "Sub-Inspector", "Inspector", "Assistant Commissioner");
        rankCombo.setValue("Sub-Inspector");

        TextField stationField = new TextField("Central Division Traffic PS");
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("ACTIVE", "ON_LEAVE", "SUSPENDED", "RETIRED");
        statusCombo.setValue("ACTIVE");

        ComboBox<Junction> junctionCombo = new ComboBox<>();
        Junction noneJunction = new Junction(0, "NONE", "-- No Beat Assigned (Reserve) --", "HQ", "", 0, 0, null);
        junctionCombo.getItems().add(noneJunction);
        try {
            List<Junction> junctions = junctionService.getAllJunctions();
            junctionCombo.getItems().addAll(junctions);
            junctionCombo.setValue(noneJunction);
        } catch (Exception ignored) {
        }

        PasswordField editPassField = new PasswordField();
        editPassField.setPromptText("Leave blank to keep existing password");

        if (isEdit) {
            nameField.setText(existing.getFullName());
            emailField.setText(existing.getEmail());
            phoneField.setText(existing.getPhone());
            badgeField.setText(existing.getBadgeNumber());
            rankCombo.setValue(existing.getRank());
            stationField.setText(existing.getPoliceStation());
            statusCombo.setValue(existing.getStatus());

            if (existing.getAssignedJunctionId() != null) {
                for (Junction j : junctionCombo.getItems()) {
                    if (j.getId() == existing.getAssignedJunctionId()) {
                        junctionCombo.setValue(j);
                        break;
                    }
                }
            }
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        int row = 0;
        if (!isEdit) {
            grid.add(new Label("Login Username *"), 0, row);
            grid.add(uField, 1, row++);
            grid.add(new Label("Password *"), 0, row);
            grid.add(pField, 1, row++);
        } else {
            grid.add(new Label("New Password"), 0, row);
            grid.add(editPassField, 1, row++);
        }

        grid.add(new Label("Badge Number *"), 0, row);
        grid.add(badgeField, 1, row++);
        grid.add(new Label("Full Name *"), 0, row);
        grid.add(nameField, 1, row++);
        grid.add(new Label("Rank *"), 0, row);
        grid.add(rankCombo, 1, row++);
        grid.add(new Label("Police Station *"), 0, row);
        grid.add(stationField, 1, row++);
        grid.add(new Label("Assigned Beat Junction"), 0, row);
        grid.add(junctionCombo, 1, row++);
        grid.add(new Label("Phone"), 0, row);
        grid.add(phoneField, 1, row++);
        grid.add(new Label("Email"), 0, row);
        grid.add(emailField, 1, row++);

        if (isEdit) {
            grid.add(new Label("Status"), 0, row);
            grid.add(statusCombo, 1, row++);
        }

        HBox btnBar = new HBox(10);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button(isEdit ? "Update Officer" : "Create Officer Account");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            try {
                Junction selectedJunction = junctionCombo.getValue();
                Integer jId = (selectedJunction != null && selectedJunction.getId() > 0) ? selectedJunction.getId() : null;

                if (isEdit) {
                    existing.setBadgeNumber(badgeField.getText().trim().toUpperCase());
                    existing.setFullName(nameField.getText().trim());
                    existing.setEmail(emailField.getText() != null ? emailField.getText().trim() : "");
                    existing.setPhone(phoneField.getText() != null ? phoneField.getText().trim() : "");
                    existing.setRank(rankCombo.getValue());
                    existing.setPoliceStation(stationField.getText().trim());
                    existing.setStatus(statusCombo.getValue());
                    existing.setAssignedJunctionId(jId);
                    existing.setAssignedJunctionName((selectedJunction != null && selectedJunction.getId() > 0) ? selectedJunction.getName() : null);

                    String newPwd = editPassField.getText();
                    if (newPwd != null && !newPwd.trim().isEmpty()) {
                        if (newPwd.trim().length() < 4) {
                            throw new ValidationException("New password must be at least 4 characters.");
                        }
                        existing.setPasswordHash(com.smarttraffic.util.PasswordUtil.hashPassword(newPwd.trim()));
                    }

                    officerService.updateOfficer(existing);
                    UIComponents.showInfoAlert("Success", "Officer details updated successfully.");
                } else {
                    officerService.registerOfficer(
                            uField.getText(),
                            pField.getText(),
                            nameField.getText(),
                            emailField.getText(),
                            phoneField.getText(),
                            badgeField.getText(),
                            rankCombo.getValue(),
                            stationField.getText(),
                            jId
                    );
                    UIComponents.showInfoAlert("Success", "New Officer registered successfully.");
                }
                dialog.close();
                loadData();
            } catch (ValidationException ex) {
                UIComponents.showWarningAlert("Validation Error", ex.getMessage());
            } catch (Exception ex) {
                UIComponents.showErrorAlert("Operation Error", ex.getMessage());
            }
        });

        btnBar.getChildren().addAll(cancelBtn, saveBtn);
        form.getChildren().addAll(title, grid, btnBar);

        Scene scene = new Scene(form);
        ViewNavigator.applyStyles(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void handleDeleteOfficer(Officer officer) {
        if (UIComponents.showConfirmDialog("Confirm Delete Officer",
                "Are you sure you want to permanently delete officer " + officer.getFullName() + " [" + officer.getBadgeNumber() + "]?")) {
            try {
                officerService.deleteOfficer(officer.getOfficerId());
                UIComponents.showInfoAlert("Deleted", "Officer deleted successfully.");
                loadData();
            } catch (Exception e) {
                UIComponents.showErrorAlert("Delete Error", "Could not delete officer: " + e.getMessage());
            }
        }
    }
}
