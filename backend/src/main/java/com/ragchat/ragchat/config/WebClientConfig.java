package com.ragchat.ragchat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Creates a WebClient bean configured to talk to the Ollama API.
 *
 * WHY WebClient instead of RestTemplate?
 * RestTemplate is blocking — it ties up a thread while waiting for
 * Ollama to respond (which can take seconds for LLM generation).
 * WebClient is non-blocking and supports streaming responses,
 * which is exactly what we need for token-by-token SSE streaming.
 */
@Configuration
public class WebClientConfig {

    @Value("${ollama.base-url}")
    private String ollamaBaseUrl;

    @Bean
    public WebClient ollamaWebClient() {
        // Ollama can be slow (especially on CPU), so generous timeouts
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMinutes(5));

        return WebClient.builder()
                .baseUrl(ollamaBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(20 * 1024 * 1024)) // 20MB buffer
                .build();
    }
}
