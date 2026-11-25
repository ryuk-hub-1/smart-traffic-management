package com.smarttraffic.controller;

import com.smarttraffic.exception.ValidationException;
import com.smarttraffic.service.AuthService;
import com.smarttraffic.view.UIComponents;
import com.smarttraffic.view.ViewNavigator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Controller for Citizen Self-Registration.
 */
public class RegisterController {
    private final BorderPane root = new BorderPane();

    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmPasswordField = new PasswordField();
    private final TextField fullNameField = new TextField();
    private final TextField emailField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField dlField = new TextField();
    private final TextField vehicleField = new TextField();
    private final TextArea addressArea = new TextArea();
    private final Label errorLabel = new Label();

    public RegisterController() {
        buildUI();
    }

    public Parent getView() {
        return root;
    }

    private void buildUI() {
        VBox container = new VBox(15);
        container.setPadding(new Insets(30, 60, 30, 60));
        container.setMaxWidth(700);
        container.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Citizen Vehicle Account Registration");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        Label subtitle = new Label("Register your profile to view challans, make fine payments, and file traffic reports.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        errorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
        errorLabel.setVisible(false);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);

        // Col 0 & 1
        grid.add(createFieldBox("Username *", usernameField, "e.g. rahul_sharma"), 0, 0);
        grid.add(createFieldBox("Full Name *", fullNameField, "e.g. Rahul Sharma"), 1, 0);

        grid.add(createFieldBox("Password *", passwordField, "Minimum 4 characters"), 0, 1);
        grid.add(createFieldBox("Confirm Password *", confirmPasswordField, "Re-enter password"), 1, 1);

        grid.add(createFieldBox("Email Address", emailField, "rahul@example.com"), 0, 2);
        grid.add(createFieldBox("Phone Number", phoneField, "+91 98765 00000"), 1, 2);

        grid.add(createFieldBox("Driving License (DL) Number", dlField, "e.g. DL-042022009988"), 0, 3);
        grid.add(createFieldBox("Vehicle Registration Number", vehicleField, "e.g. KA-01-MJ-5678"), 1, 3);

        VBox addrBox = new VBox(4);
        Label addrLbl = new Label("Residential Address");
        addrLbl.getStyleClass().add("form-label");
        addressArea.setPrefRowCount(2);
        addressArea.setPromptText("Enter your address details");
        addrBox.getChildren().addAll(addrLbl, addressArea);
        grid.add(addrBox, 0, 4, 2, 1);

        HBox btnBar = new HBox(12);
        btnBar.setPadding(new Insets(10, 0, 0, 0));
        btnBar.setAlignment(Pos.CENTER_RIGHT);

        Button backBtn = new Button("← Back to Login");
        backBtn.getStyleClass().add("btn-secondary");
        backBtn.setOnAction(e -> ViewNavigator.showLogin());

        Button submitBtn = new Button("Register & Create Account ➔");
        submitBtn.getStyleClass().add("btn-primary");
        submitBtn.setOnAction(e -> handleRegister());

        btnBar.getChildren().addAll(backBtn, submitBtn);

        container.getChildren().addAll(title, subtitle, errorLabel, grid, btnBar);

        ScrollPane sp = new ScrollPane(container);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        root.setCenter(sp);
    }

    private VBox createFieldBox(String labelText, Control inputControl, String prompt) {
        VBox box = new VBox(4);
        Label label = new Label(labelText);
        label.getStyleClass().add("form-label");
        if (inputControl instanceof TextInputControl tc) {
            tc.setPromptText(prompt);
        }
        box.getChildren().addAll(label, inputControl);
        return box;
    }

    private void handleRegister() {
        errorLabel.setVisible(false);
        try {
            AuthService.getInstance().registerCitizen(
                    usernameField.getText(),
                    passwordField.getText(),
                    confirmPasswordField.getText(),
                    fullNameField.getText(),
                    emailField.getText(),
                    phoneField.getText(),
                    dlField.getText(),
                    vehicleField.getText(),
                    addressArea.getText()
            );

            UIComponents.showInfoAlert("Registration Successful",
                    "Your citizen account has been successfully created!\nYou can now log in with your credentials.");
            ViewNavigator.showLogin();

        } catch (ValidationException ex) {
            errorLabel.setText(ex.getMessage());
            errorLabel.setVisible(true);
        } catch (Exception ex) {
            errorLabel.setText("Registration error: " + ex.getMessage());
            errorLabel.setVisible(true);
        }
    }
}
