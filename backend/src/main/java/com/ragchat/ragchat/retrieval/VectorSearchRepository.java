package com.ragchat.ragchat.retrieval;

import com.ragchat.ragchat.embedding.EmbeddingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Uses pgvector's cosine distance operator to find the most
 * semantically similar chunks to a given query embedding.
 *
 * HOW VECTOR SIMILARITY SEARCH WORKS:
 * 1. User asks: "What is the refund policy?"
 * 2. We embed that question → a 768-dim vector
 * 3. We compare that vector against every chunk's embedding
 *    using cosine distance (pgvector's <=> operator)
 * 4. Return the top-k closest chunks — these are the most
 *    "semantically relevant" passages from the document
 *
 * pgvector operators:
 *   <=>  cosine distance  (0 = identical, 2 = opposite)
 *   <->  L2 distance      (euclidean)
 *   <#>  inner product     (negative, so smaller = more similar)
 *
 * We use cosine distance because it's direction-based (ignores
 * magnitude), which works well for comparing text embeddings.
 *
 * We use DatabaseClient (Spring's reactive low-level SQL client)
 * instead of a repository interface because R2DBC repositories
 * don't understand pgvector's custom types and operators.
 */
@Repository
public class VectorSearchRepository {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchRepository.class);

    private final DatabaseClient db;

    public VectorSearchRepository(DatabaseClient db) {
        this.db = db;
    }

    /**
     * Insert a chunk with its embedding vector.
     */
    public Mono<Void> insertChunkWithEmbedding(
            Long documentId, int chunkIndex, String content, List<Double> embedding) {

        String vectorStr = EmbeddingClient.toPgVectorString(embedding);

        return db.sql("""
                INSERT INTO chunks (document_id, chunk_index, content, embedding, created_at)
                VALUES (:docId, :idx, :content, CAST(:embedding AS vector), NOW())
                """)
                .bind("docId", documentId)
                .bind("idx", chunkIndex)
                .bind("content", content)
                .bind("embedding", vectorStr)
                .then();
    }

    /**
     * Find the top-k most similar chunks to a query embedding.
     * Returns chunk content ordered by similarity (most similar first).
     */
    public Flux<ChunkSearchResult> findSimilarChunks(
            Long documentId, List<Double> queryEmbedding, int topK) {

        String vectorStr = EmbeddingClient.toPgVectorString(queryEmbedding);
        log.debug("Searching for top-{} chunks in document {}", topK, documentId);

        return db.sql("""
                SELECT id, chunk_index, content,
                       1 - (embedding <=> CAST(:queryVec AS vector)) AS similarity
                FROM chunks
                WHERE document_id = :docId
                  AND embedding IS NOT NULL
                ORDER BY embedding <=> CAST(:queryVec AS vector)
                LIMIT :topK
                """)
                .bind("docId", documentId)
                .bind("queryVec", vectorStr)
                .bind("topK", topK)
                .map((row, metadata) -> new ChunkSearchResult(
                        row.get("id", Long.class),
                        row.get("chunk_index", Integer.class),
                        row.get("content", String.class),
                        row.get("similarity", Double.class)
                ))
                .all()
                .doOnNext(result -> log.debug(
                        "  Chunk #{} similarity={:.4f}: {}...",
                        result.chunkIndex(), result.similarity(),
                        result.content().substring(0, Math.min(80, result.content().length()))));
    }

    /**
     * Result record for a similarity search hit.
     * Java records are immutable data carriers — like a class
     * with final fields, constructor, getters, equals, hashCode
     * all auto-generated. Perfect for query results.
     */
    public record ChunkSearchResult(
            Long id,
            Integer chunkIndex,
            String content,
            Double similarity
    ) {}
}
