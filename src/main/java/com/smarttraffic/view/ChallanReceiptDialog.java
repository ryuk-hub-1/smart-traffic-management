package com.smarttraffic.view;

import com.smarttraffic.model.Challan;
import com.smarttraffic.service.ReceiptService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Printable Challan Receipt Preview Modal Dialog.
 */
public class ChallanReceiptDialog {

    public static void show(Challan challan, Stage parentStage) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Digital Challan Receipt - " + challan.getChallanNumber());

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f8fafc;");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(2);
        Label title = new Label("Official Challan Receipt Slip");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Authorized e-Challan document issued by Traffic Police Department");
        subtitle.getStyleClass().add("subtitle-header");
        titleBox.getChildren().addAll(title, subtitle);
        header.getChildren().add(titleBox);

        // Receipt Content Box
        String receiptText = ReceiptService.getInstance().generateChallanReceiptText(challan);

        TextArea receiptArea = new TextArea(receiptText);
        receiptArea.setEditable(false);
        receiptArea.setWrapText(false);
        receiptArea.setStyle("""
            -fx-font-family: 'Consolas', 'Courier New', monospace;
            -fx-font-size: 12px;
            -fx-control-inner-background: #ffffff;
            -fx-text-fill: #1e293b;
            -fx-border-color: #cbd5e1;
            -fx-border-radius: 6px;
        """);
        receiptArea.setPrefHeight(420);
        receiptArea.setPrefWidth(580);
        VBox.setVgrow(receiptArea, Priority.ALWAYS);

        // Action Buttons
        HBox btnBar = new HBox(12);
        btnBar.setAlignment(Pos.CENTER_RIGHT);

        Button saveBtn = new Button("💾 Save As Text / Print Slip");
        saveBtn.getStyleClass().add("btn-secondary");
        saveBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Challan Receipt");
            fileChooser.setInitialFileName("Challan_" + challan.getChallanNumber() + ".txt");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Document (*.txt)", "*.txt"));
            File file = fileChooser.showSaveDialog(dialog);
            if (file != null) {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(receiptText);
                    UIComponents.showInfoAlert("Receipt Saved", "Challan receipt successfully saved to:\n" + file.getAbsolutePath());
                } catch (IOException ex) {
                    UIComponents.showErrorAlert("Save Failed", "Could not save receipt: " + ex.getMessage());
                }
            }
        });

        Button printSimBtn = new Button("🖨️ Print Receipt");
        printSimBtn.getStyleClass().add("btn-primary");
        printSimBtn.setOnAction(e -> {
            UIComponents.showInfoAlert("Print Dispatch",
                    "Print command sent successfully to thermal receipt printer spooler.\n\nDocument: " + challan.getChallanNumber() + "\nVehicle: " + challan.getVehicleNumber());
        });

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-secondary");
        closeBtn.setOnAction(e -> dialog.close());

        btnBar.getChildren().addAll(saveBtn, printSimBtn, closeBtn);

        root.getChildren().addAll(header, receiptArea, btnBar);

        Scene scene = new Scene(root, 640, 540);
        try {
            scene.getStylesheets().add(ChallanReceiptDialog.class.getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {
        }
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
