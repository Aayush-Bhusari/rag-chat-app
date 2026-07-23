package com.ragchat.ragchat.document;

import com.ragchat.ragchat.embedding.EmbeddingClient;
import com.ragchat.ragchat.retrieval.VectorSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.core.io.buffer.DataBufferUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Orchestrates the full document processing pipeline:
 *   upload → extract text → chunk → embed → store in pgvector
 *
 * REACTIVE CHAIN EXPLAINED:
 * Instead of step1(); step2(); step3(); (blocking, sequential),
 * reactive code chains steps with operators:
 *
 *   readFileBytes()                    → Mono<byte[]>
 *     .map(bytes -> extractText())     → Mono<String>
 *     .flatMap(text -> chunkAndEmbed())→ Mono<DocumentEntity>
 *
 * Each step runs when the previous one completes, but NO thread
 * is blocked while waiting. The thread is free to handle other
 * requests in between.
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final VectorSearchRepository vectorSearchRepository;
    private final TextExtractor textExtractor;
    private final ChunkingService chunkingService;
    private final EmbeddingClient embeddingClient;

    public DocumentService(
            DocumentRepository documentRepository,
            VectorSearchRepository vectorSearchRepository,
            TextExtractor textExtractor,
            ChunkingService chunkingService,
            EmbeddingClient embeddingClient) {
        this.documentRepository = documentRepository;
        this.vectorSearchRepository = vectorSearchRepository;
        this.textExtractor = textExtractor;
        this.chunkingService = chunkingService;
        this.embeddingClient = embeddingClient;
    }

    /**
     * Process an uploaded file end-to-end.
     *
     * @param filePart the uploaded file from the HTTP request
     * @return the saved DocumentEntity with chunk count
     */
    public Mono<DocumentEntity> processUpload(FilePart filePart) {
        String filename = filePart.filename();
        String contentType = filePart.headers().getContentType() != null
                ? filePart.headers().getContentType().toString()
                : guessContentType(filename);

        log.info("Processing upload: {} ({})", filename, contentType);

        // Step 1: Read all bytes from the uploaded file into memory
        return DataBufferUtils.join(filePart.content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                // Step 2: Extract text from the bytes
                .map(bytes -> {
                    String text = textExtractor.extract(bytes, contentType);
                    log.info("Extracted {} characters from {}", text.length(), filename);
                    return text;
                })
                // Step 3: Save document record to DB, then chunk + embed
                .flatMap(text -> {
                    DocumentEntity doc = new DocumentEntity(filename, contentType);
                    return documentRepository.save(doc)
                            .flatMap(savedDoc -> chunkAndEmbed(savedDoc, text));
                });
    }

    /**
     * Split text into chunks, embed each one, and store in pgvector.
     *
     * flatMapSequential processes chunks one at a time (not in parallel)
     * to avoid overwhelming Ollama's embedding endpoint.
     */
    private Mono<DocumentEntity> chunkAndEmbed(DocumentEntity doc, String text) {
        List<String> chunks = chunkingService.chunkText(text);
        log.info("Document {} split into {} chunks", doc.getId(), chunks.size());

        AtomicInteger index = new AtomicInteger(0);

        return Flux.fromIterable(chunks)
                .flatMapSequential(chunkText -> {
                    int chunkIndex = index.getAndIncrement();
                    // Embed this chunk, then store chunk + embedding in pgvector
                    return embeddingClient.embed(chunkText)
                            .flatMap(embedding -> vectorSearchRepository
                                    .insertChunkWithEmbedding(
                                            doc.getId(), chunkIndex,
                                            chunkText, embedding))
                            .doOnSuccess(v -> log.debug(
                                    "Stored chunk {}/{}", chunkIndex + 1, chunks.size()));
                })
                .then(Mono.defer(() -> {
                    // Update document with total chunk count
                    doc.setTotalChunks(chunks.size());
                    return documentRepository.updateTotalChunks(doc.getId(), chunks.size())
                            .thenReturn(doc);
                }));
    }

    /**
     * Get all documents (for the frontend to list them).
     */
    public Flux<DocumentEntity> getAllDocuments() {
        return documentRepository.findAll();
    }

    /**
     * Get a single document by ID.
     */
    public Mono<DocumentEntity> getDocument(Long id) {
        return documentRepository.findById(id);
    }

    private String guessContentType(String filename) {
        if (filename.toLowerCase().endsWith(".pdf")) return "application/pdf";
        if (filename.toLowerCase().endsWith(".txt")) return "text/plain";
        return "text/plain";
    }
}
