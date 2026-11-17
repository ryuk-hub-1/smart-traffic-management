package com.smarttraffic;

import com.smarttraffic.controller.LoginController;
import com.smarttraffic.controller.MainLayoutController;
import com.smarttraffic.controller.RegisterController;
import com.smarttraffic.controller.admin.*;
import com.smarttraffic.controller.citizen.*;
import com.smarttraffic.controller.officer.*;
import com.smarttraffic.dao.DatabaseManager;
import com.smarttraffic.service.AuthService;
import com.smarttraffic.view.ViewNavigator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.CountDownLatch;

public class ScreenshotGeneratorApp extends Application {

    private static final String OUTPUT_DIR = "C:\\Users\\SSD\\.gemini\\antigravity\\brain\\c1e1f316-9e58-45e1-9f12-a37e4b5f4de1\\screenshots";
    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        DatabaseManager.getInstance();
        ViewNavigator.setPrimaryStage(primaryStage);

        primaryStage.setTitle("Smart Traffic Police Management System");
        primaryStage.setWidth(1240);
        primaryStage.setHeight(840);
        primaryStage.show();

        new Thread(this::runCaptures).start();
    }

    private void runCaptures() {
        try {
            File outDir = new File(OUTPUT_DIR);
            if (!outDir.exists()) outDir.mkdirs();

            System.out.println("Starting automated screenshot capture sequence...");

            // 1. Login Screen
            captureDirectScene("01_login_portal.png", new LoginController().getView(), 960, 680);

            // 2. Citizen Registration Screen
            captureDirectScene("02_citizen_registration.png", new RegisterController().getView(), 960, 700);

            // 3. Admin Persona Screens
            AuthService.getInstance().login("admin", "admin123");
            MainLayoutController adminLayout = createMainLayout();

            // Admin Dashboard
            adminLayout.setContent(new AdminDashboardController().getView());
            captureLayout("03_admin_dashboard.png", adminLayout);

            // Officer Management
            adminLayout.setContent(new OfficerManagementController().getView());
            captureLayout("04_officer_management.png", adminLayout);

            // Junction Management
            adminLayout.setContent(new JunctionManagementController().getView());
            captureLayout("05_junction_management.png", adminLayout);

            // Duty Allocation (3-Shift Board)
            adminLayout.setContent(new DutyAllocationController().getView());
            captureLayout("06_duty_shift_board.png", adminLayout);

            // Attendance Monitor
            adminLayout.setContent(new AttendanceMonitorController().getView());
            captureLayout("07_attendance_monitor.png", adminLayout);

            // Challans Registry
            adminLayout.setContent(new AdminChallansController().getView());
            captureLayout("08_echallan_registry.png", adminLayout);

            // Citizen Complaints (Admin Redressal)
            adminLayout.setContent(new AdminComplaintsController().getView());
            captureLayout("09_citizen_complaints.png", adminLayout);

            // Emergency Command Console
            adminLayout.setContent(new AdminEmergencyController().getView());
            captureLayout("10_emergency_command_console.png", adminLayout);

            // Executive Reports & Analytics
            adminLayout.setContent(new ReportsAnalyticsController().getView());
            captureLayout("11_executive_reports.png", adminLayout);

            // 4. Officer Persona Screens
            AuthService.getInstance().login("officer1", "officer123");
            MainLayoutController officerLayout = createMainLayout();

            // Officer Operational Console
            officerLayout.setContent(new OfficerDashboardController().getView());
            captureLayout("12_officer_dashboard.png", officerLayout);

            // Log Violation & Challan
            officerLayout.setContent(new LogViolationController().getView());
            captureLayout("13_log_violation_form.png", officerLayout);

            // Officer Shifts & Duties
            officerLayout.setContent(new OfficerDutyController().getView());
            captureLayout("14_officer_duty_roster.png", officerLayout);

            // 5. Citizen Persona Screens
            AuthService.getInstance().login("citizen1", "citizen123");
            MainLayoutController citizenLayout = createMainLayout();

            // Citizen Dashboard
            citizenLayout.setContent(new CitizenDashboardController().getView());
            captureLayout("15_citizen_dashboard.png", citizenLayout);

            // Citizen Citations & Online Payment
            citizenLayout.setContent(new CitizenChallansController().getView());
            captureLayout("16_citizen_challans.png", citizenLayout);

            // File Traffic Complaint
            citizenLayout.setContent(new FileComplaintController().getView());
            captureLayout("17_file_complaint_form.png", citizenLayout);

            // Citizen Complaint History & Status Tracking
            citizenLayout.setContent(new CitizenComplaintsHistoryController().getView());
            captureLayout("18_complaint_history.png", citizenLayout);

            System.out.println("All 18 screenshots captured successfully in: " + OUTPUT_DIR);

            Thread.sleep(1000);
            Platform.runLater(() -> {
                stage.close();
                Platform.exit();
                System.exit(0);
            });

        } catch (Exception e) {
            System.err.println("Error during capture: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private MainLayoutController createMainLayout() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final MainLayoutController[] res = new MainLayoutController[1];
        Platform.runLater(() -> {
            res[0] = new MainLayoutController();
            latch.countDown();
        });
        latch.await();
        return res[0];
    }

    private void captureDirectScene(String filename, Parent root, int w, int h) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            Scene scene = new Scene(root, w, h);
            ViewNavigator.applyStyles(scene);
            stage.setWidth(w);
            stage.setHeight(h);
            stage.setScene(scene);
            stage.centerOnScreen();
        });
        Thread.sleep(900); // allow async layout & DB load
        Platform.runLater(() -> {
            try {
                WritableImage img = stage.getScene().snapshot(null);
                saveImage(img, new File(OUTPUT_DIR, filename));
                System.out.println("Captured: " + filename);
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        latch.await();
    }

    private void captureLayout(String filename, MainLayoutController layout) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            Scene scene = new Scene(layout.getView(), 1240, 840);
            ViewNavigator.applyStyles(scene);
            stage.setWidth(1240);
            stage.setHeight(840);
            stage.setScene(scene);
        });
        Thread.sleep(900); // allow layout and chart render
        Platform.runLater(() -> {
            try {
                WritableImage img = stage.getScene().snapshot(null);
                saveImage(img, new File(OUTPUT_DIR, filename));
                System.out.println("Captured: " + filename);
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        latch.await();
    }

    private void saveImage(WritableImage fxImg, File file) throws Exception {
        int w = (int) fxImg.getWidth();
        int h = (int) fxImg.getHeight();
        BufferedImage bImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader pr = fxImg.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                bImg.setRGB(x, y, pr.getArgb(x, y));
            }
        }
        ImageIO.write(bImg, "png", file);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
