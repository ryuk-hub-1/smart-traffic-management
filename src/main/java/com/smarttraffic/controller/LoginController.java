package com.smarttraffic.controller;

import com.smarttraffic.exception.AuthenticationException;
import com.smarttraffic.exception.ValidationException;
import com.smarttraffic.model.User;
import com.smarttraffic.service.AuthService;
import com.smarttraffic.view.UIComponents;
import com.smarttraffic.view.ViewNavigator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Controller for the Login View.
 * Provides secure authentication and convenient academic demonstration quick-fill buttons.
 */
public class LoginController {
    private final BorderPane root = new BorderPane();
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label errorLabel = new Label();

    public LoginController() {
        buildUI();
    }

    public Parent getView() {
        return root;
    }

    private void buildUI() {
        HBox mainSplit = new HBox();
        HBox.setHgrow(mainSplit, Priority.ALWAYS);

        // Left Branding Side (40% width)
        VBox brandingSide = new VBox(25);
        brandingSide.setPrefWidth(400);
        brandingSide.setStyle("-fx-background-color: #0f172a; -fx-padding: 50px 40px;");
        brandingSide.setAlignment(Pos.CENTER_LEFT);

        Label logoIcon = new Label("🚦");
        logoIcon.setStyle("-fx-font-size: 54px;");

        VBox titleBox = new VBox(8);
        Label mainTitle = new Label("SMART TRAFFIC\nPOLICE SYSTEM");
        mainTitle.setStyle("-fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: #ffffff; -fx-line-spacing: 2px;");

        Label subTitle = new Label("Digital Governance & Traffic Enforcement Platform");
        subTitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
        titleBox.getChildren().addAll(mainTitle, subTitle);

        VBox features = new VBox(14);
        features.getChildren().addAll(
                createFeatureBullet("🚓 Duty Rosters & Junction Scheduling", "Automate officer shift assignments & geofenced check-ins"),
                createFeatureBullet("⚡ Instant Digital e-Challan Issuance", "Accurate offense penalization with printable receipts"),
                createFeatureBullet("🚨 Live Accident & Emergency Dispatch", "Real-time incident response with hospital/tow logistics"),
                createFeatureBullet("👥 Citizen Grievance Redressal Portal", "Transparent complaints tracking & direct police resolution")
        );

        Label footer = new Label("Advanced Java Academic Project • JavaFX • SQLite JDBC • MVC");
        footer.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        brandingSide.getChildren().addAll(logoIcon, titleBox, features, footer);

        // Right Form Side (60% width)
        VBox formSide = new VBox(20);
        formSide.setStyle("-fx-background-color: #ffffff; -fx-padding: 50px 60px;");
        formSide.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(formSide, Priority.ALWAYS);

        Label welcomeTitle = new Label("Welcome to System Portal");
        welcomeTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        Label welcomeSubtitle = new Label("Enter your credentials or choose a pre-configured demo profile below.");
        welcomeSubtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        // Error message
        errorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 600; -fx-font-size: 12px;");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);

        // Fields
        VBox userBox = new VBox(6);
        Label userLbl = new Label("Username / Badge ID");
        userLbl.getStyleClass().add("form-label");
        usernameField.setPromptText("e.g. admin, officer1, citizen1");
        usernameField.setText("admin");
        userBox.getChildren().addAll(userLbl, usernameField);

        VBox passBox = new VBox(6);
        Label passLbl = new Label("Account Password");
        passLbl.getStyleClass().add("form-label");
        passwordField.setPromptText("Enter your password");
        passwordField.setText("admin123");
        passBox.getChildren().addAll(passLbl, passwordField);

        // Enter key submits form
        passwordField.setOnAction(e -> handleLogin());
        usernameField.setOnAction(e -> handleLogin());

        Button loginBtn = new Button("Secure Login ➔");
        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle(loginBtn.getStyle() + "; -fx-padding: 11px; -fx-font-size: 14px;");
        loginBtn.setOnAction(e -> handleLogin());

        // Quick Demo Fill Section for academic evaluation
        VBox demoBox = new VBox(10);
        demoBox.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-padding: 14px;");

        Label demoTitle = new Label("⚡ Quick Demonstration Profiles (One-Click Pre-fill):");
        demoTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #475569;");

        HBox demoButtons = new HBox(10);
        Button fillAdmin = new Button("👮 Chief Admin");
        fillAdmin.getStyleClass().add("btn-secondary");
        fillAdmin.setOnAction(e -> {
            usernameField.setText("admin");
            passwordField.setText("admin123");
            errorLabel.setVisible(false);
        });

        Button fillOfficer = new Button("🚔 Police Officer");
        fillOfficer.getStyleClass().add("btn-secondary");
        fillOfficer.setOnAction(e -> {
            usernameField.setText("officer1");
            passwordField.setText("officer123");
            errorLabel.setVisible(false);
        });

        Button fillCitizen = new Button("👤 Citizen");
        fillCitizen.getStyleClass().add("btn-secondary");
        fillCitizen.setOnAction(e -> {
            usernameField.setText("citizen1");
            passwordField.setText("citizen123");
            errorLabel.setVisible(false);
        });

        demoButtons.getChildren().addAll(fillAdmin, fillOfficer, fillCitizen);
        demoBox.getChildren().addAll(demoTitle, demoButtons);

        // Register link for citizens
        HBox registerLinkBox = new HBox(6);
        registerLinkBox.setAlignment(Pos.CENTER);
        Label noAccLbl = new Label("Don't have a citizen account?");
        noAccLbl.setStyle("-fx-text-fill: #64748b;");
        Hyperlink registerLink = new Hyperlink("Register Vehicle Here");
        registerLink.setStyle("-fx-font-weight: bold; -fx-text-fill: #2563eb;");
        registerLink.setOnAction(e -> ViewNavigator.showRegister());
        registerLinkBox.getChildren().addAll(noAccLbl, registerLink);

        formSide.getChildren().addAll(
                welcomeTitle,
                welcomeSubtitle,
                errorLabel,
                userBox,
                passBox,
                loginBtn,
                demoBox,
                registerLinkBox
        );

        mainSplit.getChildren().addAll(brandingSide, formSide);
        root.setCenter(mainSplit);
    }

    private VBox createFeatureBullet(String title, String desc) {
        VBox b = new VBox(2);
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
        Label d = new Label(desc);
        d.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        d.setWrapText(true);
        b.getChildren().addAll(t, d);
        return b;
    }

    private void handleLogin() {
        errorLabel.setVisible(false);
        String u = usernameField.getText();
        String p = passwordField.getText();

        try {
            User user = AuthService.getInstance().login(u, p);
            ViewNavigator.showMainDashboard();
        } catch (ValidationException | AuthenticationException ex) {
            errorLabel.setText(ex.getMessage());
            errorLabel.setVisible(true);
        } catch (Exception ex) {
            errorLabel.setText("Unexpected login error: " + ex.getMessage());
            errorLabel.setVisible(true);
        }
    }
}
