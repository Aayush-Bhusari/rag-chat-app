package com.ragchat.ragchat.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a document's text into overlapping chunks for embedding.
 *
 * WHY CHUNKING?
 * LLMs have context limits, and embedding models work best on
 * focused passages (not entire documents). By splitting a 10-page
 * PDF into ~500-character chunks, we can:
 * 1. Embed each chunk independently
 * 2. Retrieve ONLY the relevant chunks for a given question
 * 3. Fit those chunks into the LLM's context window
 *
 * WHY OVERLAP?
 * If a sentence spans a chunk boundary, the answer might be split
 * across two chunks. Overlap (e.g., 100 chars) means the boundary
 * region appears in BOTH chunks, so we don't lose context.
 *
 * We use character-based splitting here (not token-based) for
 * simplicity. In production, you'd use a tokenizer, but for a
 * portfolio project, character-based is fine and avoids adding
 * a tokenizer dependency.
 */
@Service
public class ChunkingService {

    private static final Logger log = LoggerFactory.getLogger(ChunkingService.class);

    private static final int DEFAULT_CHUNK_SIZE = 500;      // characters per chunk
    private static final int DEFAULT_OVERLAP = 100;          // overlap between chunks

    /**
     * Split text into overlapping chunks.
     *
     * @param text     the full document text
     * @param chunkSize characters per chunk
     * @param overlap   characters of overlap between consecutive chunks
     * @return list of text chunks
     */
    public List<String> chunkText(String text, int chunkSize, int overlap) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        // Clean up: collapse multiple whitespace/newlines into single spaces
        String cleaned = text.replaceAll("\\s+", " ").trim();

        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < cleaned.length()) {
            int end = Math.min(start + chunkSize, cleaned.length());

            // Try to break at a sentence boundary (period, question mark, etc.)
            // so chunks don't end mid-sentence. Look backwards from `end`.
            if (end < cleaned.length()) {
                int lastPeriod = cleaned.lastIndexOf(". ", end);
                int lastQuestion = cleaned.lastIndexOf("? ", end);
                int lastExclaim = cleaned.lastIndexOf("! ", end);
                int bestBreak = Math.max(lastPeriod, Math.max(lastQuestion, lastExclaim));

                // Only use the sentence break if it's within the current chunk
                if (bestBreak > start) {
                    end = bestBreak + 1; // include the period
                }
            }

            String chunk = cleaned.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            // Move forward by (end - overlap), so the next chunk
            // starts `overlap` characters before where this one ended
            start = end - overlap;
            if (start <= 0 && end >= cleaned.length()) break;
            if (start >= cleaned.length()) break;
        }

        log.info("Split {} characters into {} chunks (size={}, overlap={})",
                cleaned.length(), chunks.size(), chunkSize, overlap);
        return chunks;
    }

    /**
     * Convenience overload using default chunk size and overlap.
     */
    public List<String> chunkText(String text) {
        return chunkText(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }
}
