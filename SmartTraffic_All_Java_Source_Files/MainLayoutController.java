package com.smarttraffic.controller;

import com.smarttraffic.controller.admin.*;
import com.smarttraffic.controller.citizen.*;
import com.smarttraffic.controller.officer.*;
import com.smarttraffic.model.Officer;
import com.smarttraffic.model.User;
import com.smarttraffic.model.enums.Role;
import com.smarttraffic.service.AuthService;
import com.smarttraffic.util.DateUtil;
import com.smarttraffic.util.ThreadPoolManager;
import com.smarttraffic.view.UIComponents;
import com.smarttraffic.view.ViewNavigator;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Main Layout Shell Controller.
 * Provides top bar with live digital clock, role-specific sidebar navigation,
 * and swappable main content container.
 */
public class MainLayoutController {
    private final BorderPane root = new BorderPane();
    private final VBox sidebar = new VBox();
    private final StackPane contentArea = new StackPane();
    private final Label clockLabel = new Label();
    private ScheduledFuture<?> clockTask;
    private Button activeButton;

    public MainLayoutController() {
        buildUI();
        startClock();
    }

    public Parent getView() {
        return root;
    }

    public void setContent(Node content) {
        contentArea.getChildren().clear();
        if (content instanceof ScrollPane) {
            contentArea.getChildren().add(content);
        } else {
            ScrollPane sp = new ScrollPane(content);
            sp.setFitToWidth(true);
            sp.setFitToHeight(true);
            sp.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
            contentArea.getChildren().add(sp);
        }
    }

    public void highlightMenuByText(String keyword) {
        if (keyword == null) return;
        for (Node n : sidebar.getChildren()) {
            if (n instanceof Button btn) {
                if (btn.getText().toLowerCase().contains(keyword.toLowerCase())) {
                    if (activeButton != null) {
                        activeButton.getStyleClass().remove("nav-button-active");
                    }
                    btn.getStyleClass().add("nav-button-active");
                    activeButton = btn;
                    break;
                }
            }
        }
    }

