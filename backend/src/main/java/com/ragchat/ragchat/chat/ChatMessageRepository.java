package com.ragchat.ragchat.chat;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * Reactive repository for chat_messages table.
 *
 * Spring Data automatically implements this interface at runtime —
 * you never write the SQL. Method names like "findByDocumentId"
 * are parsed into SQL: SELECT * FROM chat_messages WHERE document_id = ?
 *
 * "OrderByCreatedAtAsc" adds: ORDER BY created_at ASC
 */
public interface ChatMessageRepository extends ReactiveCrudRepository<ChatMessageEntity, Long> {

    /**
     * Get all messages for a specific document's conversation,
     * ordered chronologically.
     */
    Flux<ChatMessageEntity> findByDocumentIdOrderByCreatedAtAsc(Long documentId);
}
