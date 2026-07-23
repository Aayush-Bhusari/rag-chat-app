package com.ragchat.ragchat.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller for document operations.
 *
 * @RestController = @Controller + @ResponseBody
 * (every method's return value is serialized to JSON automatically)
 *
 * @CrossOrigin allows the React frontend (localhost:5173) to call
 * these endpoints. Without it, the browser blocks the request due
 * to Same-Origin Policy (CORS).
 */
@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")   // Allow frontend dev server; tighten in production
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Upload a document (PDF or text file).
     *
     * The frontend sends a multipart/form-data request with the file
     * under the field name "file". Spring WebFlux gives us a FilePart
     * (reactive file handle) instead of MultipartFile (which is blocking).
     *
     * POST /api/documents/upload
     * Content-Type: multipart/form-data
     * Body: file=<the PDF or .txt file>
     *
     * Returns the saved document metadata (including how many chunks were created).
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<DocumentEntity> uploadDocument(@RequestPart("file") FilePart file) {
        log.info("Received upload: {}", file.filename());
        return documentService.processUpload(file);
    }

    /**
     * List all uploaded documents.
     * GET /api/documents
     */
    @GetMapping
    public Flux<DocumentEntity> listDocuments() {
        return documentService.getAllDocuments();
    }

    /**
     * Get a single document by ID.
     * GET /api/documents/{id}
     */
    @GetMapping("/{id}")
    public Mono<DocumentEntity> getDocument(@PathVariable Long id) {
        return documentService.getDocument(id);
    }
}
