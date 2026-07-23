package com.ragchat.ragchat.document;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for the `documents` table.
 *
 * ReactiveCrudRepository gives us free CRUD methods that return
 * Mono/Flux instead of blocking:
 *   - save(entity)    → Mono<DocumentEntity>
 *   - findById(id)    → Mono<DocumentEntity>
 *   - findAll()       → Flux<DocumentEntity>
 *   - deleteById(id)  → Mono<Void>
 *
 * We can also define custom queries with @Query.
 */
public interface DocumentRepository extends ReactiveCrudRepository<DocumentEntity, Long> {

    @Modifying
    @Query("UPDATE documents SET total_chunks = :totalChunks WHERE id = :id")
    Mono<Void> updateTotalChunks(Long id, Integer totalChunks);
}
