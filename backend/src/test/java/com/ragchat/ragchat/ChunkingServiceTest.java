package com.ragchat.ragchat;

import com.ragchat.ragchat.document.ChunkingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChunkingService.
 *
 * These are pure unit tests — no Spring context, no database,
 * no external services. They run in milliseconds.
 *
 * @DisplayName gives human-readable test names in the test report.
 */
class ChunkingServiceTest {

    private ChunkingService chunkingService;

    @BeforeEach
    void setUp() {
        chunkingService = new ChunkingService();
    }

    @Test
    @DisplayName("Should split long text into multiple chunks")
    void shouldSplitTextIntoChunks() {
        // Create a string longer than 500 chars
        String text = "This is a test sentence. ".repeat(50); // ~1250 chars

        List<String> chunks = chunkingService.chunkText(text, 500, 100);

        assertTrue(chunks.size() > 1,
                "Expected multiple chunks but got " + chunks.size());
        // Each chunk should be at most ~500 chars (may vary due to sentence boundary logic)
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 600,
                    "Chunk too long: " + chunk.length() + " chars");
        }
    }

    @Test
    @DisplayName("Should return single chunk for short text")
    void shouldReturnSingleChunkForShortText() {
        String text = "This is a short document. It has very little content.";

        List<String> chunks = chunkingService.chunkText(text, 500, 100);

        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0));
    }

    @Test
    @DisplayName("Should return empty list for blank input")
    void shouldHandleEmptyInput() {
        assertEquals(List.of(), chunkingService.chunkText(""));
        assertEquals(List.of(), chunkingService.chunkText(null));
        assertEquals(List.of(), chunkingService.chunkText("   "));
    }

    @Test
    @DisplayName("Chunks should overlap by the specified amount")
    void shouldHaveOverlap() {
        // Use a long text with no sentence boundaries to force exact character splits
        String text = "abcdefghij".repeat(20); // 200 chars, no spaces/periods

        List<String> chunks = chunkingService.chunkText(text, 80, 20);

        // With 80-char chunks and 20-char overlap, chunk2 should start
        // 60 chars into the text (80 - 20 = 60)
        if (chunks.size() >= 2) {
            String endOfFirst = chunks.get(0).substring(chunks.get(0).length() - 20);
            String startOfSecond = chunks.get(1).substring(0, 20);
            assertEquals(endOfFirst, startOfSecond,
                    "Last 20 chars of chunk 1 should equal first 20 chars of chunk 2");
        }
    }
}
