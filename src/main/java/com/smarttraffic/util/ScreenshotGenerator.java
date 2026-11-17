package com.smarttraffic.util;

import com.smarttraffic.controller.LoginController;
import com.smarttraffic.controller.MainLayoutController;
import com.smarttraffic.controller.RegisterController;
import com.smarttraffic.controller.admin.*;
import com.smarttraffic.controller.citizen.*;
import com.smarttraffic.controller.officer.*;
import com.smarttraffic.service.AuthService;
import com.smarttraffic.view.ViewNavigator;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

public class ScreenshotGenerator {

    private static final String OUTPUT_DIR = "C:\\Users\\SSD\\.gemini\\antigravity\\brain\\c1e1f316-9e58-45e1-9f12-a37e4b5f4de1\\screenshots";
    private Stage stage;

    public void generate(Stage stage) {
        this.stage = stage;
        stage.setTitle("Smart Traffic Police Management System");
        stage.setWidth(1240);
        stage.setHeight(840);
        stage.show();

        new Thread(this::runCaptures).start();
    }

    private void runCaptures() {
        try {
            File outDir = new File(OUTPUT_DIR);
            if (!outDir.exists()) outDir.mkdirs();

            System.out.println(">>> Starting automated screenshot capture sequence...");

            // 1. Login Screen
            captureDirectScene("01_login_portal.png", () -> (Parent) new LoginController().getView(), 960, 680);

            // 2. Citizen Registration Screen
            captureDirectScene("02_citizen_registration.png", () -> (Parent) new RegisterController().getView(), 960, 700);

            // 3. Admin Persona Screens
            AuthService.getInstance().login("admin", "admin123");
            MainLayoutController adminLayout = createMainLayout();

            // Admin Dashboard
            captureLayout("03_admin_dashboard.png", adminLayout, () -> new AdminDashboardController().getView());

            // Officer Management
            captureLayout("04_officer_management.png", adminLayout, () -> new OfficerManagementController().getView());

            // Junction Management
            captureLayout("05_junction_management.png", adminLayout, () -> new JunctionManagementController().getView());

            // Duty Allocation (3-Shift Board)
            captureLayout("06_duty_shift_board.png", adminLayout, () -> new DutyAllocationController().getView());

            // Attendance Monitor
            captureLayout("07_attendance_monitor.png", adminLayout, () -> new AttendanceMonitorController().getView());

            // Challans Registry
            captureLayout("08_echallan_registry.png", adminLayout, () -> new AdminChallansController().getView());

            // Citizen Complaints (Admin Redressal)
            captureLayout("09_citizen_complaints.png", adminLayout, () -> new AdminComplaintsController().getView());

            // Emergency Command Console
            captureLayout("10_emergency_command_console.png", adminLayout, () -> new AdminEmergencyController().getView());

            // Executive Reports & Analytics
            captureLayout("11_executive_reports.png", adminLayout, () -> new ReportsAnalyticsController().getView());

            // 4. Officer Persona Screens
            AuthService.getInstance().login("officer1", "officer123");
            MainLayoutController officerLayout = createMainLayout();

            // Officer Operational Console
            captureLayout("12_officer_dashboard.png", officerLayout, () -> new OfficerDashboardController().getView());

            // Log Violation & Challan
            captureLayout("13_log_violation_form.png", officerLayout, () -> new LogViolationController().getView());

            // Officer Shifts & Duties
            captureLayout("14_officer_duty_roster.png", officerLayout, () -> new OfficerDutyController().getView());

            // 5. Citizen Persona Screens
            AuthService.getInstance().login("citizen1", "citizen123");
            MainLayoutController citizenLayout = createMainLayout();

            // Citizen Dashboard
            captureLayout("15_citizen_dashboard.png", citizenLayout, () -> new CitizenDashboardController().getView());

            // Citizen Citations & Online Payment
            captureLayout("16_citizen_challans.png", citizenLayout, () -> new CitizenChallansController().getView());

            // File Traffic Complaint
            captureLayout("17_file_complaint_form.png", citizenLayout, () -> new FileComplaintController().getView());

            // Citizen Complaint History & Status Tracking
            captureLayout("18_complaint_history.png", citizenLayout, () -> new CitizenComplaintsHistoryController().getView());

            System.out.println(">>> All 18 screenshots captured successfully in: " + OUTPUT_DIR);

            Thread.sleep(1200);
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

    private void captureDirectScene(String filename, Supplier<Parent> rootSupplier, int w, int h) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            Parent root = rootSupplier.get();
            if (stage.getScene() == null) {
                Scene scene = new Scene(root, w, h);
                ViewNavigator.applyStyles(scene);
                stage.setScene(scene);
            } else {
                stage.getScene().setRoot(root);
            }
            stage.setWidth(w);
            stage.setHeight(h);
            stage.centerOnScreen();
        });
        Thread.sleep(1200); // allow async layout & DB load
        Platform.runLater(() -> {
            try {
                WritableImage img = stage.getScene().snapshot(null);
                saveImage(img, new File(OUTPUT_DIR, filename));
                System.out.println(">>> Saved: " + filename);
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        latch.await();
    }

    private void captureLayout(String filename, MainLayoutController layout, Supplier<Node> viewSupplier) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            Node content = viewSupplier.get();
            layout.setContent(content);
            Parent root = (Parent) layout.getView();
            if (stage.getScene() == null) {
                Scene scene = new Scene(root, 1240, 840);
                ViewNavigator.applyStyles(scene);
                stage.setScene(scene);
            } else if (stage.getScene().getRoot() != root) {
                stage.getScene().setRoot(root);
            }
            stage.setWidth(1240);
            stage.setHeight(840);
        });
        Thread.sleep(1200); // allow layout, data load and chart render
        Platform.runLater(() -> {
            try {
                WritableImage img = stage.getScene().snapshot(null);
                saveImage(img, new File(OUTPUT_DIR, filename));
                System.out.println(">>> Saved: " + filename);
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
}
