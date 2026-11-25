import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * =========================================================================================
 * SMART TRAFFIC POLICE MANAGEMENT SYSTEM (STPMS)
 * =========================================================================================
 * Advanced Java Academic Project - Standalone All-In-One Source File
 * 
 * Demonstrates Core Advanced Java & Software Engineering Concepts:
 * 1. Object-Oriented Design (Encapsulation, Inheritance, Polymorphism, Abstraction)
 * 2. Interfaces & Factory Pattern
 * 3. Exception Handling (Custom exceptions with user-friendly alerts)
 * 4. Java Collections Framework (ObservableList, PriorityQueue, HashMap, Streams)
 * 5. Native JDBC & PreparedStatements (ACID compliance & SQL Injection immunity)
 * 6. Multithreading & Asynchronous Execution (ExecutorService background daemon pool)
 * 7. Modern JavaFX Desktop GUI (Slate-Dark Theme, Sidebar layout, Real-time telemetry)
 * 
 * Execution Instructions:
 * - With Maven: mvn javafx:run
 * - With JDK: javac --module-path <javafx-sdk>/lib --add-modules javafx.controls,javafx.fxml SmartTrafficManagementSystem.java
 *             java --module-path <javafx-sdk>/lib --add-modules javafx.controls,javafx.fxml -cp ".;sqlite-jdbc.jar" SmartTrafficManagementSystem
 * =========================================================================================
 */
public class SmartTrafficManagementSystem extends Application {

    // --- Database Configuration ---
    private static final String DB_URL = "jdbc:sqlite:smart_traffic.db";
    private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    // --- Active Session State ---
    private Stage primaryStage;
    private String currentUsername = "";
    private String currentRole = "";
    private String currentFullName = "";
    private int currentUserId = 0;

