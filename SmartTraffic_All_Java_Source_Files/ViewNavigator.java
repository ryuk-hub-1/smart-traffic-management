package com.smarttraffic.view;

import com.smarttraffic.controller.LoginController;
import com.smarttraffic.controller.MainLayoutController;
import com.smarttraffic.controller.RegisterController;
import com.smarttraffic.service.AuthService;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * ViewNavigator manages the primary application stages and scene changes.
 */
public class ViewNavigator {
    private static Stage primaryStage;
    private static MainLayoutController currentMainController;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void showLogin() {
        LoginController loginView = new LoginController();
        Scene scene = new Scene(loginView.getView(), 950, 650);
        applyStyles(scene);
        primaryStage.setTitle("Smart Traffic Police Management System - Login");
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void showRegister() {
        RegisterController registerView = new RegisterController();
        Scene scene = new Scene(registerView.getView(), 950, 680);
        applyStyles(scene);
        primaryStage.setTitle("Smart Traffic Police Management System - Citizen Registration");
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }

    public static void showMainDashboard() {
        currentMainController = new MainLayoutController();
        Scene scene = new Scene(currentMainController.getView(), 1220, 780);
        applyStyles(scene);
        String roleStr = AuthService.getInstance().getCurrentUser() != null
                ? AuthService.getInstance().getCurrentUser().getRole().getDisplayName()
                : "Portal";
        primaryStage.setTitle("Smart Traffic Police Management System - " + roleStr);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }

    public static void loadMainContent(Node content) {
        if (currentMainController != null) {
            currentMainController.setContent(content);
        }
    }

    public static void loadMainContent(Node content, String menuKeyword) {
        if (currentMainController != null) {
            currentMainController.setContent(content);
            if (menuKeyword != null) {
                currentMainController.highlightMenuByText(menuKeyword);
            }
        }
    }

    public static void applyStyles(Scene scene) {
        try {
            String css = ViewNavigator.class.getResource("/css/style.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Could not load style.css: " + e.getMessage());
        }
    }
}
