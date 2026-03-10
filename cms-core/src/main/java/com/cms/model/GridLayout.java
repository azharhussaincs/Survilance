package com.cms.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a saved grid layout configuration.
 */
public class GridLayout {
    private Long id;
    private String layoutName;
    private int rows;
    private int columns;
    private Map<Integer, Long> cellCameraMap = new HashMap<>(); // cellIndex -> cameraId
    private LocalDateTime createdAt;
    private Long userId;

    public GridLayout() {}

    public GridLayout(String layoutName, int rows, int columns) {
        this.layoutName = layoutName;
        this.rows = rows;
        this.columns = columns;
    }

    public int getTotalCells() { return rows * columns; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLayoutName() { return layoutName; }
    public void setLayoutName(String layoutName) { this.layoutName = layoutName; }
    public int getRows() { return rows; }
    public void setRows(int rows) { this.rows = rows; }
    public int getColumns() { return columns; }
    public void setColumns(int columns) { this.columns = columns; }
    public Map<Integer, Long> getCellCameraMap() { return cellCameraMap; }
    public void setCellCameraMap(Map<Integer, Long> cellCameraMap) { this.cellCameraMap = cellCameraMap; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    @Override
    public String toString() {
        return layoutName + " (" + rows + "x" + columns + ")";
    }
}
