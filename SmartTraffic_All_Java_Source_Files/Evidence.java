package com.smarttraffic.model;

import java.time.LocalDateTime;

/**
 * Photographic / Document Evidence entity.
 */
public class Evidence {
    private int id;
    private String entityType; // CHALLAN, COMPLAINT, EMERGENCY
    private int entityId;
    private String filePath;
    private String fileName;
    private long fileSize;
    private String description;
    private LocalDateTime uploadedAt;

    public Evidence() {
        this.uploadedAt = LocalDateTime.now();
    }

    public Evidence(int id, String entityType, int entityId, String filePath,
                    String fileName, long fileSize, String description, LocalDateTime uploadedAt) {
        this.id = id;
        this.entityType = entityType;
        this.entityId = entityId;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.description = description;
        this.uploadedAt = uploadedAt != null ? uploadedAt : LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
