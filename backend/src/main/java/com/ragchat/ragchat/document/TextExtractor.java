package com.ragchat.ragchat.document;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TextExtractor {

    private static final Logger log = LoggerFactory.getLogger(TextExtractor.class);

    public String extract(byte[] fileBytes, String contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException("Content type is required");
        }

        return switch (contentType.toLowerCase()) {
            case "application/pdf" -> extractFromPdf(fileBytes);
            case "text/plain" -> new String(fileBytes);
            default -> throw new IllegalArgumentException(
                    "Unsupported file type: " + contentType +
                    ". Only PDF and text files are supported.");
        };
    }

    private String extractFromPdf(byte[] fileBytes) {
        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.info("Extracted {} characters from PDF ({} pages)",
                    text.length(), document.getNumberOfPages());
            return text;
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract text from PDF: " + e.getMessage(), e);
        }
    }
}
