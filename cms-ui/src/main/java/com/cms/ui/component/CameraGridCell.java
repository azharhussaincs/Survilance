package com.cms.ui.component;
import java.util.Map;
import java.util.HashMap;
import com.cms.model.Camera;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.web.WebErrorEvent;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

/**
 * A single grid cell for displaying a camera stream.
 * Supports drag-and-drop, RTSP via WebView, and MJPEG display.
 */
public class CameraGridCell extends StackPane {
    private static final Logger logger = LoggerFactory.getLogger(CameraGridCell.class);

    private final int cellIndex;
    private Camera camera;
    private WebView webView;
    private VBox emptyPlaceholder;
    private VBox cameraOverlay;
    private Label cameraNameLabel;
    private Label statusLabel;
    private BiConsumer<Integer, Camera> onCameraDrop;
    private boolean loading = false;

    public CameraGridCell(int cellIndex) {
        this.cellIndex = cellIndex;
        setupUI();
        setupDragDrop();
    }

    private void setupUI() {
        getStyleClass().add("camera-cell");
        setMinSize(160, 120);

        // Empty placeholder
        emptyPlaceholder = new VBox(8);
        emptyPlaceholder.setAlignment(Pos.CENTER);
        emptyPlaceholder.getStyleClass().add("camera-cell-empty");

        Label icon = new Label("📷");
        icon.getStyleClass().add("camera-icon");
        Label hint = new Label("Drag camera here\nor double-click");
        hint.getStyleClass().add("camera-hint");
        Label cellNumLabel = new Label("Cell " + (cellIndex + 1));
        cellNumLabel.getStyleClass().add("cell-number");
        emptyPlaceholder.getChildren().addAll(icon, cellNumLabel, hint);

        // Camera overlay (shown over video)
        cameraOverlay = new VBox();
        cameraOverlay.setAlignment(Pos.TOP_LEFT);
        cameraOverlay.setPickOnBounds(false);
        cameraOverlay.getStyleClass().add("camera-overlay");

        cameraNameLabel = new Label();
        cameraNameLabel.getStyleClass().add("camera-name-label");
        statusLabel = new Label("●");
        statusLabel.getStyleClass().add("camera-status-online");

        HBox topBar = new HBox(6, statusLabel, cameraNameLabel);
        topBar.getStyleClass().add("camera-top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        cameraOverlay.getChildren().add(topBar);
        cameraOverlay.setVisible(false);

        getChildren().addAll(emptyPlaceholder, cameraOverlay);
    }

    private void setupDragDrop() {
        setOnDragOver(event -> {
            if (event.getDragboard().hasString() &&
                event.getDragboard().getString().startsWith("CAMERA:")) {
                event.acceptTransferModes(TransferMode.COPY);
                getStyleClass().add("camera-cell-drag-over");
            }
            event.consume();
        });

        setOnDragExited(event -> {
            getStyleClass().remove("camera-cell-drag-over");
            event.consume();
        });

        setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString() && db.getString().startsWith("CAMERA:")) {
                String cameraIdStr = db.getString().substring("CAMERA:".length());
                try {
                    Long cameraId = Long.parseLong(cameraIdStr);
                    // Find camera from the drag source - handled via callback
                    if (onCameraDrop != null) {
                        // We need the camera object - using a lookup approach
                        Camera draggedCamera = CameraDragRegistry.get(cameraId);
                        if (draggedCamera != null) {
                            onCameraDrop.accept(cellIndex, draggedCamera);
                        }
                    }
                    success = true;
                } catch (NumberFormatException e) {
                    logger.warn("Invalid camera id in drag: {}", cameraIdStr);
                }
            }
            event.setDropCompleted(success);
            getStyleClass().remove("camera-cell-drag-over");
            event.consume();
        });
    }

    public void loadCamera(Camera camera) {
        this.camera = camera;
        emptyPlaceholder.setVisible(false);
        cameraOverlay.setVisible(true);
        cameraNameLabel.setText(camera.getCameraName());
        statusLabel.getStyleClass().clear();
        statusLabel.getStyleClass().add("camera-status-connecting");
        statusLabel.setText("●");

        // Remove old webview if present
        getChildren().removeIf(n -> n instanceof WebView);

        startStream(camera);
    }

    private void startStream(Camera camera) {
        String streamUrl = camera.getStreamUrl();
        if (streamUrl == null || streamUrl.isBlank()) {
            showOfflineState("No stream URL configured");
            return;
        }

        logger.info("Starting stream for camera: {} (Type: {}, URL: {})",
            camera.getCameraName(), camera.getStreamType(), streamUrl);

        switch (camera.getStreamType()) {
            case RTSP -> loadRtspStream(camera);
            case WEBVIEW -> loadWebViewStream(streamUrl);
            case MJPEG -> loadMjpegStream(streamUrl);
            case HTTP_SNAPSHOT -> loadSnapshotView(camera);
            default -> loadRtspStream(camera);
        }
    }

    private void loadRtspStream(Camera camera) {
        // For RTSP, use WebView with an embedded HTML5 player or show stream info
        // In production, JavaCV/VLC bindings would be used here
        // For this implementation we use WebView with a proxy approach
        Platform.runLater(() -> {
            webView = createWebView();
            getChildren().add(1, webView); // Add behind overlay

            // Load a simple monitoring page
            String html = buildRtspPlaceholderHtml(camera);
            webView.getEngine().loadContent(html);
            webView.toBack();
            cameraOverlay.toFront();

            updateStatus(true);
        });
    }

    private void loadWebViewStream(String url) {
        Platform.runLater(() -> {
            webView = createWebView();
            getChildren().add(1, webView);
            webView.getEngine().load(url);
            webView.toBack();
            cameraOverlay.toFront();
            updateStatus(true);
        });
    }

    private void loadMjpegStream(String url) {
        // MJPEG via WebView img tag
        Platform.runLater(() -> {
            webView = createWebView();
            getChildren().add(1, webView);
            String html = "<html><body style='margin:0;background:#000;display:flex;align-items:center;justify-content:center;height:100vh;'>" +
                "<img src='" + url + "' style='max-width:100%;max-height:100%;object-fit:contain' " +
                "onerror=\"this.style.display='none'; console.error('Failed to load MJPEG stream: ' + this.src)\"/></body></html>";
            webView.getEngine().loadContent(html);
            webView.toBack();
            cameraOverlay.toFront();
            updateStatus(true);
        });
    }

    private void loadSnapshotView(Camera camera) {
        Platform.runLater(() -> {
            webView = createWebView();
            getChildren().add(1, webView);
            String snapshotUrl = camera.getSnapshotUrl() != null ? camera.getSnapshotUrl() : camera.getStreamUrl();
            String html = buildSnapshotHtml(snapshotUrl, camera.getCameraName());
            webView.getEngine().loadContent(html);
            webView.toBack();
            cameraOverlay.toFront();
            updateStatus(true);
        });
    }

    private WebView createWebView() {
        WebView wv = new WebView();
        wv.getStyleClass().add("stream-view");

        // Add event listeners for better debugging
        wv.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.FAILED) {
                Throwable exception = wv.getEngine().getLoadWorker().getException();
                String msg = exception != null ? exception.getMessage() : "Unknown error";
                logger.error("WebView failed to load for camera {}: {}",
                    camera != null ? camera.getCameraName() : "unknown", msg);
                showOfflineState("Load failed: " + msg);
            }
        });

        wv.getEngine().setOnError(event -> {
            logger.error("WebView engine error for camera {}: {}",
                camera != null ? camera.getCameraName() : "unknown", event.getMessage());
        });

        // Bridge console.log from WebView to SLF4J if accessible
        wv.getEngine().javaScriptEnabledProperty().set(true);
        try {
            // Using reflection or a safer way if available, but for now we'll just check if we can access it
            // or better yet, just don't use it if it's restricted in this environment
            // com.sun.javafx.webkit.WebConsoleListener.setDefaultListener((webView1, message, lineNumber, sourceId) -> {
            //    logger.info("[WebView Console] {}:{}: {}", sourceId, lineNumber, message);
            // });
        } catch (Throwable t) {
            logger.warn("Could not register WebView console listener: {}", t.getMessage());
        }

        return wv;
    }

    private String buildRtspPlaceholderHtml(Camera camera) {
        String snapshotHtml = "";
        if (camera.getSnapshotUrl() != null && !camera.getSnapshotUrl().isBlank()) {
            snapshotHtml = String.format("<img src='%s' style='max-width:80%%;max-height:40vh;border:1px solid #333;margin-bottom:10px;' onerror=\"this.style.display='none'\">", 
                camera.getSnapshotUrl());
        }
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head><style>
                body { margin:0; background:#0a0a0a; display:flex; align-items:center; justify-content:center;
                       height:100vh; font-family:Arial,sans-serif; color:#00e5ff; }
                .container { text-align:center; width:100%%; }
                .camera-icon { font-size:48px; margin-bottom:10px; }
                .stream-info { font-size:11px; color:#666; margin-top:8px; word-break:break-all; padding:0 10px; }
                .pulse { animation: pulse 2s infinite; }
                @keyframes pulse { 0%%,100%%{opacity:1} 50%%{opacity:0.4} }
                .live-badge { background:#e53935; color:white; padding:2px 8px; border-radius:3px;
                              font-size:10px; font-weight:bold; display:inline-block; margin-top:6px; }
            </style></head>
            <body>
            <div class="container">
                %s
                <div class="camera-icon pulse">📹</div>
                <div style="font-weight:bold;font-size:14px;">%s</div>
                <div class="live-badge">● LIVE</div>
                <div class="stream-info">%s</div>
            </div>
            </body></html>
            """, snapshotHtml, camera.getCameraName(), camera.getStreamUrl());
    }

    private String buildSnapshotHtml(String url, String name) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head><style>
                body { margin:0; background:#000; overflow:hidden; }
                img { width:100%%; height:100vh; object-fit:cover; }
            </style>
            <script>
                function reload() { document.getElementById('snap').src='%s?t='+Date.now(); }
                setInterval(reload, 3000);
            </script>
            </head>
            <body><img id='snap' src='%s' alt='%s'/></body>
            </html>
            """, url, url, name);
    }

    private void updateStatus(boolean online) {
        statusLabel.getStyleClass().clear();
        statusLabel.getStyleClass().add(online ? "camera-status-online" : "camera-status-offline");
        statusLabel.setText("●");
    }

    private void showOfflineState(String reason) {
        Platform.runLater(() -> {
            getChildren().removeIf(n -> n instanceof WebView);
            statusLabel.getStyleClass().clear();
            statusLabel.getStyleClass().add("camera-status-offline");
            cameraNameLabel.setText(camera.getCameraName() + " (Offline)");
            
            // Add error message to overlay
            Label errorMsg = new Label(reason);
            errorMsg.setStyle("-fx-text-fill: #ff5252; -fx-font-size: 10px; -fx-padding: 5 10 5 10;");
            errorMsg.setWrapText(true);
            if (cameraOverlay.getChildren().size() > 1) {
                cameraOverlay.getChildren().remove(1, cameraOverlay.getChildren().size());
            }
            cameraOverlay.getChildren().add(errorMsg);
            
            cameraOverlay.setVisible(true);
            emptyPlaceholder.setVisible(false);
        });
    }

    public void stopStream() {
        if (webView != null) {
            Platform.runLater(() -> {
                webView.getEngine().load("about:blank");
                getChildren().remove(webView);
                webView = null;
            });
        }
    }

    public void clear() {
        stopStream();
        this.camera = null;
        emptyPlaceholder.setVisible(true);
        cameraOverlay.setVisible(false);
    }

    public boolean isEmpty() { return camera == null; }
    public boolean hasCamera() { return camera != null; }
    public Camera getCamera() { return camera; }
    public int getCellIndex() { return cellIndex; }
    public void setOnCameraDrop(BiConsumer<Integer, Camera> handler) { this.onCameraDrop = handler; }

    /**
     * Registry for drag-and-drop camera lookup.
     */
    public static class CameraDragRegistry {
        private static final Map<Long, Camera> registry = new java.util.HashMap<>();
        public static void register(Camera camera) { if (camera.getId() != null) registry.put(camera.getId(), camera); }
        public static Camera get(Long id) { return registry.get(id); }
        public static void clear() { registry.clear(); }
    }
}
