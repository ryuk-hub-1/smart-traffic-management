package com.smarttraffic.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Modern UI Component Factory.
 * Builds bespoke cards, license plates, rank avatars, and telemetry chips.
 */
public class UIComponents {

    public static VBox createKpiCard(String title, String value, String subtitle, String accentColor) {
        VBox card = new VBox(4);
        card.getStyleClass().add("kpi-card");
        card.setStyle(card.getStyle() + "; -fx-border-left-color: " + accentColor + "; -fx-border-left-width: 4px;");

        Label titleLbl = new Label(title.toUpperCase());
        titleLbl.getStyleClass().add("kpi-title");

        Label valLbl = new Label(value);
        valLbl.getStyleClass().add("kpi-value");

        Label subLbl = new Label(subtitle);
        subLbl.getStyleClass().add("kpi-subtitle");

        card.getChildren().addAll(titleLbl, valLbl, subLbl);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    public static Label createLicensePlate(String plateNumber) {
        Label plate = new Label(plateNumber != null && !plateNumber.isEmpty() ? plateNumber.toUpperCase() : "IND");
        plate.getStyleClass().add("license-plate");
        return plate;
    }

    public static StackPane createRankAvatar(String rank) {
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("officer-avatar");

        String abbrev = "OFF";
        if (rank != null) {
            String r = rank.toUpperCase();
            if (r.contains("INSP")) abbrev = "INSP";
            else if (r.contains("SUB-INSP") || r.contains("SI")) abbrev = "SI";
            else if (r.contains("HEAD") || r.contains("HC")) abbrev = "HC";
            else if (r.contains("CONSTABLE")) abbrev = "PC";
            else if (r.contains("COMMISSIONER")) abbrev = "CP";
        }

        Label txt = new Label(abbrev);
        txt.getStyleClass().add("officer-avatar-text");
        avatar.getChildren().add(txt);
        return avatar;
    }

    public static Label createBadge(String text, String styleClass) {
        Label badge = new Label(text);
        badge.getStyleClass().addAll("badge-pill", styleClass);
        return badge;
    }

    public static VBox createViewHeader(String title, String subtitle) {
        VBox box = new VBox(3);
        box.setPadding(new Insets(0, 0, 15, 0));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("title-header");

        Label subLabel = new Label(subtitle);
        subLabel.getStyleClass().add("subtitle-header");

        box.getChildren().addAll(titleLabel, subLabel);
        return box;
    }

    public static TextField createSearchField(String placeholder) {
        TextField tf = new TextField();
        tf.setPromptText("🔍 " + placeholder);
        tf.setPrefWidth(280);
        return tf;
    }

    public static HBox createViewToggle(String labelCardView, String labelTableView, Consumer<Boolean> onToggle) {
        HBox bar = new HBox(4);
        bar.getStyleClass().add("view-switch-bar");
        bar.setAlignment(Pos.CENTER);

        Button cardBtn = new Button("🗂️ " + labelCardView);
        cardBtn.getStyleClass().addAll("view-switch-btn", "view-switch-btn-active");

        Button tableBtn = new Button("📑 " + labelTableView);
        tableBtn.getStyleClass().add("view-switch-btn");

        cardBtn.setOnAction(e -> {
            cardBtn.getStyleClass().add("view-switch-btn-active");
            tableBtn.getStyleClass().remove("view-switch-btn-active");
            onToggle.accept(true);
        });

        tableBtn.setOnAction(e -> {
            tableBtn.getStyleClass().add("view-switch-btn-active");
            cardBtn.getStyleClass().remove("view-switch-btn-active");
            onToggle.accept(false);
        });

        bar.getChildren().addAll(cardBtn, tableBtn);
        return bar;
    }

    public static HBox createStatChip(String icon, String label, String value, String bgHex) {
        HBox chip = new HBox(8);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setStyle("-fx-background-color: " + bgHex + "; -fx-background-radius: 8px; -fx-padding: 8px 14px; -fx-border-color: #cbd5e1; -fx-border-radius: 8px;");

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 16px;");

        VBox textCol = new VBox(1);
        Label titleLbl = new Label(label);
        titleLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #64748b;");
        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");
        textCol.getChildren().addAll(titleLbl, valLbl);

        chip.getChildren().addAll(iconLbl, textCol);
        return chip;
    }

    public static void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showWarningAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static boolean showConfirmDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