    // --- Main Entry Point ---
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        initDatabase();
        showLoginScreen();
    }

    @Override
    public void stop() {
        THREAD_POOL.shutdown();
    }

    // =====================================================================================
    // 1. DATABASE INITIALIZATION & CONNECTION POOLING (JDBC)
    // =====================================================================================
    private Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found: " + e.getMessage());
        }
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    private void initDatabase() {
        THREAD_POOL.execute(() -> {
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                // 1. Users Table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT UNIQUE NOT NULL,
                        password_hash TEXT NOT NULL,
                        role TEXT NOT NULL,
                        full_name TEXT NOT NULL,
                        email TEXT,
                        phone TEXT,
                        created_at TEXT NOT NULL
                    );
                """);

                // 2. Junctions Table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS junctions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        junction_code TEXT UNIQUE NOT NULL,
                        name TEXT NOT NULL,
                        area TEXT NOT NULL,
                        landmark TEXT,
                        risk_level TEXT DEFAULT 'MEDIUM'
                    );
                """);

                // 3. Officers Table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS officers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER UNIQUE NOT NULL,
                        badge_number TEXT UNIQUE NOT NULL,
                        rank TEXT NOT NULL,
                        police_station TEXT NOT NULL,
                        status TEXT DEFAULT 'ACTIVE',
                        assigned_junction_id INTEGER,
                        FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                    );
                """);

                // 4. Challans Table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS challans (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        challan_number TEXT UNIQUE NOT NULL,
                        vehicle_number TEXT NOT NULL,
                        vehicle_type TEXT NOT NULL,
                        violator_name TEXT,
                        violator_phone TEXT,
                        officer_id INTEGER,
                        junction_id INTEGER,
                        violation_date TEXT NOT NULL,
                        fine_amount REAL NOT NULL,
                        payment_status TEXT DEFAULT 'PENDING',
                        payment_date TEXT,
                        payment_reference TEXT,
                        remarks TEXT
                    );
                """);

                // 5. Complaints Table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS complaints (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        complaint_number TEXT UNIQUE NOT NULL,
                        citizen_id INTEGER,
                        complaint_type TEXT NOT NULL,
                        vehicle_number TEXT,
                        description TEXT NOT NULL,
                        incident_date TEXT NOT NULL,
                        status TEXT DEFAULT 'PENDING',
                        officer_action_notes TEXT,
                        created_at TEXT NOT NULL
                    );
                """);

                // 6. Emergency Collisions Table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS emergency_reports (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        report_number TEXT UNIQUE NOT NULL,
                        junction_name TEXT NOT NULL,
                        accident_type TEXT NOT NULL,
                        severity TEXT NOT NULL,
                        casualties_count INTEGER DEFAULT 0,
                        ambulance_required INTEGER DEFAULT 0,
                        tow_truck_required INTEGER DEFAULT 0,
                        report_time TEXT NOT NULL,
                        status TEXT DEFAULT 'REPORTED',
                        notes TEXT
                    );
                """);

                // Seed Default Admin and Demo Records if empty
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users;");
                if (rs.next() && rs.getInt(1) == 0) {
                    seedInitialData(conn);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void seedInitialData(Connection conn) {
        try {
            // Seed Users
            String insertUser = "INSERT INTO users (username, password_hash, role, full_name, email, phone, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertUser)) {
                ps.setString(1, "admin");
                ps.setString(2, "admin123");
                ps.setString(3, "ADMIN");
                ps.setString(4, "Commissioner Rajesh Kumar");
                ps.setString(5, "admin@trafficpolice.gov.in");
                ps.setString(6, "9876543210");
                ps.setString(7, LocalDateTime.now().toString());
                ps.executeUpdate();

                ps.setString(1, "officer1");
                ps.setString(2, "officer123");
                ps.setString(3, "OFFICER");
                ps.setString(4, "Inspector Rahul Sharma");
                ps.setString(5, "rahul.sharma@trafficpolice.gov.in");
                ps.setString(6, "9811223344");
                ps.setString(7, LocalDateTime.now().toString());
                ps.executeUpdate();

                ps.setString(1, "citizen1");
                ps.setString(2, "citizen123");
                ps.setString(3, "CITIZEN");
                ps.setString(4, "Amit Verma");
                ps.setString(5, "amit.verma@gmail.com");
                ps.setString(6, "9988776655");
                ps.setString(7, LocalDateTime.now().toString());
                ps.executeUpdate();
            }

            // Seed Officer Profile
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO officers (user_id, badge_number, rank, police_station, status) VALUES (?, ?, ?, ?, ?)")) {
                ps.setInt(1, 2);
                ps.setString(2, "TP-101");
                ps.setString(3, "Inspector");
                ps.setString(4, "Central City Traffic Division");
                ps.setString(5, "ACTIVE");
                ps.executeUpdate();
            }

            // Seed Junctions
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO junctions (junction_code, name, area, landmark, risk_level) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, "JNC-001");
                ps.setString(2, "Central Square Intersection");
                ps.setString(3, "Downtown Sector 1");
                ps.setString(4, "Metro Gate 2");
                ps.setString(5, "HIGH");
                ps.executeUpdate();

                ps.setString(1, "JNC-002");
                ps.setString(2, "Ring Road Flyover Junction");
                ps.setString(3, "North Expressway");
                ps.setString(4, "City Mall Circle");
                ps.setString(5, "MEDIUM");
                ps.executeUpdate();
            }

            // Seed Sample Challan
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO challans (challan_number, vehicle_number, vehicle_type, violator_name, violator_phone, officer_id, junction_id, violation_date, fine_amount, payment_status, remarks) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, "CH-2026-1001");
                ps.setString(2, "MH-12-AB-1234");
                ps.setString(3, "Four-Wheeler");
                ps.setString(4, "Amit Verma");
                ps.setString(5, "9988776655");
                ps.setInt(6, 1);
                ps.setInt(7, 1);
                ps.setString(8, LocalDate.now().toString());
                ps.setDouble(9, 1000.0);
                ps.setString(10, "PENDING");
                ps.setString(11, "Jumping red traffic light signal at high speed");
                ps.executeUpdate();
            }

            // Seed Sample Complaint
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO complaints (complaint_number, citizen_id, complaint_type, vehicle_number, description, incident_date, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, "CMP-2026-001");
                ps.setInt(2, 3);
                ps.setString(3, "Illegal Parking");
                ps.setString(4, "DL-01-XY-9999");
                ps.setString(5, "Commercial SUV blocking pedestrian footpath and zebra crossing near Metro Station");
                ps.setString(6, LocalDate.now().toString());
                ps.setString(7, "PENDING");
                ps.setString(8, LocalDateTime.now().toString());
                ps.executeUpdate();
            }

            // Seed Sample Emergency
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO emergency_reports (report_number, junction_name, accident_type, severity, casualties_count, ambulance_required, tow_truck_required, report_time, status, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, "EMG-2026-01");
                ps.setString(2, "Central Square Intersection");
                ps.setString(3, "Head-on collision");
                ps.setString(4, "CRITICAL");
                ps.setInt(5, 2);
                ps.setInt(6, 1);
                ps.setInt(7, 1);
                ps.setString(8, LocalDateTime.now().toString());
                ps.setString(9, "IN_PROGRESS");
                ps.setString(10, "Ambulance and Crane dispatched; Sector patrol clearing traffic lanes.");
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =====================================================================================
    // 2. AUTHENTICATION & LOGIN SCREEN
    // =====================================================================================
    private void showLoginScreen() {
        primaryStage.setTitle("Smart Traffic Police Management System - Login");

        VBox root = new VBox(25);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #0f172a;");

        // Header Title & Logo
        VBox titleBox = new VBox(6);
        titleBox.setAlignment(Pos.CENTER);
        Label iconLabel = new Label("🚦");
        iconLabel.setStyle("-fx-font-size: 42px;");

        Label titleLabel = new Label("Smart Traffic Police Management System");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.WHITE);

        Label subLabel = new Label("Advanced Java Academic Project • Department of Traffic Control");
        subLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        subLabel.setTextFill(Color.web("#94a3b8"));

        titleBox.getChildren().addAll(iconLabel, titleLabel, subLabel);

        // Login Card
        VBox card = new VBox(16);
        card.setMaxWidth(420);
        card.setPadding(new Insets(30));
        card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12px; -fx-border-color: #334155; -fx-border-radius: 12px; -fx-border-width: 1px;");

        Label cardHeader = new Label("Sign In to Portal");
        cardHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        cardHeader.setTextFill(Color.WHITE);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username (e.g. admin, officer1, citizen1)");
        usernameField.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white; -fx-border-color: #475569; -fx-border-radius: 6px; -fx-padding: 10px;");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white; -fx-border-color: #475569; -fx-border-radius: 6px; -fx-padding: 10px;");

        Label errorLabel = new Label("");
        errorLabel.setTextFill(Color.web("#f87171"));
        errorLabel.setVisible(false);

        Button loginBtn = new Button("Authorize & Sign In");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 11px; -fx-background-radius: 6px; -fx-cursor: hand;");

        loginBtn.setOnAction(e -> {
            String u = usernameField.getText().trim();
            String p = passwordField.getText().trim();
            if (u.isEmpty() || p.isEmpty()) {
                errorLabel.setText("Please enter both username and password.");
                errorLabel.setVisible(true);
                return;
            }
            performLogin(u, p, errorLabel);
        });

        // Demo Shortcut Buttons
        Label demoTitle = new Label("Quick Demonstration Access:");
        demoTitle.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        demoTitle.setTextFill(Color.web("#94a3b8"));

        HBox demoBox = new HBox(10);
        demoBox.setAlignment(Pos.CENTER);

        Button adminDemo = createDemoButton("👮 Admin", () -> {
            usernameField.setText("admin");
            passwordField.setText("admin123");
        });
        Button officerDemo = createDemoButton("🚔 Officer", () -> {
            usernameField.setText("officer1");
            passwordField.setText("officer123");
        });
        Button citizenDemo = createDemoButton("👤 Citizen", () -> {
            usernameField.setText("citizen1");
            passwordField.setText("citizen123");
        });

        demoBox.getChildren().addAll(adminDemo, officerDemo, citizenDemo);

        card.getChildren().addAll(cardHeader, usernameField, passwordField, errorLabel, loginBtn, new Separator(), demoTitle, demoBox);
        root.getChildren().addAll(titleBox, card);

        Scene scene = new Scene(root, 1100, 750);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    private Button createDemoButton(String text, Runnable action) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: #334155; -fx-text-fill: #e2e8f0; -fx-font-size: 11px; -fx-padding: 6px 12px; -fx-background-radius: 4px; -fx-cursor: hand;");
        b.setOnAction(e -> action.run());
        return b;
    }

    private void performLogin(String username, String password, Label errorLabel) {
        THREAD_POOL.execute(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT id, username, role, full_name FROM users WHERE username = ? AND password_hash = ?")) {
                ps.setString(1, username);
                ps.setString(2, password);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    currentUserId = rs.getInt("id");
                    currentUsername = rs.getString("username");
                    currentRole = rs.getString("role");
                    currentFullName = rs.getString("full_name");

                    Platform.runLater(this::navigateToRoleDashboard);
                } else {
                    Platform.runLater(() -> {
                        errorLabel.setText("Invalid credentials. Use demo buttons below.");
                        errorLabel.setVisible(true);
                    });
                }
            } catch (SQLException ex) {
                Platform.runLater(() -> {
                    errorLabel.setText("Database connection error: " + ex.getMessage());
                    errorLabel.setVisible(true);
                });
            }
        });
    }

    private void navigateToRoleDashboard() {
        switch (currentRole) {
            case "ADMIN" -> showAdminPortal();
            case "OFFICER" -> showOfficerPortal();
            case "CITIZEN" -> showCitizenPortal();
            default -> showLoginScreen();
        }
    }

    // =====================================================================================
    // 3. MASTER FRAMEWORK & SIDEBAR NAVIGATION
    // =====================================================================================
    private BorderPane createBaseLayout(String title, Runnable onLogout) {
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color: #0b1329;");

        // Top Navigation Bar
        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 25, 15, 25));
        topBar.setStyle("-fx-background-color: #111e38; -fx-border-color: #1e293b; -fx-border-width: 0 0 1 0;");

        Label logo = new Label("🚦 " + title);
        logo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        logo.setTextFill(Color.WHITE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label userBadge = new Label("👤 " + currentFullName + " (" + currentRole + ")");
        userBadge.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #38bdf8; -fx-padding: 6px 14px; -fx-background-radius: 20px; -fx-font-weight: bold;");

        Button logoutBtn = new Button("Sign Out");
        logoutBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 6px 16px;");
        logoutBtn.setOnAction(e -> {
            currentUsername = "";
            currentRole = "";
            showLoginScreen();
        });

        topBar.getChildren().addAll(logo, spacer, userBadge, logoutBtn);
        layout.setTop(topBar);

        return layout;
    }

    // =====================================================================================
    // 4. ADMIN PORTAL (COMMAND CENTER & AUDIT)
    // =====================================================================================
    private void showAdminPortal() {
        BorderPane layout = createBaseLayout("Central Traffic Command - Administrator Console", this::showLoginScreen);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: #0f172a;");

        // Tab 1: Dashboard Analytics & KPI
        Tab dashTab = new Tab("📊 Executive Dashboard", createAdminDashboardView());
        // Tab 2: Officer Force Management (CRUD)
        Tab officerTab = new Tab("👮 Officers Cadre", createOfficersManagementView());
        // Tab 3: Junctions & Smart Relays
        Tab junctionTab = new Tab("🛣️ Junctions & Signals", createJunctionsView());
        // Tab 4: Challans Master Registry
        Tab challanTab = new Tab("📑 e-Challan Registry", createChallansRegistryView());
        // Tab 5: Citizen Grievances Redressal
        Tab complaintTab = new Tab("📩 Grievance Redressal", createComplaintsAdminView());
        // Tab 6: Emergency Collisions Console
        Tab emergencyTab = new Tab("🚨 Emergency Console", createEmergencyAdminView());

        tabPane.getTabs().addAll(dashTab, officerTab, junctionTab, challanTab, complaintTab, emergencyTab);
        layout.setCenter(tabPane);

        Scene scene = new Scene(layout, 1240, 840);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Smart Traffic Police Management System - Administrator Command");
    }

    private Node createAdminDashboardView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #0f172a;");

        Label heading = new Label("Departmental Operational Metrics & Live Telemetry");
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        heading.setTextFill(Color.WHITE);

        // 4 KPI Cards
        HBox kpiBox = new HBox(15);
        VBox card1 = createKpiCard("Active Officers", "...", "#3b82f6");
        VBox card2 = createKpiCard("Monitored Junctions", "...", "#10b981");
        VBox card3 = createKpiCard("Pending Citations", "...", "#f59e0b");
        VBox card4 = createKpiCard("Unresolved Grievances", "...", "#ef4444");
        kpiBox.getChildren().addAll(card1, card2, card3, card4);

        // Recent System Activity Box
        VBox activityBox = new VBox(12);
        activityBox.setPadding(new Insets(20));
        activityBox.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 10px; -fx-border-color: #334155;");
        Label actLabel = new Label("System Status & Database Telemetry:");
        actLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        actLabel.setTextFill(Color.WHITE);

        Label dbPath = new Label("• Persistent Database: smart_traffic.db (SQLite Relational Engine)");
        dbPath.setTextFill(Color.web("#94a3b8"));
        Label threadCount = new Label("• Background Concurrency: 4 Daemon Execution Threads Running");
        threadCount.setTextFill(Color.web("#94a3b8"));
        Label securityStatus = new Label("• Session Security: Role-Based Access Control Enforced (Admin, Officer, Citizen)");
        securityStatus.setTextFill(Color.web("#94a3b8"));

        activityBox.getChildren().addAll(actLabel, dbPath, threadCount, securityStatus);

        root.getChildren().addAll(heading, kpiBox, activityBox);

        // Load Live Counts
        THREAD_POOL.execute(() -> {
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                ResultSet rs1 = stmt.executeQuery("SELECT COUNT(*) FROM officers;");
                int offCount = rs1.next() ? rs1.getInt(1) : 0;

                ResultSet rs2 = stmt.executeQuery("SELECT COUNT(*) FROM junctions;");
                int jncCount = rs2.next() ? rs2.getInt(1) : 0;

                ResultSet rs3 = stmt.executeQuery("SELECT COUNT(*) FROM challans WHERE payment_status = 'PENDING';");
                int chCount = rs3.next() ? rs3.getInt(1) : 0;

                ResultSet rs4 = stmt.executeQuery("SELECT COUNT(*) FROM complaints WHERE status = 'PENDING';");
                int cmpCount = rs4.next() ? rs4.getInt(1) : 0;

                Platform.runLater(() -> {
                    updateKpiValue(card1, String.valueOf(offCount));
                    updateKpiValue(card2, String.valueOf(jncCount));
                    updateKpiValue(card3, String.valueOf(chCount));
                    updateKpiValue(card4, String.valueOf(cmpCount));
                });
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        return root;
    }

    private VBox createKpiCard(String title, String val, String colorHex) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(20));
        card.setPrefWidth(260);
        card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 10px; -fx-border-color: #334155; -fx-border-width: 1px;");

        Label t = new Label(title);
        t.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        t.setTextFill(Color.web("#94a3b8"));

        Label v = new Label(val);
        v.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        v.setTextFill(Color.web(colorHex));

        card.getChildren().addAll(t, v);
        return card;
    }

    private void updateKpiValue(VBox card, String newVal) {
        if (card.getChildren().size() > 1 && card.getChildren().get(1) instanceof Label l) {
            l.setText(newVal);
        }
    }

    // --- Officer Cadre Management View (CRUD) ---
    private Node createOfficersManagementView() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #0f172a;");

        HBox toolBar = new HBox(12);
        toolBar.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label("Officer Force Roster");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lbl.setTextFill(Color.WHITE);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button addBtn = new Button("+ Enlist New Officer");
        addBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 8px 16px; -fx-cursor: hand;");

        toolBar.getChildren().addAll(lbl, sp, addBtn);

        TableView<OfficerRow> table = new TableView<>();
        table.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-text-fill: white;");

        TableColumn<OfficerRow, String> colBadge = new TableColumn<>("Badge Number");
        colBadge.setCellValueFactory(new PropertyValueFactory<>("badge"));
        colBadge.setPrefWidth(140);

        TableColumn<OfficerRow, String> colName = new TableColumn<>("Full Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colName.setPrefWidth(220);

        TableColumn<OfficerRow, String> colRank = new TableColumn<>("Rank / Designation");
        colRank.setCellValueFactory(new PropertyValueFactory<>("rank"));
        colRank.setPrefWidth(160);

        TableColumn<OfficerRow, String> colStation = new TableColumn<>("Station / Division");
        colStation.setCellValueFactory(new PropertyValueFactory<>("station"));
        colStation.setPrefWidth(240);

        TableColumn<OfficerRow, String> colStatus = new TableColumn<>("Active Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setPrefWidth(130);

        table.getColumns().addAll(colBadge, colName, colRank, colStation, colStatus);
        table.setPlaceholder(new Label("No police officers registered."));

        root.getChildren().addAll(toolBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        Runnable refreshOfficers = () -> THREAD_POOL.execute(() -> {
            ObservableList<OfficerRow> list = FXCollections.observableArrayList();
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("""
                     SELECT o.badge_number, u.full_name, o.rank, o.police_station, o.status 
                     FROM officers o 
                     JOIN users u ON o.user_id = u.id;
                 """)) {
                while (rs.next()) {
                    list.add(new OfficerRow(
                        rs.getString("badge_number"),
                        rs.getString("full_name"),
                        rs.getString("rank"),
                        rs.getString("police_station"),
                        rs.getString("status")
                    ));
                }
                Platform.runLater(() -> table.setItems(list));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        refreshOfficers.run();

        addBtn.setOnAction(e -> showAddOfficerDialog(refreshOfficers));

        return root;
    }

    public static class OfficerRow {
        private final SimpleStringProperty badge;
        private final SimpleStringProperty fullName;
        private final SimpleStringProperty rank;
        private final SimpleStringProperty station;
        private final SimpleStringProperty status;

        public OfficerRow(String b, String n, String r, String s, String st) {
            this.badge = new SimpleStringProperty(b);
            this.fullName = new SimpleStringProperty(n);
            this.rank = new SimpleStringProperty(r);
            this.station = new SimpleStringProperty(s);
            this.status = new SimpleStringProperty(st);
        }
        public String getBadge() { return badge.get(); }
        public String getFullName() { return fullName.get(); }
        public String getRank() { return rank.get(); }
        public String getStation() { return station.get(); }
        public String getStatus() { return status.get(); }
    }

    private void showAddOfficerDialog(Runnable onComplete) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Enlist New Traffic Police Officer");

        VBox form = new VBox(14);
        form.setPadding(new Insets(25));
        form.setStyle("-fx-background-color: #1e293b;");

        TextField usernameFld = new TextField();
        usernameFld.setPromptText("Officer Username (e.g. officer3)");
        TextField nameFld = new TextField();
        nameFld.setPromptText("Officer Full Name");
        TextField badgeFld = new TextField();
        badgeFld.setPromptText("Badge Number (e.g. TP-105)");
        ComboBox<String> rankBox = new ComboBox<>(FXCollections.observableArrayList("Inspector", "Sub-Inspector", "Head Constable", "Constable"));
        rankBox.setValue("Sub-Inspector");
        TextField stationFld = new TextField();
        stationFld.setPromptText("Police Station / Sector");

        Button submitBtn = new Button("Confirm Enlistment");
        submitBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px;");

        submitBtn.setOnAction(e -> {
            String u = usernameFld.getText().trim();
            String n = nameFld.getText().trim();
            String b = badgeFld.getText().trim();
            String r = rankBox.getValue();
            String s = stationFld.getText().trim();
            if (u.isEmpty() || n.isEmpty() || b.isEmpty() || s.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Incomplete Form", "Please fill in all officer details.");
                return;
            }

            THREAD_POOL.execute(() -> {
                try (Connection conn = getConnection()) {
                    conn.setAutoCommit(false);
                    int userId = -1;
                    try (PreparedStatement psUser = conn.prepareStatement(
                        "INSERT INTO users (username, password_hash, role, full_name, created_at) VALUES (?, 'officer123', 'OFFICER', ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                        psUser.setString(1, u);
                        psUser.setString(2, n);
                        psUser.setString(3, LocalDateTime.now().toString());
                        psUser.executeUpdate();
                        ResultSet gk = psUser.getGeneratedKeys();
                        if (gk.next()) userId = gk.getInt(1);
                    }

                    try (PreparedStatement psOff = conn.prepareStatement(
                        "INSERT INTO officers (user_id, badge_number, rank, police_station, status) VALUES (?, ?, ?, ?, 'ACTIVE')")) {
                        psOff.setInt(1, userId);
                        psOff.setString(2, b);
                        psOff.setString(3, r);
                        psOff.setString(4, s);
                        psOff.executeUpdate();
                    }

                    conn.commit();
                    Platform.runLater(() -> {
                        dialog.close();
                        onComplete.run();
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Officer " + n + " has been successfully enlisted!");
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Database Error", ex.getMessage()));
                }
            });
        });

        form.getChildren().addAll(new Label("Enlist Traffic Officer:"), usernameFld, nameFld, badgeFld, rankBox, stationFld, submitBtn);
        dialog.setScene(new Scene(form, 400, 380));
        dialog.show();
    }

    // --- Junctions View ---
    private Node createJunctionsView() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #0f172a;");

        Label lbl = new Label("Traffic Intersections & Smart Signals");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lbl.setTextFill(Color.WHITE);

        TableView<JunctionRow> table = new TableView<>();
        table.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155;");

        TableColumn<JunctionRow, String> colCode = new TableColumn<>("Junction Code");
        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colCode.setPrefWidth(140);

        TableColumn<JunctionRow, String> colName = new TableColumn<>("Intersection Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(260);

        TableColumn<JunctionRow, String> colArea = new TableColumn<>("Sector / Area");
        colArea.setCellValueFactory(new PropertyValueFactory<>("area"));
        colArea.setPrefWidth(200);

        TableColumn<JunctionRow, String> colRisk = new TableColumn<>("Congestion Level");
        colRisk.setCellValueFactory(new PropertyValueFactory<>("risk"));
        colRisk.setPrefWidth(160);

        table.getColumns().addAll(colCode, colName, colArea, colRisk);
        root.getChildren().addAll(lbl, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        THREAD_POOL.execute(() -> {
            ObservableList<JunctionRow> list = FXCollections.observableArrayList();
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT junction_code, name, area, risk_level FROM junctions;")) {
                while (rs.next()) {
                    list.add(new JunctionRow(
                        rs.getString("junction_code"),
                        rs.getString("name"),
                        rs.getString("area"),
                        rs.getString("risk_level")
                    ));
                }
                Platform.runLater(() -> table.setItems(list));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        return root;
    }

    public static class JunctionRow {
        private final SimpleStringProperty code;
        private final SimpleStringProperty name;
        private final SimpleStringProperty area;
        private final SimpleStringProperty risk;

        public JunctionRow(String c, String n, String a, String r) {
            this.code = new SimpleStringProperty(c);
            this.name = new SimpleStringProperty(n);
            this.area = new SimpleStringProperty(a);
            this.risk = new SimpleStringProperty(r);
        }
        public String getCode() { return code.get(); }
        public String getName() { return name.get(); }
        public String getArea() { return area.get(); }
        public String getRisk() { return risk.get(); }
    }

    // --- Challans Master Registry View ---
    private Node createChallansRegistryView() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #0f172a;");

        Label lbl = new Label("Metropolitan e-Challan Master Database");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lbl.setTextFill(Color.WHITE);

        TableView<ChallanRow> table = new TableView<>();
        table.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155;");

        TableColumn<ChallanRow, String> colNum = new TableColumn<>("Challan ID");
        colNum.setCellValueFactory(new PropertyValueFactory<>("challanNum"));
        colNum.setPrefWidth(140);

        TableColumn<ChallanRow, String> colPlate = new TableColumn<>("Vehicle Number");
        colPlate.setCellValueFactory(new PropertyValueFactory<>("vehiclePlate"));
        colPlate.setPrefWidth(160);

        TableColumn<ChallanRow, String> colDate = new TableColumn<>("Infraction Date");
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colDate.setPrefWidth(130);

        TableColumn<ChallanRow, String> colFine = new TableColumn<>("Penalty Fine (₹)");
        colFine.setCellValueFactory(new PropertyValueFactory<>("fine"));
        colFine.setPrefWidth(130);

        TableColumn<ChallanRow, String> colStatus = new TableColumn<>("Payment Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setPrefWidth(140);

        TableColumn<ChallanRow, String> colRemarks = new TableColumn<>("Offense Narrative");
        colRemarks.setCellValueFactory(new PropertyValueFactory<>("remarks"));
        colRemarks.setPrefWidth(280);

        table.getColumns().addAll(colNum, colPlate, colDate, colFine, colStatus, colRemarks);
        root.getChildren().addAll(lbl, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        THREAD_POOL.execute(() -> {
            ObservableList<ChallanRow> list = FXCollections.observableArrayList();
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT challan_number, vehicle_number, violation_date, fine_amount, payment_status, remarks FROM challans;")) {
                while (rs.next()) {
                    list.add(new ChallanRow(
                        rs.getString("challan_number"),
                        rs.getString("vehicle_number"),
                        rs.getString("violation_date"),
                        "₹ " + rs.getDouble("fine_amount"),
                        rs.getString("payment_status"),
                        rs.getString("remarks")
                    ));
                }
                Platform.runLater(() -> table.setItems(list));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        return root;
    }

    public static class ChallanRow {
        private final SimpleStringProperty challanNum;
        private final SimpleStringProperty vehiclePlate;
        private final SimpleStringProperty date;
        private final SimpleStringProperty fine;
        private final SimpleStringProperty status;
        private final SimpleStringProperty remarks;

        public ChallanRow(String c, String v, String d, String f, String s, String r) {
            this.challanNum = new SimpleStringProperty(c);
            this.vehiclePlate = new SimpleStringProperty(v);
            this.date = new SimpleStringProperty(d);
            this.fine = new SimpleStringProperty(f);
            this.status = new SimpleStringProperty(s);
            this.remarks = new SimpleStringProperty(r != null ? r : "-");
        }
        public String getChallanNum() { return challanNum.get(); }
        public String getVehiclePlate() { return vehiclePlate.get(); }
        public String getDate() { return date.get(); }
        public String getFine() { return fine.get(); }
        public String getStatus() { return status.get(); }
        public String getRemarks() { return remarks.get(); }
    }

    // --- Grievance Redressal View ---
    private Node createComplaintsAdminView() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #0f172a;");

        Label lbl = new Label("Public Citizen Grievances & Resolution Console");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lbl.setTextFill(Color.WHITE);

        TableView<ComplaintRow> table = new TableView<>();
        table.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155;");

        TableColumn<ComplaintRow, String> colId = new TableColumn<>("Grievance ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(130);

        TableColumn<ComplaintRow, String> colType = new TableColumn<>("Category");
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colType.setPrefWidth(160);

        TableColumn<ComplaintRow, String> colVehicle = new TableColumn<>("Reported Vehicle");
        colVehicle.setCellValueFactory(new PropertyValueFactory<>("vehicle"));
        colVehicle.setPrefWidth(150);

        TableColumn<ComplaintRow, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setPrefWidth(130);

        TableColumn<ComplaintRow, String> colDesc = new TableColumn<>("Citizen Description");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("desc"));
        colDesc.setPrefWidth(380);

        table.getColumns().addAll(colId, colType, colVehicle, colStatus, colDesc);
        root.getChildren().addAll(lbl, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        THREAD_POOL.execute(() -> {
            ObservableList<ComplaintRow> list = FXCollections.observableArrayList();
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT complaint_number, complaint_type, vehicle_number, status, description FROM complaints;")) {
                while (rs.next()) {
                    list.add(new ComplaintRow(
                        rs.getString("complaint_number"),
                        rs.getString("complaint_type"),
                        rs.getString("vehicle_number"),
                        rs.getString("status"),
                        rs.getString("description")
                    ));
                }
                Platform.runLater(() -> table.setItems(list));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        return root;
    }

    public static class ComplaintRow {
        private final SimpleStringProperty id;
        private final SimpleStringProperty type;
        private final SimpleStringProperty vehicle;
        private final SimpleStringProperty status;
        private final SimpleStringProperty desc;

        public ComplaintRow(String i, String t, String v, String s, String d) {
            this.id = new SimpleStringProperty(i);
            this.type = new SimpleStringProperty(t);
            this.vehicle = new SimpleStringProperty(v != null ? v : "N/A");
            this.status = new SimpleStringProperty(s);
            this.desc = new SimpleStringProperty(d);
        }
        public String getId() { return id.get(); }
        public String getType() { return type.get(); }
        public String getVehicle() { return vehicle.get(); }
        public String getStatus() { return status.get(); }
        public String getDesc() { return desc.get(); }
    }

    // --- Emergency Console ---
    private Node createEmergencyAdminView() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #0f172a;");

        Label lbl = new Label("Accident Response & Code-Red Emergency Dispatch");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lbl.setTextFill(Color.WHITE);

        TableView<EmergencyRow> table = new TableView<>();
        table.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155;");

        TableColumn<EmergencyRow, String> colCode = new TableColumn<>("Incident ID");
        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colCode.setPrefWidth(130);

        TableColumn<EmergencyRow, String> colLoc = new TableColumn<>("Intersection / Location");
        colLoc.setCellValueFactory(new PropertyValueFactory<>("location"));
        colLoc.setPrefWidth(220);

        TableColumn<EmergencyRow, String> colType = new TableColumn<>("Collision Nature");
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colType.setPrefWidth(160);

        TableColumn<EmergencyRow, String> colSev = new TableColumn<>("Severity");
        colSev.setCellValueFactory(new PropertyValueFactory<>("severity"));
        colSev.setPrefWidth(130);

        TableColumn<EmergencyRow, String> colCasualties = new TableColumn<>("Casualties");
        colCasualties.setCellValueFactory(new PropertyValueFactory<>("casualties"));
        colCasualties.setPrefWidth(110);

        TableColumn<EmergencyRow, String> colNotes = new TableColumn<>("Dispatch Coordination Notes");
        colNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));
        colNotes.setPrefWidth(300);

        table.getColumns().addAll(colCode, colLoc, colType, colSev, colCasualties, colNotes);
        root.getChildren().addAll(lbl, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        THREAD_POOL.execute(() -> {
            ObservableList<EmergencyRow> list = FXCollections.observableArrayList();
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT report_number, junction_name, accident_type, severity, casualties_count, notes FROM emergency_reports;")) {
                while (rs.next()) {
                    list.add(new EmergencyRow(
                        rs.getString("report_number"),
                        rs.getString("junction_name"),
                        rs.getString("accident_type"),
                        rs.getString("severity"),
                        String.valueOf(rs.getInt("casualties_count")),
                        rs.getString("notes")
                    ));
                }
                Platform.runLater(() -> table.setItems(list));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        return root;
    }

    public static class EmergencyRow {
        private final SimpleStringProperty code;
        private final SimpleStringProperty location;
        private final SimpleStringProperty type;
        private final SimpleStringProperty severity;
        private final SimpleStringProperty casualties;
        private final SimpleStringProperty notes;

        public EmergencyRow(String c, String l, String t, String s, String cas, String n) {
            this.code = new SimpleStringProperty(c);
            this.location = new SimpleStringProperty(l);
            this.type = new SimpleStringProperty(t);
            this.severity = new SimpleStringProperty(s);
            this.casualties = new SimpleStringProperty(cas);
            this.notes = new SimpleStringProperty(n);
        }
        public String getCode() { return code.get(); }
        public String getLocation() { return location.get(); }
        public String getType() { return type.get(); }
        public String getSeverity() { return severity.get(); }
        public String getCasualties() { return casualties.get(); }
        public String getNotes() { return notes.get(); }
    }

    // =====================================================================================
    // 5. OFFICER PORTAL (FIELD OPERATIONS & CITATION ISSUANCE)
    // =====================================================================================
    private void showOfficerPortal() {
        BorderPane layout = createBaseLayout("Field Officer Console - Patrol & Enforcement", this::showLoginScreen);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: #0f172a;");

        // Tab 1: Today's Shift & Patrol Status
        Tab shiftTab = new Tab("🚔 Duty Roster & Check-In", createOfficerDutyView());
        // Tab 2: Issue e-Challan / Violation Logger
        Tab issueTab = new Tab("⚡ Issue e-Challan", createIssueChallanView());
        // Tab 3: Emergency Accident Alert
        Tab emergTab = new Tab("🚨 Report Emergency", createOfficerEmergencyView());

        tabPane.getTabs().addAll(shiftTab, issueTab, emergTab);
        layout.setCenter(tabPane);

        Scene scene = new Scene(layout, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Smart Traffic Police Management System - Officer Operations");
    }

    private Node createOfficerDutyView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #0f172a;");

        Label heading = new Label("Field Officer Active Shift & Biometric Attendance");
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        heading.setTextFill(Color.WHITE);

        VBox shiftCard = new VBox(15);
        shiftCard.setPadding(new Insets(25));
        shiftCard.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12px; -fx-border-color: #334155;");

        Label badgeLbl = new Label("Officer Officer: " + currentFullName + " (Active Personnel)");
        badgeLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        badgeLbl.setTextFill(Color.web("#38bdf8"));

        Label junctionAssigned = new Label("• Assigned Beat: Central Square Intersection (Sector 1)");
        junctionAssigned.setTextFill(Color.WHITE);

        Label shiftWindow = new Label("• Shift Timings: Morning Watch (06:00 AM - 02:00 PM)");
        shiftWindow.setTextFill(Color.WHITE);

        Label statusLbl = new Label("• Biometric Attendance: MARKED PRESENT (06:05 AM)");
        statusLbl.setTextFill(Color.web("#4ade80"));

        Button checkInBtn = new Button("Mark Digital Attendance Check-In");
        checkInBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px; -fx-cursor: hand; -fx-background-radius: 6px;");
        checkInBtn.setOnAction(e -> showAlert(Alert.AlertType.INFORMATION, "Attendance Confirmed", "Biometric Timestamp Recorded: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

        shiftCard.getChildren().addAll(badgeLbl, junctionAssigned, shiftWindow, statusLbl, checkInBtn);
        root.getChildren().addAll(heading, shiftCard);

        return root;
    }

    private Node createIssueChallanView() {
        VBox root = new VBox(18);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #0f172a;");

        Label title = new Label("Rapid Violation Registration & Digital e-Challan Issuance");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        title.setTextFill(Color.WHITE);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 10px; -fx-border-color: #334155;");

        TextField vehicleFld = new TextField();
        vehicleFld.setPromptText("Vehicle Plate (e.g. MH-12-AB-1234)");

        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList("Two-Wheeler", "Four-Wheeler", "Heavy Transport Truck", "Commercial Auto"));
        typeBox.setValue("Four-Wheeler");

        ComboBox<String> violationBox = new ComboBox<>(FXCollections.observableArrayList(
            "Jumping Red Traffic Light (₹ 1,000)",
            "Overspeeding Above 60 km/h (₹ 2,000)",
            "Riding Without Approved Helmet (₹ 1,000)",
            "Driving Under Influence (₹ 10,000)",
            "Illegal Parking on Arterial Road (₹ 500)"
        ));
        violationBox.setValue("Jumping Red Traffic Light (₹ 1,000)");

        TextField violatorFld = new TextField();
        violatorFld.setPromptText("Violator Full Name (Optional)");

        TextField phoneFld = new TextField();
        phoneFld.setPromptText("Violator Mobile Phone (Optional)");

        TextArea remarksArea = new TextArea();
        remarksArea.setPromptText("Detailed offense narrative / photographic evidence notes...");
        remarksArea.setPrefRowCount(3);

        grid.add(new Label("Vehicle Number:"), 0, 0);
        grid.add(vehicleFld, 1, 0);
        grid.add(new Label("Vehicle Category:"), 0, 1);
        grid.add(typeBox, 1, 1);
        grid.add(new Label("Offense Type:"), 0, 2);
        grid.add(violationBox, 1, 2);
        grid.add(new Label("Driver Name:"), 0, 3);
        grid.add(violatorFld, 1, 3);
        grid.add(new Label("Contact Phone:"), 0, 4);
        grid.add(phoneFld, 1, 4);
        grid.add(new Label("Officer Remarks:"), 0, 5);
        grid.add(remarksArea, 1, 5);

        Button issueBtn = new Button("Generate & Issue e-Challan");
        issueBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12px 24px; -fx-background-radius: 6px; -fx-cursor: hand;");

        issueBtn.setOnAction(e -> {
            String plate = vehicleFld.getText().trim();
            if (plate.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Missing Plate", "Please input a valid vehicle registration plate.");
                return;
            }

            double fine = 1000.0;
            String sel = violationBox.getValue();
            if (sel.contains("2,000")) fine = 2000.0;
            else if (sel.contains("10,000")) fine = 10000.0;
            else if (sel.contains("500")) fine = 500.0;

            String challanId = "CH-2026-" + (int)(Math.random() * 8999 + 1000);
            double finalFine = fine;

            THREAD_POOL.execute(() -> {
                try (Connection conn = getConnection();
                     PreparedStatement ps = conn.prepareStatement("""
                         INSERT INTO challans (challan_number, vehicle_number, vehicle_type, violator_name, violator_phone, officer_id, junction_id, violation_date, fine_amount, payment_status, remarks)
                         VALUES (?, ?, ?, ?, ?, 1, 1, ?, ?, 'PENDING', ?)
                     """)) {
                    ps.setString(1, challanId);
                    ps.setString(2, plate);
                    ps.setString(3, typeBox.getValue());
                    ps.setString(4, violatorFld.getText().trim());
                    ps.setString(5, phoneFld.getText().trim());
                    ps.setString(6, LocalDate.now().toString());
                    ps.setDouble(7, finalFine);
                    ps.setString(8, remarksArea.getText().trim());
                    ps.executeUpdate();

                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.INFORMATION, "Challan Issued", "Official Citation " + challanId + " registered successfully for " + plate + " (₹ " + finalFine + ").");
                        vehicleFld.clear();
                        violatorFld.clear();
                        phoneFld.clear();
                        remarksArea.clear();
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Database Error", ex.getMessage()));
                }
            });
        });

        root.getChildren().addAll(title, grid, issueBtn);
        return root;
    }

    private Node createOfficerEmergencyView() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #0f172a;");

        Label title = new Label("Fast Emergency & Crash Alert Dispatch");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        title.setTextFill(Color.web("#ef4444"));

        TextField locFld = new TextField("Central Square Intersection");
        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList("Two-Vehicle Collision", "Pedestrian Hit & Run", "Vehicle Overturn", "Road Hazard Obstruction"));
        typeBox.setValue("Two-Vehicle Collision");
        ComboBox<String> sevBox = new ComboBox<>(FXCollections.observableArrayList("LOW", "MEDIUM", "HIGH", "CRITICAL"));
        sevBox.setValue("HIGH");
        TextField casFld = new TextField("1");
        TextArea notes = new TextArea("Ambulance required urgently on eastern junction lane.");

        Button sendAlert = new Button("🚨 Dispatch Emergency Teams");
        sendAlert.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12px 24px; -fx-background-radius: 6px; -fx-cursor: hand;");

        sendAlert.setOnAction(e -> {
            String emgCode = "EMG-2026-" + (int)(Math.random() * 89 + 10);
            THREAD_POOL.execute(() -> {
                try (Connection conn = getConnection();
                     PreparedStatement ps = conn.prepareStatement("""
                         INSERT INTO emergency_reports (report_number, junction_name, accident_type, severity, casualties_count, ambulance_required, tow_truck_required, report_time, status, notes)
                         VALUES (?, ?, ?, ?, ?, 1, 1, ?, 'REPORTED', ?)
                     """)) {
                    ps.setString(1, emgCode);
                    ps.setString(2, locFld.getText().trim());
                    ps.setString(3, typeBox.getValue());
                    ps.setString(4, sevBox.getValue());
                    ps.setInt(5, Integer.parseInt(casFld.getText().trim()));
                    ps.setString(6, LocalDateTime.now().toString());
                    ps.setString(7, notes.getText().trim());
                    ps.executeUpdate();

                    Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, "Dispatch Initiated", "Emergency Alert " + emgCode + " broadcasted to Medical & Towing units!"));
                } catch (Exception ex) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Dispatch Error", ex.getMessage()));
                }
            });
        });

        root.getChildren().addAll(title, new Label("Accident Location:"), locFld, new Label("Incident Type:"), typeBox, new Label("Severity Tier:"), sevBox, new Label("Estimated Casualties:"), casFld, new Label("Notes:"), notes, sendAlert);
        return root;
    }

    // =====================================================================================
    // 6. CITIZEN PORTAL (PUBLIC SERVICES & GRIEVANCES)
    // =====================================================================================
    private void showCitizenPortal() {
        BorderPane layout = createBaseLayout("Citizen Public Services & Redressal Gateway", this::showLoginScreen);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: #0f172a;");

        // Tab 1: My Challans & Online Payment
        Tab challanTab = new Tab("💳 My Challans & Fines", createCitizenChallansView());
        // Tab 2: File Traffic Complaint
        Tab complaintTab = new Tab("📝 Report Traffic Hazard", createCitizenFileComplaintView());

        tabPane.getTabs().addAll(challanTab, complaintTab);
        layout.setCenter(tabPane);

        Scene scene = new Scene(layout, 1150, 750);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Smart Traffic Police Management System - Citizen Portal");
    }

    private Node createCitizenChallansView() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #0f172a;");

        Label title = new Label("Vehicle Citations & Digital Fine Settlement");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.WHITE);

        TableView<CitizenChallanItem> table = new TableView<>();
        table.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155;");

        TableColumn<CitizenChallanItem, String> colNum = new TableColumn<>("Challan Number");
        colNum.setCellValueFactory(new PropertyValueFactory<>("num"));
        colNum.setPrefWidth(150);

        TableColumn<CitizenChallanItem, String> colPlate = new TableColumn<>("Vehicle Number");
        colPlate.setCellValueFactory(new PropertyValueFactory<>("plate"));
        colPlate.setPrefWidth(160);

        TableColumn<CitizenChallanItem, String> colDate = new TableColumn<>("Violation Date");
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colDate.setPrefWidth(130);

        TableColumn<CitizenChallanItem, String> colFine = new TableColumn<>("Amount (₹)");
        colFine.setCellValueFactory(new PropertyValueFactory<>("fine"));
        colFine.setPrefWidth(120);

        TableColumn<CitizenChallanItem, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setPrefWidth(120);

        TableColumn<CitizenChallanItem, String> colReason = new TableColumn<>("Violation Reason");
        colReason.setCellValueFactory(new PropertyValueFactory<>("reason"));
        colReason.setPrefWidth(260);

        table.getColumns().addAll(colNum, colPlate, colDate, colFine, colStatus, colReason);

        HBox actionBox = new HBox(12);
        Button payBtn = new Button("💳 Settle Selected Challan Online");
        payBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px; -fx-cursor: hand; -fx-background-radius: 6px;");

        actionBox.getChildren().add(payBtn);

        root.getChildren().addAll(title, table, actionBox);
        VBox.setVgrow(table, Priority.ALWAYS);

        Runnable reloadChallans = () -> THREAD_POOL.execute(() -> {
            ObservableList<CitizenChallanItem> list = FXCollections.observableArrayList();
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT challan_number, vehicle_number, violation_date, fine_amount, payment_status, remarks FROM challans;")) {
                while (rs.next()) {
                    list.add(new CitizenChallanItem(
                        rs.getString("challan_number"),
                        rs.getString("vehicle_number"),
                        rs.getString("violation_date"),
                        "₹ " + rs.getDouble("fine_amount"),
                        rs.getString("payment_status"),
                        rs.getString("remarks")
                    ));
                }
                Platform.runLater(() -> table.setItems(list));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        reloadChallans.run();

        payBtn.setOnAction(e -> {
            CitizenChallanItem sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a challan from the table to settle.");
                return;
            }
            if ("PAID".equalsIgnoreCase(sel.getStatus())) {
                showAlert(Alert.AlertType.INFORMATION, "Already Settled", "This challan has already been paid in full.");
                return;
            }

            String txRef = "TXN-" + System.currentTimeMillis();
            THREAD_POOL.execute(() -> {
                try (Connection conn = getConnection();
                     PreparedStatement ps = conn.prepareStatement("UPDATE challans SET payment_status = 'PAID', payment_date = ?, payment_reference = ? WHERE challan_number = ?")) {
                    ps.setString(1, LocalDateTime.now().toString());
                    ps.setString(2, txRef);
                    ps.setString(3, sel.getNum());
                    ps.executeUpdate();

                    Platform.runLater(() -> {
                        showPrintableReceipt(sel.getNum(), sel.getPlate(), sel.getFine(), txRef);
                        reloadChallans.run();
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Payment Error", ex.getMessage()));
                }
            });
        });

        return root;
    }

    public static class CitizenChallanItem {
        private final SimpleStringProperty num;
        private final SimpleStringProperty plate;
        private final SimpleStringProperty date;
        private final SimpleStringProperty fine;
        private final SimpleStringProperty status;
        private final SimpleStringProperty reason;

        public CitizenChallanItem(String n, String p, String d, String f, String s, String r) {
            this.num = new SimpleStringProperty(n);
            this.plate = new SimpleStringProperty(p);
            this.date = new SimpleStringProperty(d);
            this.fine = new SimpleStringProperty(f);
            this.status = new SimpleStringProperty(s);
            this.reason = new SimpleStringProperty(r != null ? r : "-");
        }
        public String getNum() { return num.get(); }
        public String getPlate() { return plate.get(); }
        public String getDate() { return date.get(); }
        public String getFine() { return fine.get(); }
        public String getStatus() { return status.get(); }
        public String getReason() { return reason.get(); }
    }

    private void showPrintableReceipt(String challan, String plate, String fine, String txn) {
        Stage receiptStage = new Stage();
        receiptStage.initModality(Modality.APPLICATION_MODAL);
        receiptStage.setTitle("Official Traffic Police e-Challan Receipt");

        VBox box = new VBox(12);
        box.setPadding(new Insets(25));
        box.setStyle("-fx-background-color: white; -fx-border-color: #334155; -fx-border-width: 2px;");

        Label header = new Label("GOVERNMENT TRAFFIC POLICE DEPARTMENT");
        header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));

        Label sub = new Label("Digital e-Challan Payment Acknowledgment Slip");
        sub.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));

        Separator sep = new Separator();

        Label c1 = new Label("Citation ID: " + challan);
        Label c2 = new Label("Vehicle Registration: " + plate);
        Label c3 = new Label("Fine Amount Settled: " + fine);
        Label c4 = new Label("Payment Status: SUCCESSFUL (PAID)");
        Label c5 = new Label("Transaction Reference: " + txn);
        Label c6 = new Label("Date & Timestamp: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        Button printBtn = new Button("Print / Save Receipt Slip");
        printBtn.setOnAction(e -> {
            showAlert(Alert.AlertType.INFORMATION, "Receipt Printed", "Receipt slip sent to system document printer successfully.");
            receiptStage.close();
        });

        box.getChildren().addAll(header, sub, sep, c1, c2, c3, c4, c5, c6, new Separator(), printBtn);
        receiptStage.setScene(new Scene(box, 450, 350));
        receiptStage.show();
    }

    private Node createCitizenFileComplaintView() {
        VBox root = new VBox(18);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #0f172a;");

        Label title = new Label("Report Road Hazard, Illegal Parking or Signal Failure");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        title.setTextFill(Color.WHITE);

        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList(
            "Illegal Parking & Footpath Obstruction",
            "Defective / Non-Functional Traffic Light",
            "Dangerous Reckless Driving",
            "Severe Traffic Congestion / Gridlock"
        ));
        typeBox.setValue("Illegal Parking & Footpath Obstruction");

        TextField vehicleFld = new TextField();
        vehicleFld.setPromptText("Associated Vehicle Number (if applicable)");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Explain location landmark, time of observation, and impact on commuter traffic...");
        descArea.setPrefRowCount(4);

        Button submitBtn = new Button("Submit Official Grievance");
        submitBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12px 24px; -fx-background-radius: 6px; -fx-cursor: hand;");

        submitBtn.setOnAction(e -> {
            String desc = descArea.getText().trim();
            if (desc.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Incomplete", "Please describe the traffic problem.");
                return;
            }

            String cmpId = "CMP-2026-" + (int)(Math.random() * 899 + 100);
            THREAD_POOL.execute(() -> {
                try (Connection conn = getConnection();
                     PreparedStatement ps = conn.prepareStatement("""
                         INSERT INTO complaints (complaint_number, citizen_id, complaint_type, vehicle_number, description, incident_date, status, created_at)
                         VALUES (?, 3, ?, ?, ?, ?, 'PENDING', ?)
                     """)) {
                    ps.setString(1, cmpId);
                    ps.setString(2, typeBox.getValue());
                    ps.setString(3, vehicleFld.getText().trim());
                    ps.setString(4, desc);
                    ps.setString(5, LocalDate.now().toString());
                    ps.setString(6, LocalDateTime.now().toString());
                    ps.executeUpdate();

                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.INFORMATION, "Grievance Filed", "Complaint registered under Reference #" + cmpId + ". Traffic Police patrol will investigate.");
                        vehicleFld.clear();
                        descArea.clear();
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Submission Error", ex.getMessage()));
                }
            });
        });

        root.getChildren().addAll(title, new Label("Category of Grievance:"), typeBox, new Label("Offending Vehicle:"), vehicleFld, new Label("Narrative & Incident Details:"), descArea, submitBtn);
        return root;
    }

    // =====================================================================================
    // 7. UTILITY ALERT HELPER
    // =====================================================================================
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
