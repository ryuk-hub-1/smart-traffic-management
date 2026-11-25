package com.smarttraffic;

import com.smarttraffic.dao.DatabaseManager;
import com.smarttraffic.util.ThreadPoolManager;
import com.smarttraffic.view.ViewNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main JavaFX Application Entry Point for Smart Traffic Police Management System.
 */
public class SmartTrafficApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Initialize SQLite Database schema and sample data
        DatabaseManager.getInstance();

        // Configure primary stage in Navigator
        ViewNavigator.setPrimaryStage(primaryStage);

        // Check if screenshot capture mode was requested
        if (getParameters().getRaw().contains("--screenshots")) {
            new com.smarttraffic.util.ScreenshotGenerator().generate(primaryStage);
            return;
        }

        // Show login scene
        ViewNavigator.showLogin();
    }

    @Override
    public void stop() {
        // Gracefully shutdown background thread pools
        ThreadPoolManager.getInstance().shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
