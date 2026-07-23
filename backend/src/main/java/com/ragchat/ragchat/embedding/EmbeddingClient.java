package com.ragchat.ragchat.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Calls Ollama's /api/embed endpoint to convert text into vectors.
 *
 * WHAT IS AN EMBEDDING?
 * An embedding is a list of numbers (a vector) that represents the
 * "meaning" of a piece of text. Texts with similar meanings produce
 * vectors that are close together in this number-space. This is how
 * we find "relevant chunks" — we embed the user's question, then
 * find chunks whose embeddings are closest to it.
 *
 * nomic-embed-text produces 768-dimensional vectors (a list of 768 floats).
 *
 * Ollama API shape:
 *   POST /api/embed
 *   { "model": "nomic-embed-text", "input": "some text" }
 *   → { "embeddings": [[0.123, -0.456, ...]] }
 */
@Component
public class EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClient.class);

    private final WebClient ollamaWebClient;
    private final String embedModel;

    public EmbeddingClient(
            WebClient ollamaWebClient,
            @Value("${ollama.embed-model}") String embedModel) {
        this.ollamaWebClient = ollamaWebClient;
        this.embedModel = embedModel;
    }

    /**
     * Embed a single text string.
     * Returns a Mono (reactive single-value) containing the embedding vector.
     */
    public Mono<List<Double>> embed(String text) {
        log.debug("Embedding text ({} chars) with model {}", text.length(), embedModel);

        return ollamaWebClient.post()
                .uri("/api/embed")
                .bodyValue(Map.of(
                        "model", embedModel,
                        "input", text
                ))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    // Ollama returns: { "embeddings": [[0.1, 0.2, ...]] }
                    JsonNode embeddingArray = response.get("embeddings").get(0);
                    List<Double> embedding = new java.util.ArrayList<>();
                    embeddingArray.forEach(node -> embedding.add(node.asDouble()));
                    log.debug("Got embedding with {} dimensions", embedding.size());
                    return embedding;
                })
                .doOnError(e -> log.error("Embedding failed: {}", e.getMessage()));
    }

    /**
     * Format embedding as a pgvector-compatible string.
     * pgvector expects: '[0.1,0.2,0.3,...]'
     */
    public static String toPgVectorString(List<Double> embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding.get(i));
        }
        sb.append("]");
        return sb.toString();
    }
}
