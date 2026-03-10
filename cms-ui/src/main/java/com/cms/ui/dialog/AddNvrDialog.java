package com.cms.ui.dialog;

import com.cms.model.NvrDevice;
import com.cms.util.ValidationUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.util.Optional;

/**
 * Dialog for adding a new NVR device.
 */
public class AddNvrDialog extends Dialog<NvrDevice> {

    private TextField locationField;
    private TextField ipField;
    private TextField portField;
    private TextField usernameField;
    private PasswordField passwordField;
    private ComboBox<NvrDevice.NvrBrand> brandCombo;
    private Label validationLabel;
    private ProgressIndicator connecting;

    public AddNvrDialog() {
        setupDialog();
    }

    private void setupDialog() {
        setTitle("Add NVR Device");
        setHeaderText("Connect to a new NVR/DVR");
        getDialogPane().getStylesheets().add(
            getClass().getResource("/com/cms/ui/css/dark-theme.css").toExternalForm());
        getDialogPane().getStyleClass().add("nvr-dialog");

        // Form layout
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.setMinWidth(420);

        // Location Name
        locationField = new TextField();
        locationField.setPromptText("e.g. Warehouse NVR, Office DVR");
        locationField.getStyleClass().add("dialog-field");

        // Brand
        brandCombo = new ComboBox<>();
        brandCombo.getItems().addAll(NvrDevice.NvrBrand.values());
        brandCombo.setValue(NvrDevice.NvrBrand.ONVIF);
        brandCombo.setMaxWidth(Double.MAX_VALUE);
        brandCombo.setConverter(new StringConverter<>() {
            @Override public String toString(NvrDevice.NvrBrand b) { return b != null ? b.getDisplayName() : ""; }
            @Override public NvrDevice.NvrBrand fromString(String s) { return null; }
        });
        brandCombo.setOnAction(e -> updatePortDefault());

        // IP Address
        ipField = new TextField();
        ipField.setPromptText("e.g. 192.168.1.100 or nvr.example.com");
        ipField.getStyleClass().add("dialog-field");

        // Port
        portField = new TextField();
        portField.setPromptText("Leave blank for default");
        portField.getStyleClass().add("dialog-field");
        portField.setPrefWidth(100);
        updatePortDefault();

        // Username
        usernameField = new TextField();
        usernameField.setPromptText("admin");
        usernameField.getStyleClass().add("dialog-field");

        // Password
        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("dialog-field");

        // Validation label
        validationLabel = new Label();
        validationLabel.getStyleClass().add("validation-error");
        validationLabel.setWrapText(true);
        validationLabel.setVisible(false);

        // Loading
        connecting = new ProgressIndicator();
        connecting.setMaxSize(20, 20);
        connecting.setVisible(false);

        int row = 0;
        grid.add(makeLabel("Location Name *"), 0, row);
        grid.add(locationField, 1, row++);
        grid.add(makeLabel("Brand / Protocol *"), 0, row);
        grid.add(brandCombo, 1, row++);
        grid.add(makeLabel("IP Address / URL *"), 0, row);
        grid.add(ipField, 1, row++);
        grid.add(makeLabel("Port"), 0, row);
        HBox portBox = new HBox(8, portField, new Label("(optional)"));
        portBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(portBox, 1, row++);
        grid.add(makeLabel("Username"), 0, row);
        grid.add(usernameField, 1, row++);
        grid.add(makeLabel("Password"), 0, row);
        grid.add(passwordField, 1, row++);
        grid.add(validationLabel, 0, row, 2, 1);

        GridPane.setHgrow(locationField, Priority.ALWAYS);
        GridPane.setHgrow(brandCombo, Priority.ALWAYS);
        GridPane.setHgrow(ipField, Priority.ALWAYS);
        GridPane.setHgrow(usernameField, Priority.ALWAYS);
        GridPane.setHgrow(passwordField, Priority.ALWAYS);

        getDialogPane().setContent(grid);

        ButtonType connectBtn = new ButtonType("Connect", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(connectBtn, cancelBtn);

        // Validate on Connect
        Button connectButton = (Button) getDialogPane().lookupButton(connectBtn);
        connectButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!validate()) event.consume();
        });

        setResultConverter(btnType -> {
            if (btnType == connectBtn) {
                return buildNvrDevice();
            }
            return null;
        });

        Platform.runLater(() -> locationField.requestFocus());
    }

    private boolean validate() {
        validationLabel.setVisible(false);
        if (locationField.getText().isBlank()) { showError("Location name is required"); return false; }
        if (ipField.getText().isBlank()) { showError("IP address or URL is required"); return false; }
        String ip = ipField.getText().trim();
        if (!ValidationUtil.isValidIpOrHostname(ip) && !ValidationUtil.isValidUrl(ip)) {
            showError("Invalid IP address or hostname format"); return false;
        }
        String port = portField.getText().trim();
        if (!port.isBlank() && !ValidationUtil.isValidPort(port)) {
            showError("Invalid port number (must be 1-65535)"); return false;
        }
        return true;
    }

    private NvrDevice buildNvrDevice() {
        NvrDevice nvr = new NvrDevice();
        nvr.setLocationName(locationField.getText().trim());
        nvr.setBrand(brandCombo.getValue());
        nvr.setProtocol(NvrDevice.NvrProtocol.ONVIF);

        String ip = ipField.getText().trim();
        // Check if URL contains port
        int urlPort = ValidationUtil.extractPortFromUrl(ip.startsWith("http") ? ip : "http://" + ip);
        nvr.setIpAddress(ValidationUtil.extractHostFromUrl(ip));

        String portText = portField.getText().trim();
        if (!portText.isBlank()) {
            nvr.setPort(Integer.parseInt(portText));
        } else if (urlPort > 0) {
            nvr.setPort(urlPort);
        } else {
            nvr.setPort(brandCombo.getValue().getDefaultHttpPort());
        }

        nvr.setUsername(usernameField.getText().trim());
        nvr.setPassword(passwordField.getText());
        
        // Ensure ipAddress is preserved if it was a full URL
        if (ip.startsWith("http")) {
            nvr.setIpAddress(ip);
        }
        
        return nvr;
    }

    private void updatePortDefault() {
        NvrDevice.NvrBrand brand = brandCombo.getValue();
        if (brand != null && portField.getText().isBlank()) {
            portField.setPromptText("Default: " + brand.getDefaultHttpPort());
        }
    }

    private void showError(String msg) {
        validationLabel.setText("⚠ " + msg);
        validationLabel.setVisible(true);
    }

    private Label makeLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #ccc; -fx-font-size: 12px;");
        return l;
    }
}
