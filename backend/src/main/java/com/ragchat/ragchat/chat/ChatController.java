package com.ragchat.ragchat.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * REST controller for chat operations.
 *
 * The key endpoint here is /stream — it returns Server-Sent Events (SSE).
 *
 * WHAT ARE SERVER-SENT EVENTS?
 * SSE is a browser-native protocol for the server to push data to the
 * client over a single HTTP connection. Unlike WebSockets (bidirectional),
 * SSE is one-way (server → client), which is exactly what we need:
 * the server streams LLM tokens to the frontend.
 *
 * The browser receives events like:
 *   data: The
 *   data:  refund
 *   data:  policy
 *   data:  states
 *   ...
 *
 * And the frontend appends each one to the message bubble.
 *
 * MediaType.TEXT_EVENT_STREAM_VALUE tells Spring to format the
 * response as SSE (not JSON). Each element of the Flux becomes
 * one SSE event.
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Stream a RAG-powered answer via SSE.
     *
     * POST /api/chat/stream?documentId=1
     * Body: { "question": "What is the refund policy?" }
     *
     * Returns: text/event-stream with one token per event.
     *
     * WHY ServerSentEvent<String> instead of just String?
     * Wrapping in ServerSentEvent gives us control over event
     * names, IDs, and retry intervals. The frontend can listen
     * for specific event types.
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(
            @RequestParam Long documentId,
            @RequestBody ChatRequest request) {

        log.info("Chat stream request - doc: {}, question: {}", documentId, request.question());

        return chatService.streamAnswer(documentId, request.question())
                .map(token -> ServerSentEvent.<String>builder()
                        .event("token")           // event name the frontend listens for
                        .data(token)               // the actual token text
                        .build())
                .concatWithValues(
                        // Send a final "done" event so the frontend knows streaming is complete
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("[DONE]")
                                .build()
                );
    }

    /**
     * Get chat history for a document.
     * GET /api/chat/history?documentId=1
     */
    @GetMapping("/history")
    public Flux<ChatMessageEntity> getChatHistory(@RequestParam Long documentId) {
        return chatService.getChatHistory(documentId);
    }

    /**
     * Request body record for chat messages.
     * Java records are perfect for DTOs — immutable, minimal boilerplate.
     */
    public record ChatRequest(String question) {}
}
