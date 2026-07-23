package com.ragchat.ragchat.chat;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Streams chat completions from Ollama's /api/chat endpoint.
 *
 * HOW STREAMING WORKS WITH OLLAMA:
 * When you set "stream": true, Ollama sends the response as a
 * series of newline-delimited JSON objects, each containing one
 * token (word fragment):
 *
 *   {"message":{"content":"The"},"done":false}
 *   {"message":{"content":" refund"},"done":false}
 *   {"message":{"content":" policy"},"done":false}
 *   ...
 *   {"message":{"content":""},"done":true}
 *
 * WebClient receives these as a Flux<JsonNode> — a reactive stream
 * where each element arrives as Ollama generates it. We extract the
 * "content" field from each JSON object and pass it along.
 *
 * This is the same pattern you'd use with OpenAI or Anthropic's
 * streaming APIs — only the JSON shape differs slightly.
 */
@Component
public class OllamaChatClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaChatClient.class);

    private final WebClient ollamaWebClient;
    private final String chatModel;

    public OllamaChatClient(
            WebClient ollamaWebClient,
            @Value("${ollama.chat-model}") String chatModel) {
        this.ollamaWebClient = ollamaWebClient;
        this.chatModel = chatModel;
    }

    /**
     * Stream a chat completion from Ollama.
     *
     * @param systemPrompt instructions for the LLM (includes retrieved context)
     * @param userMessage  the user's question
     * @return a Flux that emits one String per token as Ollama generates them
     */
    public Flux<String> streamChat(String systemPrompt, String userMessage) {
        log.info("Starting streaming chat with model: {}", chatModel);

        Map<String, Object> requestBody = Map.of(
                "model", chatModel,
                "stream", true,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        return ollamaWebClient.post()
                .uri("/api/chat")
                .bodyValue(requestBody)
                .retrieve()
                // Ollama sends newline-delimited JSON — each line is one token
                .bodyToFlux(JsonNode.class)
                .filter(node -> node.has("message")
                        && node.get("message").has("content"))
                .map(node -> node.get("message").get("content").asText())
                .filter(token -> !token.isEmpty())
                .doOnComplete(() -> log.info("Streaming complete"))
                .doOnError(e -> log.error("Streaming error: {}", e.getMessage()));
    }
}
