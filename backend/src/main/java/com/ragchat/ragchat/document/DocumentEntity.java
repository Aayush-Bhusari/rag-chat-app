package com.ragchat.ragchat.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Represents a row in the `documents` table.
 *
 * NOTE: We use Spring Data R2DBC annotations, NOT JPA annotations.
 * - @Table instead of @Entity
 * - @Id from org.springframework.data.annotation, NOT javax.persistence
 * - No @Column needed if field names match column names (snake_case auto-mapped)
 *
 * R2DBC entities are simple POJOs — no lazy loading, no proxies,
 * no session management. Much simpler than JPA entities.
 */
@Table("documents")
public class DocumentEntity {

    @Id
    private Long id;
    private String filename;
    private String contentType;
    private LocalDateTime uploadedAt;
    private Integer totalChunks;

    // Default constructor (needed by Spring Data)
    public DocumentEntity() {}

    public DocumentEntity(String filename, String contentType) {
        this.filename = filename;
        this.contentType = contentType;
        this.uploadedAt = LocalDateTime.now();
        this.totalChunks = 0;
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public Integer getTotalChunks() { return totalChunks; }
    public void setTotalChunks(Integer totalChunks) { this.totalChunks = totalChunks; }
}
