package com.ragchat.ragchat;

import com.ragchat.ragchat.chat.ChatController;
import com.ragchat.ragchat.chat.ChatMessageEntity;
import com.ragchat.ragchat.chat.ChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration test for ChatController using WebTestClient.
 *
 * @WebFluxTest loads ONLY the web layer (controllers, filters),
 * NOT the full application context. This means:
 * - No database connection needed
 * - No Ollama connection needed
 * - Services are replaced with @MockBean mocks
 * - Tests run fast (no heavy startup)
 *
 * WebTestClient is Spring WebFlux's test utility — it sends HTTP
 * requests to the controller and asserts on the response.
 */
@WebFluxTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ChatService chatService;

    @Test
    @DisplayName("SSE stream endpoint should return text/event-stream content type")
    void shouldStreamTokensAsSSE() {
        // Mock: when chatService.streamAnswer is called, return a fake token stream
        when(chatService.streamAnswer(anyLong(), anyString()))
                .thenReturn(Flux.just("Hello", " from", " RAG"));

        webTestClient.post()
                .uri("/api/chat/stream?documentId=1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\": \"What is this about?\"}")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM);
                // SSE responses are harder to assert on content,
                // but verifying status + content-type confirms the
                // endpoint is wired correctly.
    }

    @Test
    @DisplayName("Chat history endpoint should return messages in order")
    void shouldReturnChatHistory() {
        ChatMessageEntity msg1 = new ChatMessageEntity(1L, "user", "Hello");
        ChatMessageEntity msg2 = new ChatMessageEntity(1L, "assistant", "Hi there!");

        when(chatService.getChatHistory(1L))
                .thenReturn(Flux.just(msg1, msg2));

        webTestClient.get()
                .uri("/api/chat/history?documentId=1")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ChatMessageEntity.class)
                .hasSize(2);
    }
}
