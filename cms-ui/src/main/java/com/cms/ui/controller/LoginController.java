package com.cms.ui.controller;

import com.cms.service.AuthService;
import com.cms.ui.util.SceneManager;
import com.cms.ui.util.UIUtils;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Login screen controller with modern UI and animations.
 */
public class LoginController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @FXML private StackPane rootPane;
    @FXML private VBox loginCard;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisible;
    @FXML private Button togglePasswordBtn;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    @FXML private Label versionLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private HBox loadingPane;

    private boolean passwordShown = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupKeyboardNavigation();
        setupAnimations();
        versionLabel.setText("v1.0.0");
        errorLabel.setVisible(false);
        loadingPane.setVisible(false);
    }

    private void setupKeyboardNavigation() {
        usernameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.TAB) {
                passwordField.requestFocus();
            }
        });
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleLogin(null);
        });
        passwordVisible.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleLogin(null);
        });
    }

    private void setupAnimations() {
        loginCard.setOpacity(0);
        loginCard.setTranslateY(30);
        PauseTransition pause = new PauseTransition(Duration.millis(100));
        pause.setOnFinished(e -> {
            FadeTransition fade = new FadeTransition(Duration.millis(500), loginCard);
            fade.setFromValue(0);
            fade.setToValue(1);
            TranslateTransition translate = new TranslateTransition(Duration.millis(500), loginCard);
            translate.setFromY(30);
            translate.setToY(0);
            ParallelTransition pt = new ParallelTransition(fade, translate);
            pt.play();
        });
        pause.play();
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordShown ? passwordVisible.getText() : passwordField.getText();

        hideError();
        if (username.isBlank()) { showError("Please enter your username"); usernameField.requestFocus(); return; }
        if (password.isBlank()) { showError("Please enter your password"); return; }

        setLoading(true);

        // Run auth in background thread
        new Thread(() -> {
            AuthService.AuthResult result = AuthService.getInstance().authenticate(username, password);
            Platform.runLater(() -> {
                setLoading(false);
                if (result.success()) {
                    logger.info("Login successful for: {}", username);
                    playSuccessAnimation();
                } else {
                    showError(result.message());
                    shakeAnimation();
                }
            });
        }).start();
    }

    private void playSuccessAnimation() {
        FadeTransition fade = new FadeTransition(Duration.millis(400), rootPane);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setOnFinished(e -> SceneManager.getInstance().showDashboard());
        fade.play();
    }

    @FXML
    private void togglePasswordVisibility(ActionEvent event) {
        passwordShown = !passwordShown;
        if (passwordShown) {
            passwordVisible.setText(passwordField.getText());
            passwordField.setVisible(false); passwordField.setManaged(false);
            passwordVisible.setVisible(true); passwordVisible.setManaged(true);
            togglePasswordBtn.setText("🙈");
            passwordVisible.requestFocus();
            passwordVisible.positionCaret(passwordVisible.getText().length());
        } else {
            passwordField.setText(passwordVisible.getText());
            passwordVisible.setVisible(false); passwordVisible.setManaged(false);
            passwordField.setVisible(true); passwordField.setManaged(true);
            togglePasswordBtn.setText("👁");
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        FadeTransition fade = new FadeTransition(Duration.millis(200), errorLabel);
        fade.setFromValue(0); fade.setToValue(1); fade.play();
    }

    private void hideError() {
        errorLabel.setVisible(false);
    }

    private void setLoading(boolean loading) {
        loadingPane.setVisible(loading);
        loginButton.setDisable(loading);
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
    }

    private void shakeAnimation() {
        TranslateTransition tt = new TranslateTransition(Duration.millis(60), loginCard);
        tt.setFromX(0); tt.setByX(10); tt.setCycleCount(6); tt.setAutoReverse(true);
        tt.play();
    }
}
