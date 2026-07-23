package com.ragchat.ragchat.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Represents a row in the `chunks` table.
 *
 * Each chunk holds a piece of a document's text plus its
 * vector embedding (stored in pgvector, but we handle the
 * embedding column manually via custom SQL since R2DBC
 * doesn't natively understand pgvector's vector type).
 */
@Table("chunks")
public class ChunkEntity {

    @Id
    private Long id;
    private Long documentId;
    private Integer chunkIndex;
    private String content;
    // NOTE: We don't map the `embedding` column here because
    // R2DBC can't map pgvector's vector type to a Java type.
    // We insert/query embeddings via custom SQL in the repository.
    private LocalDateTime createdAt;

    public ChunkEntity() {}

    public ChunkEntity(Long documentId, Integer chunkIndex, String content) {
        this.documentId = documentId;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
