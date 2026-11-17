package com.smarttraffic.dao;

import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.util.PasswordUtil;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DatabaseManager initializes the SQLite database schema and manages JDBC connections.
 * Demonstrates JDBC connection handling, PreparedStatement, Statement, and Transactions.
 */
public class DatabaseManager {
    private static final String DB_FILE = "smart_traffic.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;
    private static DatabaseManager instance;

    private DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
            initDatabase();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        } catch (DatabaseException e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    private void initDatabase() throws DatabaseException {
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
                    latitude REAL,
                    longitude REAL,
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
                    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY(assigned_junction_id) REFERENCES junctions(id) ON DELETE SET NULL
                );
            """);

            // 4. Citizen Profile Extensions Table (for DL & default vehicle)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS citizen_profiles (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER UNIQUE NOT NULL,
                    dl_number TEXT,
                    vehicle_number TEXT,
                    address TEXT,
                    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                );
            """);

            // 5. Duty Assignments Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS duty_assignments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    officer_id INTEGER NOT NULL,
                    junction_id INTEGER NOT NULL,
                    shift TEXT NOT NULL,
                    duty_date TEXT NOT NULL,
                    start_time TEXT NOT NULL,
                    end_time TEXT NOT NULL,
                    status TEXT DEFAULT 'SCHEDULED',
                    notes TEXT,
                    FOREIGN KEY(officer_id) REFERENCES officers(id) ON DELETE CASCADE,
                    FOREIGN KEY(junction_id) REFERENCES junctions(id) ON DELETE CASCADE
                );
            """);

            // 6. Attendance Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS attendance (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    officer_id INTEGER NOT NULL,
                    duty_id INTEGER,
                    attendance_date TEXT NOT NULL,
                    check_in_time TEXT,
                    check_out_time TEXT,
                    status TEXT NOT NULL,
                    remarks TEXT,
                    FOREIGN KEY(officer_id) REFERENCES officers(id) ON DELETE CASCADE,
                    FOREIGN KEY(duty_id) REFERENCES duty_assignments(id) ON DELETE SET NULL
                );
            """);

            // 7. Violations Type Master Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS violations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    violation_code TEXT UNIQUE NOT NULL,
                    title TEXT NOT NULL,
                    category TEXT NOT NULL,
                    description TEXT,
                    standard_fine_amount REAL NOT NULL
                );
            """);

            // 8. Challans Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS challans (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    challan_number TEXT UNIQUE NOT NULL,
                    vehicle_number TEXT NOT NULL,
                    vehicle_type TEXT NOT NULL,
                    violator_name TEXT,
                    violator_phone TEXT,
                    officer_id INTEGER NOT NULL,
                    junction_id INTEGER NOT NULL,
                    violation_id INTEGER NOT NULL,
                    violation_date TEXT NOT NULL,
                    fine_amount REAL NOT NULL,
                    payment_status TEXT DEFAULT 'PENDING',
                    payment_date TEXT,
                    payment_reference TEXT,
                    evidence_file_path TEXT,
                    remarks TEXT,
                    FOREIGN KEY(officer_id) REFERENCES officers(id) ON DELETE RESTRICT,
                    FOREIGN KEY(junction_id) REFERENCES junctions(id) ON DELETE RESTRICT,
                    FOREIGN KEY(violation_id) REFERENCES violations(id) ON DELETE RESTRICT
                );
            """);

            // 9. Complaints Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS complaints (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    complaint_number TEXT UNIQUE NOT NULL,
                    citizen_id INTEGER NOT NULL,
                    junction_id INTEGER,
                    complaint_type TEXT NOT NULL,
                    vehicle_number TEXT,
                    description TEXT NOT NULL,
                    incident_date TEXT NOT NULL,
                    status TEXT DEFAULT 'PENDING',
                    assigned_officer_id INTEGER,
                    officer_action_notes TEXT,
                    evidence_file_path TEXT,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    FOREIGN KEY(citizen_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY(junction_id) REFERENCES junctions(id) ON DELETE SET NULL,
                    FOREIGN KEY(assigned_officer_id) REFERENCES officers(id) ON DELETE SET NULL
                );
            """);

            // 10. Emergency Reports Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS emergency_reports (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    report_number TEXT UNIQUE NOT NULL,
                    officer_id INTEGER,
                    junction_id INTEGER NOT NULL,
                    accident_type TEXT NOT NULL,
                    severity TEXT NOT NULL,
                    vehicles_involved INTEGER DEFAULT 1,
                    casualties_count INTEGER DEFAULT 0,
                    ambulance_required INTEGER DEFAULT 0,
                    tow_truck_required INTEGER DEFAULT 0,
                    road_blocked INTEGER DEFAULT 0,
                    report_time TEXT NOT NULL,
                    status TEXT DEFAULT 'REPORTED',
                    notes TEXT,
                    FOREIGN KEY(officer_id) REFERENCES officers(id) ON DELETE SET NULL,
                    FOREIGN KEY(junction_id) REFERENCES junctions(id) ON DELETE CASCADE
                );
            """);

            // 11. Evidence Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS evidence (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    entity_type TEXT NOT NULL,
                    entity_id INTEGER NOT NULL,
                    file_path TEXT NOT NULL,
                    file_name TEXT,
                    file_size INTEGER,
                    description TEXT,
                    uploaded_at TEXT NOT NULL
                );
            """);

            // Seed sample data if users table is empty
            seedInitialData(conn);

        } catch (SQLException e) {
            throw new DatabaseException("Failed to initialize database tables: " + e.getMessage(), e);
        }
    }

    private void seedInitialData(Connection conn) throws SQLException {
        try (Statement checkStmt = conn.createStatement();
             ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) FROM users;")) {
            if (rs.next() && rs.getInt(1) > 0) {
                // Already seeded
                return;
            }
        }

        System.out.println("Seeding initial demonstration data into SQLite database...");
        String now = LocalDateTime.now().toString();
        String today = LocalDate.now().toString();

        // 1. Seed Users (Admin, Officers, Citizens)
        String userSql = "INSERT INTO users (username, password_hash, role, full_name, email, phone, created_at) VALUES (?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(userSql)) {
            // Admin user (admin / admin123)
            pstmt.setString(1, "admin");
            pstmt.setString(2, PasswordUtil.hashPassword("admin123"));
            pstmt.setString(3, "ADMIN");
            pstmt.setString(4, "Chief Commissioner Arvind Rao");
            pstmt.setString(5, "admin@trafficpolice.gov.in");
            pstmt.setString(6, "+91 98765 43210");
            pstmt.setString(7, now);
            pstmt.executeUpdate();

            // Officer 1 (officer1 / officer123)
            pstmt.setString(1, "officer1");
            pstmt.setString(2, PasswordUtil.hashPassword("officer123"));
            pstmt.setString(3, "OFFICER");
            pstmt.setString(4, "Inspector Vikram Sharma");
            pstmt.setString(5, "vikram.sharma@trafficpolice.gov.in");
            pstmt.setString(6, "+91 98111 22334");
            pstmt.setString(7, now);
            pstmt.executeUpdate();

            // Officer 2 (officer2 / officer123)
            pstmt.setString(1, "officer2");
            pstmt.setString(2, PasswordUtil.hashPassword("officer123"));
            pstmt.setString(3, "OFFICER");
            pstmt.setString(4, "Sub-Inspector Priya Nair");
            pstmt.setString(5, "priya.nair@trafficpolice.gov.in");
            pstmt.setString(6, "+91 98222 33445");
            pstmt.setString(7, now);
            pstmt.executeUpdate();

            // Officer 3 (officer3 / officer123)
            pstmt.setString(1, "officer3");
            pstmt.setString(2, PasswordUtil.hashPassword("officer123"));
            pstmt.setString(3, "OFFICER");
            pstmt.setString(4, "Head Constable Rahul Verma");
            pstmt.setString(5, "rahul.verma@trafficpolice.gov.in");
            pstmt.setString(6, "+91 98333 44556");
            pstmt.setString(7, now);
            pstmt.executeUpdate();

            // Citizen 1 (citizen1 / citizen123)
            pstmt.setString(1, "citizen1");
            pstmt.setString(2, PasswordUtil.hashPassword("citizen123"));
            pstmt.setString(3, "CITIZEN");
            pstmt.setString(4, "Amit Patel");
            pstmt.setString(5, "amit.patel@gmail.com");
            pstmt.setString(6, "+91 97444 55667");
            pstmt.setString(7, now);
            pstmt.executeUpdate();

            // Citizen 2 (citizen2 / citizen123)
            pstmt.setString(1, "citizen2");
            pstmt.setString(2, PasswordUtil.hashPassword("citizen123"));
            pstmt.setString(3, "CITIZEN");
            pstmt.setString(4, "Sneha Reddy");
            pstmt.setString(5, "sneha.reddy@yahoo.com");
            pstmt.setString(6, "+91 97555 66778");
            pstmt.setString(7, now);
            pstmt.executeUpdate();
        }

        // 2. Seed Junctions
        String junctionSql = "INSERT INTO junctions (junction_code, name, area, landmark, latitude, longitude, risk_level) VALUES (?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(junctionSql)) {
            pstmt.setString(1, "JN-101");
            pstmt.setString(2, "MG Road - Brigade Crossroads");
            pstmt.setString(3, "Central Business District");
            pstmt.setString(4, "Near Metro Station Gate 2");
            pstmt.setDouble(5, 12.9716);
            pstmt.setDouble(6, 77.5946);
            pstmt.setString(7, "HIGH");
            pstmt.executeUpdate();

            pstmt.setString(1, "JN-102");
            pstmt.setString(2, "Railway Station Roundabout");
            pstmt.setString(3, "Majestic Hub");
            pstmt.setString(4, "City Central Railway Terminus");
            pstmt.setDouble(5, 12.9781);
            pstmt.setDouble(6, 77.5694);
            pstmt.setString(7, "HIGH");
            pstmt.executeUpdate();

            pstmt.setString(1, "JN-103");
            pstmt.setString(2, "Cyber Expressway Toll & Flyover");
            pstmt.setString(3, "Tech Park Corridor");
            pstmt.setString(4, "Tower 3 Exit Ramp");
            pstmt.setDouble(5, 12.9279);
            pstmt.setDouble(6, 77.6835);
            pstmt.setString(7, "MEDIUM");
            pstmt.executeUpdate();

            pstmt.setString(1, "JN-104");
            pstmt.setString(2, "South Boulevard Square");
            pstmt.setString(3, "Jayanagar 4th Block");
            pstmt.setString(4, "Opposite Complex Market");
            pstmt.setDouble(5, 12.9304);
            pstmt.setDouble(6, 77.5838);
            pstmt.setString(7, "LOW");
            pstmt.executeUpdate();
        }

        // 3. Seed Officers
        String officerSql = "INSERT INTO officers (user_id, badge_number, rank, police_station, status, assigned_junction_id) VALUES (?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(officerSql)) {
            // Inspector Vikram (user_id 2) -> JN-101 (id 1)
            pstmt.setInt(1, 2);
            pstmt.setString(2, "TP-101");
            pstmt.setString(3, "Inspector");
            pstmt.setString(4, "Central Division Traffic PS");
            pstmt.setString(5, "ACTIVE");
            pstmt.setInt(6, 1);
            pstmt.executeUpdate();

            // Sub-Inspector Priya (user_id 3) -> JN-102 (id 2)
            pstmt.setInt(1, 3);
            pstmt.setString(2, "TP-102");
            pstmt.setString(3, "Sub-Inspector");
            pstmt.setString(4, "Majestic Traffic PS");
            pstmt.setString(5, "ACTIVE");
            pstmt.setInt(6, 2);
            pstmt.executeUpdate();

            // Head Constable Rahul (user_id 4) -> JN-103 (id 3)
            pstmt.setInt(1, 4);
            pstmt.setString(2, "TP-103");
            pstmt.setString(3, "Head Constable");
            pstmt.setString(4, "Tech Corridor Traffic PS");
            pstmt.setString(5, "ACTIVE");
            pstmt.setInt(6, 3);
            pstmt.executeUpdate();
        }

        // 4. Seed Citizen Profiles
        String citizenSql = "INSERT INTO citizen_profiles (user_id, dl_number, vehicle_number, address) VALUES (?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(citizenSql)) {
            pstmt.setInt(1, 5); // Amit Patel
            pstmt.setString(2, "DL-042018001234");
            pstmt.setString(3, "KA-01-AB-1234");
            pstmt.setString(4, "Flat 402, Green Glen Apartments, Bangalore");
            pstmt.executeUpdate();

            pstmt.setInt(1, 6); // Sneha Reddy
            pstmt.setString(2, "DL-052020005678");
            pstmt.setString(3, "KA-05-MN-5678");
            pstmt.setString(4, "No 18, 7th Cross, Indiranagar, Bangalore");
            pstmt.executeUpdate();
        }

        // 5. Seed Violation Types
        String violTypeSql = "INSERT INTO violations (violation_code, title, category, description, standard_fine_amount) VALUES (?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(violTypeSql)) {
            pstmt.setString(1, "VIO-01");
            pstmt.setString(2, "Red Light Violation");
            pstmt.setString(3, "Traffic Signal");
            pstmt.setString(4, "Jumping red traffic light or stop sign at junction");
            pstmt.setDouble(5, 1000.00);
            pstmt.executeUpdate();

            pstmt.setString(1, "VIO-02");
            pstmt.setString(2, "Over-speeding");
            pstmt.setString(3, "Speed Enforcement");
            pstmt.setString(4, "Exceeding designated speed limit on urban/expressway road");
            pstmt.setDouble(5, 2000.00);
            pstmt.executeUpdate();

            pstmt.setString(1, "VIO-03");
            pstmt.setString(2, "Riding Without Helmet");
            pstmt.setString(3, "Safety Compliance");
            pstmt.setString(4, "Rider or pillion without certified safety helmet");
            pstmt.setDouble(5, 500.00);
            pstmt.executeUpdate();

            pstmt.setString(1, "VIO-04");
            pstmt.setString(2, "Driving Without Seatbelt");
            pstmt.setString(3, "Safety Compliance");
            pstmt.setString(4, "Driver or front passenger not fastened with seatbelt");
            pstmt.setDouble(5, 1000.00);
            pstmt.executeUpdate();

            pstmt.setString(1, "VIO-05");
            pstmt.setString(2, "Illegal Parking / Obstruction");
            pstmt.setString(3, "Parking Regulation");
            pstmt.setString(4, "Parking in No Parking zone or obstructing vehicular flow");
            pstmt.setDouble(5, 1000.00);
            pstmt.executeUpdate();

            pstmt.setString(1, "VIO-06");
            pstmt.setString(2, "Drunk & Impaired Driving");
            pstmt.setString(3, "Major Hazard");
            pstmt.setString(4, "Operating vehicle exceeding permissible Blood Alcohol Content");
            pstmt.setDouble(5, 10000.00);
            pstmt.executeUpdate();

            pstmt.setString(1, "VIO-07");
            pstmt.setString(2, "Using Mobile Phone While Driving");
            pstmt.setString(3, "Distracted Driving");
            pstmt.setString(4, "Holding or operating mobile hand-held device while moving");
            pstmt.setDouble(5, 1500.00);
            pstmt.executeUpdate();

            pstmt.setString(1, "VIO-08");
            pstmt.setString(2, "Driving on Wrong Side / One-Way");
            pstmt.setString(3, "Dangerous Driving");
            pstmt.setString(4, "Driving against flow of traffic on designated one-way street");
            pstmt.setDouble(5, 2000.00);
            pstmt.executeUpdate();
        }

        // 6. Seed Duty Assignments
        String dutySql = "INSERT INTO duty_assignments (officer_id, junction_id, shift, duty_date, start_time, end_time, status, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(dutySql)) {
            pstmt.setInt(1, 1); // Officer 1
            pstmt.setInt(2, 1); // JN-101
            pstmt.setString(3, "MORNING");
            pstmt.setString(4, today);
            pstmt.setString(5, "06:00");
            pstmt.setString(6, "14:00");
            pstmt.setString(7, "IN_PROGRESS");
            pstmt.setString(8, "Peak morning office rush supervision");
            pstmt.executeUpdate();

            pstmt.setInt(1, 2); // Officer 2
            pstmt.setInt(2, 2); // JN-102
            pstmt.setString(3, "MORNING");
            pstmt.setString(4, today);
            pstmt.setString(5, "06:00");
            pstmt.setString(6, "14:00");
            pstmt.setString(7, "IN_PROGRESS");
            pstmt.setString(8, "Station pedestrian crowd control and auto queue management");
            pstmt.executeUpdate();

            pstmt.setInt(1, 3); // Officer 3
            pstmt.setInt(2, 3); // JN-103
            pstmt.setString(3, "EVENING");
            pstmt.setString(4, today);
            pstmt.setString(5, "14:00");
            pstmt.setString(6, "22:00");
            pstmt.setString(7, "SCHEDULED");
            pstmt.setString(8, "Tech park evening peak hour clearing");
            pstmt.executeUpdate();
        }

        // 7. Seed Attendance Records
        String attSql = "INSERT INTO attendance (officer_id, duty_id, attendance_date, check_in_time, check_out_time, status, remarks) VALUES (?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(attSql)) {
            pstmt.setInt(1, 1);
            pstmt.setInt(2, 1);
            pstmt.setString(3, today);
            pstmt.setString(4, "05:55");
            pstmt.setNull(5, java.sql.Types.VARCHAR);
            pstmt.setString(6, "ON_DUTY");
            pstmt.setString(7, "Reporting on time at MG Road");
            pstmt.executeUpdate();

            pstmt.setInt(1, 2);
            pstmt.setInt(2, 2);
            pstmt.setString(3, today);
            pstmt.setString(4, "06:05");
            pstmt.setNull(5, java.sql.Types.VARCHAR);
            pstmt.setString(6, "ON_DUTY");
            pstmt.setString(7, "Checked in at Majestic roundabout");
            pstmt.executeUpdate();
        }

        // 8. Seed Sample Challans
        String challanSql = "INSERT INTO challans (challan_number, vehicle_number, vehicle_type, violator_name, violator_phone, officer_id, junction_id, violation_id, violation_date, fine_amount, payment_status, payment_date, payment_reference, evidence_file_path, remarks) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(challanSql)) {
            // Challan 1: For citizen 1 (Amit Patel) - Pending
            pstmt.setString(1, "CH-2026-00101");
            pstmt.setString(2, "KA-01-AB-1234");
            pstmt.setString(3, "FOUR_WHEELER_CAR");
            pstmt.setString(4, "Amit Patel");
            pstmt.setString(5, "+91 97444 55667");
            pstmt.setInt(6, 1); // Officer 1
            pstmt.setInt(7, 1); // JN-101
            pstmt.setInt(8, 1); // Red light
            pstmt.setString(9, LocalDateTime.now().minusDays(2).toString());
            pstmt.setDouble(10, 1000.00);
            pstmt.setString(11, "PENDING");
            pstmt.setNull(12, java.sql.Types.VARCHAR);
            pstmt.setNull(13, java.sql.Types.VARCHAR);
            pstmt.setString(14, "evidence_sample_redlight.jpg");
            pstmt.setString(15, "Crossed stop line 4 seconds after red signal engaged.");
            pstmt.executeUpdate();

            // Challan 2: For citizen 1 - Paid
            pstmt.setString(1, "CH-2026-00088");
            pstmt.setString(2, "KA-01-AB-1234");
            pstmt.setString(3, "FOUR_WHEELER_CAR");
            pstmt.setString(4, "Amit Patel");
            pstmt.setString(5, "+91 97444 55667");
            pstmt.setInt(6, 1);
            pstmt.setInt(7, 1);
            pstmt.setInt(8, 4); // Seatbelt
            pstmt.setString(9, LocalDateTime.now().minusDays(15).toString());
            pstmt.setDouble(10, 1000.00);
            pstmt.setString(11, "PAID");
            pstmt.setString(12, LocalDateTime.now().minusDays(14).toString());
            pstmt.setString(13, "TXN-8823910928");
            pstmt.setString(14, "");
            pstmt.setString(15, "Payment verified via online portal receipt.");
            pstmt.executeUpdate();

            // Challan 3: For citizen 2 (Sneha Reddy) - Overdue
            pstmt.setString(1, "CH-2026-00045");
            pstmt.setString(2, "KA-05-MN-5678");
            pstmt.setString(3, "TWO_WHEELER");
            pstmt.setString(4, "Sneha Reddy");
            pstmt.setString(5, "+91 97555 66778");
            pstmt.setInt(6, 2);
            pstmt.setInt(7, 2);
            pstmt.setInt(8, 3); // No helmet
            pstmt.setString(9, LocalDateTime.now().minusDays(35).toString());
            pstmt.setDouble(10, 500.00);
            pstmt.setString(11, "OVERDUE");
            pstmt.setNull(12, java.sql.Types.VARCHAR);
            pstmt.setNull(13, java.sql.Types.VARCHAR);
            pstmt.setString(14, "evidence_sample_helmet.jpg");
            pstmt.setString(15, "Pillion rider without helmet detected on junction camera.");
            pstmt.executeUpdate();

            // Challan 4: General violator - Pending
            pstmt.setString(1, "CH-2026-00104");
            pstmt.setString(2, "KA-03-XY-9012");
            pstmt.setString(3, "HEAVY_VEHICLE");
            pstmt.setString(4, "Ramesh Transport Corp");
            pstmt.setString(5, "+91 99887 76655");
            pstmt.setInt(6, 1);
            pstmt.setInt(7, 1);
            pstmt.setInt(8, 8); // Wrong side
            pstmt.setString(9, LocalDateTime.now().minusHours(3).toString());
            pstmt.setDouble(10, 2000.00);
            pstmt.setString(11, "PENDING");
            pstmt.setNull(12, java.sql.Types.VARCHAR);
            pstmt.setNull(13, java.sql.Types.VARCHAR);
            pstmt.setString(14, "");
            pstmt.setString(15, "Commercial truck entered one-way during morning school hours.");
            pstmt.executeUpdate();
        }

        // 9. Seed Complaints
        String compSql = "INSERT INTO complaints (complaint_number, citizen_id, junction_id, complaint_type, vehicle_number, description, incident_date, status, assigned_officer_id, officer_action_notes, evidence_file_path, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(compSql)) {
            // Complaint 1 by Amit Patel
            pstmt.setString(1, "CMP-2026-0012");
            pstmt.setInt(2, 5); // Amit Patel
            pstmt.setInt(3, 1); // JN-101
            pstmt.setString(4, "Illegal Parking / Road Blockage");
            pstmt.setString(5, "KA-53-Z-8899");
            pstmt.setString(6, "SUV parked right in front of pedestrian walkway and zebra crossing blocking handicapped ramp.");
            pstmt.setString(7, LocalDateTime.now().minusHours(6).toString());
            pstmt.setString(8, "INVESTIGATING");
            pstmt.setInt(9, 1); // Officer Vikram Sharma
            pstmt.setString(10, "Patrol squad dispatched. Towing crane requested.");
            pstmt.setString(11, "complaint_sample_parking.jpg");
            pstmt.setString(12, LocalDateTime.now().minusHours(6).toString());
            pstmt.setString(13, LocalDateTime.now().minusHours(4).toString());
            pstmt.executeUpdate();

            // Complaint 2 by Sneha Reddy
            pstmt.setString(1, "CMP-2026-0008");
            pstmt.setInt(2, 6); // Sneha Reddy
            pstmt.setInt(3, 2); // JN-102
            pstmt.setString(4, "Broken Traffic Light Signal");
            pstmt.setNull(5, java.sql.Types.VARCHAR);
            pstmt.setString(6, "The North-bound amber and red lights are continuously blinking, causing massive confusion and near-misses.");
            pstmt.setString(7, LocalDateTime.now().minusDays(1).toString());
            pstmt.setString(8, "RESOLVED");
            pstmt.setInt(9, 2); // Officer Priya Nair
            pstmt.setString(10, "Signal technicians from Municipal electrical wing replaced the relay unit. Lights restored to normal sync.");
            pstmt.setString(11, "");
            pstmt.setString(12, LocalDateTime.now().minusDays(1).toString());
            pstmt.setString(13, LocalDateTime.now().minusHours(18).toString());
            pstmt.executeUpdate();
        }

        // 10. Seed Emergency Reports
        String emergSql = "INSERT INTO emergency_reports (report_number, officer_id, junction_id, accident_type, severity, vehicles_involved, casualties_count, ambulance_required, tow_truck_required, road_blocked, report_time, status, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(emergSql)) {
            pstmt.setString(1, "EMG-2026-0005");
            pstmt.setInt(2, 1);
            pstmt.setInt(3, 1); // JN-101
            pstmt.setString(4, "Rear-end collision at intersection stop");
            pstmt.setString(5, "MINOR");
            pstmt.setInt(6, 2);
            pstmt.setInt(7, 0);
            pstmt.setInt(8, 0);
            pstmt.setInt(9, 1); // Tow truck needed
            pstmt.setInt(10, 0);
            pstmt.setString(11, LocalDateTime.now().minusHours(2).toString());
            pstmt.setString(12, "ASSISTANCE_DISPATCHED");
            pstmt.setString(13, "Two private cars minor fender bender. Moving to shoulder road to avoid bottleneck.");
            pstmt.executeUpdate();

            pstmt.setString(1, "EMG-2026-0002");
            pstmt.setInt(2, 2);
            pstmt.setInt(3, 2); // JN-102
            pstmt.setString(4, "Motorcycle skid on oil spill");
            pstmt.setString(5, "MAJOR");
            pstmt.setInt(6, 1);
            pstmt.setInt(7, 1);
            pstmt.setInt(8, 1); // Ambulance required
            pstmt.setInt(9, 0);
            pstmt.setInt(10, 1);
            pstmt.setString(11, LocalDateTime.now().minusDays(2).toString());
            pstmt.setString(12, "CLEARED");
            pstmt.setString(13, "Rider treated at St. Martha's Hospital with minor abrasions. Sand spread over oil spill. Road fully cleared.");
            pstmt.executeUpdate();
        }

        System.out.println("Sample database records initialized successfully!");
    }
}
