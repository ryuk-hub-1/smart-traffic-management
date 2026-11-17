package com.smarttraffic.controller.admin;

import com.smarttraffic.service.ReportService;
import com.smarttraffic.util.ThreadPoolManager;
import com.smarttraffic.view.UIComponents;
import com.smarttraffic.view.ViewNavigator;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Controller for Administrative Reports & Analytics.
 * Demonstrates Polymorphism and Multithreaded report synthesis.
 */
public class ReportsAnalyticsController {
    private final VBox root = new VBox(15);
    private final ReportService reportService = ReportService.getInstance();

    public ReportsAnalyticsController() {
        buildUI();
    }

    public Node getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(10, 10, 30, 10));

        VBox header = UIComponents.createViewHeader(
                "Executive Reports & Performance Analytics",
                "Synthesize departmental intelligence, revenue collections, and collision hazard analytics"
        );

        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: transparent;");

        tabPane.getTabs().add(createReportTab("📊 Violations & Offenses", () -> reportService.getViolationReport()));
        tabPane.getTabs().add(createReportTab("💰 Revenue & Fine Recovery", () -> reportService.getRevenueReport()));
        tabPane.getTabs().add(createReportTab("🚨 Accident & Hazard Summary", () -> reportService.getAccidentReport()));

        root.getChildren().addAll(header, tabPane);
    }

    @FunctionalInterface
    private interface ReportSupplier {
        ReportService.ReportGenerator get() throws Exception;
    }

    private Tab createReportTab(String tabTitle, ReportSupplier supplier) {
        Tab tab = new Tab(tabTitle);
        tab.setClosable(false);

        VBox tabContent = new VBox(15);
        tabContent.setPadding(new Insets(15));
        tabContent.getStyleClass().add("content-card");

        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label statusLbl = new Label("Generating report analytics in background thread...");
        statusLbl.setStyle("-fx-text-fill: #64748b; -fx-font-style: italic;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button exportBtn = new Button("💾 Export Report to Text");
        exportBtn.getStyleClass().add("btn-secondary");
        exportBtn.setDisable(true);

        Button reloadBtn = new Button("🔄 Re-synthesize");
        reloadBtn.getStyleClass().add("btn-primary");

        topBar.getChildren().addAll(statusLbl, spacer, exportBtn, reloadBtn);

        TextArea reportArea = new TextArea();
        reportArea.setEditable(false);
        reportArea.setPrefHeight(450);
        reportArea.setStyle("""
            -fx-font-family: 'Consolas', 'Courier New', monospace;
            -fx-font-size: 13px;
            -fx-control-inner-background: #0f172a;
            -fx-text-fill: #38bdf8;
            -fx-border-color: #334155;
            -fx-border-radius: 6px;
        """);

        tabContent.getChildren().addAll(topBar, reportArea);
        tab.setContent(tabContent);

        Runnable runGenerator = () -> {
            Platform.runLater(() -> {
                statusLbl.setText("Compiling metrics across database records...");
                exportBtn.setDisable(true);
            });

            try {
                // Background execution (Advanced Java Multithreading)
                ReportService.ReportGenerator gen = supplier.get();
                String text = gen.generateTextSummary();

                Platform.runLater(() -> {
                    reportArea.setText(text);
                    statusLbl.setText("✓ " + gen.getTitle() + " - Generated successfully");
                    exportBtn.setDisable(false);

                    exportBtn.setOnAction(e -> {
                        FileChooser fileChooser = new FileChooser();
                        fileChooser.setTitle("Export " + gen.getTitle());
                        fileChooser.setInitialFileName(gen.getTitle().replaceAll("[^a-zA-Z0-9.-]", "_") + ".txt");
                        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text File (*.txt)", "*.txt"));
                        File file = fileChooser.showSaveDialog(ViewNavigator.getPrimaryStage());
                        if (file != null) {
                            try (FileWriter fw = new FileWriter(file)) {
                                fw.write(text);
                                UIComponents.showInfoAlert("Export Successful", "Report exported to:\n" + file.getAbsolutePath());
                            } catch (IOException ex) {
                                UIComponents.showErrorAlert("Export Failed", ex.getMessage());
                            }
                        }
                    });
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    statusLbl.setText("Failed to generate report: " + ex.getMessage());
                    reportArea.setText("Error generating report: " + ex.getMessage());
                });
            }
        };

        reloadBtn.setOnAction(e -> ThreadPoolManager.getInstance().execute(runGenerator));
        ThreadPoolManager.getInstance().execute(runGenerator);

        return tab;
    }
}
