package com.ragchat.ragchat.chat;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Represents a row in the `chat_messages` table.
 * Stores both user messages and assistant responses
 * so the conversation persists across page refreshes.
 */
@Table("chat_messages")
public class ChatMessageEntity {

    @Id
    private Long id;
    private Long documentId;
    private String role;        // "user" or "assistant"
    private String content;
    private LocalDateTime createdAt;

    public ChatMessageEntity() {}

    public ChatMessageEntity(Long documentId, String role, String content) {
        this.documentId = documentId;
        this.role = role;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
