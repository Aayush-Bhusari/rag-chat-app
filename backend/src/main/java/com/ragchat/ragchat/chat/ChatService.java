package com.ragchat.ragchat.chat;

import com.ragchat.ragchat.embedding.EmbeddingClient;
import com.ragchat.ragchat.retrieval.VectorSearchRepository;
import com.ragchat.ragchat.retrieval.VectorSearchRepository.ChunkSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The RAG orchestration layer — the heart of the application.
 *
 * RAG (Retrieval-Augmented Generation) flow:
 * 1. EMBED:    Convert user's question into a vector
 * 2. RETRIEVE: Find the most similar document chunks via pgvector
 * 3. AUGMENT:  Build a prompt that includes the retrieved context
 * 4. GENERATE: Stream the LLM's response token by token
 *
 * WHY RAG instead of just asking the LLM?
 * LLMs can only answer based on their training data. They haven't
 * read YOUR document. RAG solves this by injecting relevant parts
 * of your document into the prompt, so the LLM answers based on
 * your actual content — dramatically reducing hallucination.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final int TOP_K = 5;  // number of chunks to retrieve

    private final EmbeddingClient embeddingClient;
    private final VectorSearchRepository vectorSearchRepository;
    private final OllamaChatClient ollamaChatClient;
    private final ChatMessageRepository chatMessageRepository;

    public ChatService(
            EmbeddingClient embeddingClient,
            VectorSearchRepository vectorSearchRepository,
            OllamaChatClient ollamaChatClient,
            ChatMessageRepository chatMessageRepository) {
        this.embeddingClient = embeddingClient;
        this.vectorSearchRepository = vectorSearchRepository;
        this.ollamaChatClient = ollamaChatClient;
        this.chatMessageRepository = chatMessageRepository;
    }

    /**
     * Full RAG pipeline: embed → retrieve → augment → generate (streaming).
     *
     * REACTIVE CHAIN:
     * embeddingClient.embed(question)                          → Mono<List<Double>>
     *   .flatMapMany(vec -> findSimilarChunks + buildPrompt)   → Flux<String> (tokens)
     *
     * flatMapMany = "take a single value, produce a stream of many values"
     * Perfect for: one embedding → many streamed tokens.
     */
    public Flux<String> streamAnswer(Long documentId, String question) {
        log.info("RAG query on doc {}: {}", documentId, question);

        // Save the user's message first
        ChatMessageEntity userMsg = new ChatMessageEntity(documentId, "user", question);

        return chatMessageRepository.save(userMsg)
                // Step 1: Embed the user's question
                .then(embeddingClient.embed(question))
                // Step 2: Retrieve similar chunks + Step 3-4: Build prompt and stream
                .flatMapMany(queryEmbedding ->
                        vectorSearchRepository.findSimilarChunks(documentId, queryEmbedding, TOP_K)
                                .collectList()
                                .flatMapMany(chunks -> {
                                    String prompt = buildPrompt(chunks, question);
                                    log.debug("Built prompt with {} chunks", chunks.size());

                                    // Step 4: Stream the LLM response
                                    // We also collect all tokens to save the full response later
                                    StringBuilder fullResponse = new StringBuilder();

                                    return ollamaChatClient.streamChat(prompt, question)
                                            .doOnNext(fullResponse::append)
                                            .doOnComplete(() -> {
                                                // Save assistant's complete response to DB
                                                ChatMessageEntity assistantMsg = new ChatMessageEntity(
                                                        documentId, "assistant", fullResponse.toString());
                                                chatMessageRepository.save(assistantMsg)
                                                        .subscribe(); // fire-and-forget save
                                            });
                                })
                );
    }

    /**
     * Build the system prompt with retrieved context.
     *
     * This is the "augmented" part of RAG — we're augmenting the
     * LLM's knowledge with specific passages from the document.
     */
    private String buildPrompt(List<ChunkSearchResult> chunks, String question) {
        String context = chunks.stream()
                .map(chunk -> String.format(
                        "[Chunk %d (similarity: %.2f)]:\n%s",
                        chunk.chunkIndex(), chunk.similarity(), chunk.content()))
                .collect(Collectors.joining("\n\n"));

        return """
                You are a helpful assistant that answers questions based on the provided document context.
                
                RULES:
                - Answer ONLY based on the provided context below
                - If the context doesn't contain enough information to answer, say so honestly
                - Quote specific parts of the context when relevant
                - Keep answers clear and concise
                
                DOCUMENT CONTEXT:
                %s
                
                Answer the user's question based on the context above.
                """.formatted(context);
    }

    /**
     * Get chat history for a document.
     */
    public Flux<ChatMessageEntity> getChatHistory(Long documentId) {
        return chatMessageRepository.findByDocumentIdOrderByCreatedAtAsc(documentId);
    }
}
