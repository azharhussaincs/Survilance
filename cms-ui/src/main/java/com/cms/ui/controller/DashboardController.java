package com.cms.ui.controller;

import com.cms.model.Camera;
import com.cms.model.NvrDevice;
import com.cms.service.AuthService;
import com.cms.service.NvrService;
import com.cms.ui.component.CameraGridCell;
import com.cms.ui.component.NvrTreeItem;
import com.cms.ui.dialog.AddNvrDialog;
import com.cms.ui.util.SceneManager;
import com.cms.ui.util.UIUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;

/**
 * Main dashboard controller managing the NVR tree, grid layout, and live streams.
 */
public class DashboardController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @FXML private TreeView<Object> nvrTreeView;
    @FXML private GridPane cameraGrid;
    @FXML private ScrollPane gridScrollPane;
    @FXML private ComboBox<String> layoutSelector;
    @FXML private Label statusLabel;
    @FXML private Label userLabel;
    @FXML private Label nvrCountLabel;
    @FXML private Label cameraCountLabel;
    @FXML private Button addNvrBtn;
    @FXML private Button deleteNvrBtn;
    @FXML private Button refreshBtn;
    @FXML private ProgressIndicator busyIndicator;
    @FXML private SplitPane mainSplitPane;

    private int gridRows = 2;
    private int gridCols = 2;
    private List<CameraGridCell> gridCells = new ArrayList<>();
    private NvrDevice selectedNvr;

    private static final String[] LAYOUT_OPTIONS = {"1x1", "2x2", "3x3", "4x4", "6x6", "2x3", "3x4"};

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupToolbar();
        setupNvrTree();
        setupGrid(2, 2);
        loadNvrs();
        updateStatusBar();
    }

    private void setupToolbar() {
        userLabel.setText("👤 " + AuthService.getInstance().getCurrentUser().getFullName());
        layoutSelector.setItems(FXCollections.observableArrayList(LAYOUT_OPTIONS));
        layoutSelector.setValue("2x2");
        layoutSelector.setOnAction(e -> applySelectedLayout());
        setBusy(false);
    }

    private void setupNvrTree() {
        TreeItem<Object> root = new TreeItem<>("NVR Devices");
        root.setExpanded(true);
        nvrTreeView.setRoot(root);
        nvrTreeView.setShowRoot(false);
        nvrTreeView.setCellFactory(tv -> new NvrTreeItem());

        nvrTreeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<Object> selected = nvrTreeView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getValue() instanceof Camera camera) {
                    addCameraToNextEmptyCell(camera);
                }
            }
        });

        // Drag from tree
        nvrTreeView.setOnDragDetected(event -> {
            TreeItem<Object> selected = nvrTreeView.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getValue() instanceof Camera camera) {
                Dragboard db = nvrTreeView.startDragAndDrop(TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();
                content.putString("CAMERA:" + camera.getId());
                db.setContent(content);
                event.consume();
            }
        });
    }

    private void loadNvrs() {
        setBusy(true);
        new Thread(() -> {
            List<NvrDevice> nvrs = NvrService.getInstance().getAllNvrsWithCameras();
            Platform.runLater(() -> {
                populateNvrTree(nvrs);
                updateStatusBar();
                setBusy(false);
            });
        }).start();
    }

    private void populateNvrTree(List<NvrDevice> nvrs) {
        TreeItem<Object> root = nvrTreeView.getRoot();
        root.getChildren().clear();
        CameraGridCell.CameraDragRegistry.clear();

        int totalCameras = 0;
        for (NvrDevice nvr : nvrs) {
            TreeItem<Object> nvrItem = new TreeItem<>(nvr);
            nvrItem.setExpanded(true);
            for (Camera cam : nvr.getCameras()) {
                cam.setNvr(nvr);
                nvrItem.getChildren().add(new TreeItem<>(cam));
                CameraGridCell.CameraDragRegistry.register(cam);
                totalCameras++;
            }
            root.getChildren().add(nvrItem);
        }
        nvrCountLabel.setText(String.valueOf(nvrs.size()));
        cameraCountLabel.setText(String.valueOf(totalCameras));
    }

    private void setupGrid(int rows, int cols) {
        this.gridRows = rows;
        this.gridCols = cols;
        gridCells.clear();
        cameraGrid.getChildren().clear();
        cameraGrid.getRowConstraints().clear();
        cameraGrid.getColumnConstraints().clear();

        for (int r = 0; r < rows; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS);
            rc.setPercentHeight(100.0 / rows);
            cameraGrid.getRowConstraints().add(rc);
        }
        for (int c = 0; c < cols; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setPercentWidth(100.0 / cols);
            cameraGrid.getColumnConstraints().add(cc);
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int cellIndex = r * cols + c;
                CameraGridCell cell = new CameraGridCell(cellIndex);
                cell.setOnCameraDrop(this::handleCameraDrop);
                GridPane.setMargin(cell, new Insets(2));
                cameraGrid.add(cell, c, r);
                gridCells.add(cell);
            }
        }
        logger.info("Grid configured: {}x{} ({} cells)", rows, cols, rows * cols);
    }

    private void applySelectedLayout() {
        String layout = layoutSelector.getValue();
        if (layout == null) return;
        String[] parts = layout.split("x");
        if (parts.length == 2) {
            try {
                int r = Integer.parseInt(parts[0]);
                int c = Integer.parseInt(parts[1]);
                // Stop existing streams
                gridCells.forEach(CameraGridCell::stopStream);
                setupGrid(r, c);
            } catch (NumberFormatException e) {
                logger.warn("Invalid layout format: {}", layout);
            }
        }
    }

    private void handleCameraDrop(int cellIndex, Camera camera) {
        if (cellIndex >= 0 && cellIndex < gridCells.size()) {
            CameraGridCell cell = gridCells.get(cellIndex);
            cell.loadCamera(camera);
            logger.info("Camera '{}' loaded into cell {}", camera.getCameraName(), cellIndex);
        }
    }

    private void addCameraToNextEmptyCell(Camera camera) {
        for (CameraGridCell cell : gridCells) {
            if (cell.isEmpty()) {
                cell.loadCamera(camera);
                return;
            }
        }
        // All full – replace first cell
        if (!gridCells.isEmpty()) {
            gridCells.get(0).loadCamera(camera);
        }
    }

    @FXML
    private void handleAddNvr(ActionEvent event) {
        AddNvrDialog dialog = new AddNvrDialog();
        dialog.showAndWait().ifPresent(nvr -> {
            setBusy(true);
            new Thread(() -> {
                NvrService.ConnectionResult result = NvrService.getInstance().connectNvr(nvr);
                Platform.runLater(() -> {
                    setBusy(false);
                    if (result.success()) {
                        // Ensure all new cameras are registered
                        result.cameras().forEach(CameraGridCell.CameraDragRegistry::register);
                        UIUtils.showInfo("Connected", "Successfully connected to " + nvr.getLocationName() +
                            "\nDiscovered " + result.cameras().size() + " cameras.");
                        loadNvrs();
                    } else {
                        UIUtils.showError("Connection Failed", "Could not connect to NVR: " + result.message());
                    }
                });
            }).start();
        });
    }

    @FXML
    private void handleDeleteNvr(ActionEvent event) {
        TreeItem<Object> selected = nvrTreeView.getSelectionModel().getSelectedItem();
        if (selected == null || !(selected.getValue() instanceof NvrDevice nvr)) {
            UIUtils.showWarning("No Selection", "Please select an NVR device to delete.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete NVR '" + nvr.getLocationName() + "' and all its cameras?",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                NvrService.getInstance().deleteNvr(nvr.getId());
                // Stop streams for cameras of this NVR
                gridCells.forEach(cell -> {
                    if (cell.hasCamera() && cell.getCamera().getNvrId().equals(nvr.getId())) {
                        cell.stopStream();
                        cell.clear();
                    }
                });
                loadNvrs();
                UIUtils.showInfo("Deleted", "NVR '" + nvr.getLocationName() + "' has been removed.");
            }
        });
    }

    @FXML
    private void handleRefresh(ActionEvent event) { loadNvrs(); }

    @FXML
    private void handleLogout(ActionEvent event) {
        gridCells.forEach(CameraGridCell::stopStream);
        AuthService.getInstance().logout();
        SceneManager.getInstance().showLogin();
    }

    private void setBusy(boolean busy) {
        busyIndicator.setVisible(busy);
        addNvrBtn.setDisable(busy);
        refreshBtn.setDisable(busy);
    }

    private void updateStatusBar() {
        statusLabel.setText("Ready");
    }
}