    private void buildUI() {
        // --- 1. Top Header Bar ---
        HBox topBar = new HBox(15);
        topBar.getStyleClass().add("top-header-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label logoIcon = new Label("🚦");
        logoIcon.setStyle("-fx-font-size: 24px;");

        VBox titleBox = new VBox(2);
        Label appTitle = new Label("SMART TRAFFIC POLICE MANAGEMENT SYSTEM");
        appTitle.getStyleClass().add("top-brand-title");

        User user = AuthService.getInstance().getCurrentUser();
        String subDesc = user != null
                ? user.getRole().getDisplayName() + " Portal • Station Operations"
                : "Central Control System";
        Label subLabel = new Label(subDesc);
        subLabel.getStyleClass().add("top-brand-subtitle");
        titleBox.getChildren().addAll(appTitle, subLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Live Clock
        clockLabel.getStyleClass().add("clock-label");
        clockLabel.setText(DateUtil.formatDateTime(LocalDateTime.now()));

        // User badge
        HBox userBadge = new HBox(10);
        userBadge.getStyleClass().add("user-badge");
        userBadge.setAlignment(Pos.CENTER_LEFT);

        VBox userDetails = new VBox(1);
        String name = user != null ? user.getFullName() : "Guest";
        String roleStr = user != null ? user.getRole().name() : "N/A";
        if (user instanceof Officer o && o.getBadgeNumber() != null) {
            roleStr += " (" + o.getBadgeNumber() + ")";
        }

        Label nameLbl = new Label(name);
        nameLbl.getStyleClass().add("user-badge-name");
        Label roleLbl = new Label(roleStr);
        roleLbl.getStyleClass().add("user-badge-role");
        userDetails.getChildren().addAll(nameLbl, roleLbl);
        userBadge.getChildren().add(userDetails);

        // Logout Button
        Button logoutBtn = new Button("Sign Out 🚪");
        logoutBtn.getStyleClass().add("logout-button");
        logoutBtn.setOnAction(e -> handleLogout());

        topBar.getChildren().addAll(logoIcon, titleBox, spacer, clockLabel, userBadge, logoutBtn);
        root.setTop(topBar);

        // --- 2. Sidebar Navigation ---
        sidebar.getStyleClass().add("sidebar");
        buildSidebarMenu(user);
        root.setLeft(sidebar);

        // --- 3. Content Area ---
        contentArea.setStyle("-fx-background-color: #f1f5f9; -fx-padding: 20px;");
        root.setCenter(contentArea);
    }

    private void buildSidebarMenu(User user) {
        sidebar.getChildren().clear();

        if (user == null) return;

        Label menuHeader = new Label("NAVIGATION MENU");
        menuHeader.getStyleClass().add("sidebar-category");
        sidebar.getChildren().add(menuHeader);

        if (user.getRole() == Role.ADMIN) {
            addNavButton("📊 Department Dashboard", () -> setContent(new AdminDashboardController().getView()), true);
            addNavButton("👮 Officer Management", () -> setContent(new OfficerManagementController().getView()), false);
            addNavButton("📍 Traffic Junctions", () -> setContent(new JunctionManagementController().getView()), false);
            addNavButton("📅 Duty & Shift Scheduling", () -> setContent(new DutyAllocationController().getView()), false);
            addNavButton("⏱️ Attendance Monitor", () -> setContent(new AttendanceMonitorController().getView()), false);
            addNavButton("📋 Violations & Challans", () -> setContent(new AdminChallansController().getView()), false);
            addNavButton("📩 Citizen Complaints", () -> setContent(new AdminComplaintsController().getView()), false);
            addNavButton("🚨 Emergency & Accidents", () -> setContent(new AdminEmergencyController().getView()), false);
            addNavButton("📈 Reports & Analytics", () -> setContent(new ReportsAnalyticsController().getView()), false);
        } else if (user.getRole() == Role.OFFICER) {
            addNavButton("📊 Officer Dashboard", () -> setContent(new OfficerDashboardController().getView()), true);
            addNavButton("⚡ Log Violation & Challan", () -> setContent(new LogViolationController().getView()), false);
            addNavButton("📄 Issued Challans", () -> setContent(new OfficerChallansController().getView()), false);
            addNavButton("📅 My Shift & Duties", () -> setContent(new OfficerDutyController().getView()), false);
            addNavButton("🚨 Report Emergency", () -> setContent(new ReportEmergencyController().getView()), false);
            addNavButton("📩 Assigned Complaints", () -> setContent(new OfficerComplaintsController().getView()), false);
        } else if (user.getRole() == Role.CITIZEN) {
            addNavButton("📊 Citizen Dashboard", () -> setContent(new CitizenDashboardController().getView()), true);
            addNavButton("💳 My Challans & Pay Fines", () -> setContent(new CitizenChallansController().getView()), false);
            addNavButton("📝 File Traffic Complaint", () -> setContent(new FileComplaintController().getView()), false);
            addNavButton("🕒 Complaint History", () -> setContent(new CitizenComplaintsHistoryController().getView()), false);
        }
    }

    private void addNavButton(String text, Runnable action, boolean isDefault) {
        Button btn = new Button(text);
        btn.getStyleClass().add("nav-button");

        btn.setOnAction(e -> {
            if (activeButton != null) {
                activeButton.getStyleClass().remove("nav-button-active");
            }
            btn.getStyleClass().add("nav-button-active");
            activeButton = btn;
            action.run();
        });

        sidebar.getChildren().add(btn);

        if (isDefault) {
            btn.getStyleClass().add("nav-button-active");
            activeButton = btn;
            action.run();
        }
    }

    private void startClock() {
        clockTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                clockLabel.setText(DateUtil.formatDateTime(LocalDateTime.now()));
            });
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void handleLogout() {
        if (UIComponents.showConfirmDialog("Confirm Sign Out", "Are you sure you want to sign out from the portal?")) {
            if (clockTask != null) {
                clockTask.cancel(true);
            }
            AuthService.getInstance().logout();
            ViewNavigator.showLogin();
        }
    }
}
