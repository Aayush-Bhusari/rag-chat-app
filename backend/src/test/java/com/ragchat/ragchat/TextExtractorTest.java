package com.ragchat.ragchat;

import com.ragchat.ragchat.document.TextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TextExtractor.
 */
class TextExtractorTest {

    private TextExtractor textExtractor;

    @BeforeEach
    void setUp() {
        textExtractor = new TextExtractor();
    }

    @Test
    @DisplayName("Should extract text from plain text bytes")
    void shouldExtractPlainText() {
        String input = "Hello, this is a test document.";
        byte[] bytes = input.getBytes();

        String result = textExtractor.extract(bytes, "text/plain");

        assertEquals(input, result);
    }

    @Test
    @DisplayName("Should throw exception for unsupported content type")
    void shouldRejectUnsupportedType() {
        byte[] bytes = "test".getBytes();

        assertThrows(IllegalArgumentException.class,
                () -> textExtractor.extract(bytes, "image/png"));
    }

    @Test
    @DisplayName("Should throw exception for null content type")
    void shouldRejectNullContentType() {
        byte[] bytes = "test".getBytes();

        assertThrows(IllegalArgumentException.class,
                () -> textExtractor.extract(bytes, null));
    }
}
