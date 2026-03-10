package com.cms.ui.component;

import com.cms.model.Camera;
import com.cms.model.NvrDevice;
import javafx.scene.control.TreeCell;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.control.Label;

/**
 * Custom TreeCell for rendering NVR devices and cameras with icons and status indicators.
 */
public class NvrTreeItem extends TreeCell<Object> {

    @Override
    protected void updateItem(Object item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null); setGraphic(null); return;
        }

        if (item instanceof NvrDevice nvr) {
            renderNvrCell(nvr);
        } else if (item instanceof Camera camera) {
            renderCameraCell(camera);
        } else {
            setText(item.toString());
            setGraphic(null);
        }
    }

    private void renderNvrCell(NvrDevice nvr) {
        HBox box = new HBox(6);
        box.setStyle("-fx-alignment: center-left;");

        Label icon = new Label("🖥");
        icon.setStyle("-fx-font-size: 14px;");

        Label name = new Label(nvr.getLocationName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        Circle statusDot = new Circle(5);
        statusDot.setFill(getStatusColor(nvr.getConnectionStatus()));

        Label ipLabel = new Label(" " + nvr.getIpAddress());
        ipLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 10px;");

        Label camCount = new Label(" [" + nvr.getCameras().size() + "]");
        camCount.setStyle("-fx-text-fill: #00b4d8; -fx-font-size: 10px;");

        box.getChildren().addAll(statusDot, icon, name, ipLabel, camCount);
        setGraphic(box);
        setText(null);
    }

    private void renderCameraCell(Camera camera) {
        HBox box = new HBox(6);
        box.setStyle("-fx-alignment: center-left; -fx-padding: 0 0 0 16;");

        Label icon = new Label("📷");
        icon.setStyle("-fx-font-size: 12px;");

        Label name = new Label(camera.getCameraName());
        name.setStyle("-fx-font-size: 11px;");

        Circle dot = new Circle(4);
        dot.setFill(camera.getStatus() == Camera.CameraStatus.ONLINE ? Color.web("#4caf50") : Color.web("#757575"));

        Label ch = new Label("Ch." + camera.getChannelNumber());
        ch.setStyle("-fx-text-fill: #888; -fx-font-size: 9px;");

        // Register for drag (optional, now handled in DashboardController)
        if (camera.getId() != null) {
            CameraGridCell.CameraDragRegistry.register(camera);
        }

        box.getChildren().addAll(dot, icon, name, ch);
        setGraphic(box);
        setText(null);
    }

    private Color getStatusColor(NvrDevice.ConnectionStatus status) {
        return switch (status) {
            case CONNECTED -> Color.web("#4caf50");
            case CONNECTING -> Color.web("#ffb300");
            case ERROR -> Color.web("#f44336");
            case DISCONNECTED -> Color.web("#9e9e9e");
            default -> Color.web("#607d8b");
        };
    }
}
