package com.ragchat.ragchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the RAG Chat application.
 *
 * @SpringBootApplication does three things at once:
 * 1. @Configuration   — this class can define beans
 * 2. @ComponentScan   — auto-discovers @Service, @Repository, @Controller in this package
 * 3. @EnableAutoConfiguration — Spring Boot auto-configures based on dependencies in pom.xml
 *    (e.g., it sees R2DBC on the classpath and auto-creates a database connection pool)
 */
@SpringBootApplication
public class RagChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(RagChatApplication.class, args);
    }
}
