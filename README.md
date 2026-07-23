\# RAG Chat App



An AI-powered document Q\&A application using Retrieval-Augmented Generation (RAG).

Upload a PDF or text file and ask questions about it. Answers stream back token by token.



\## Tech Stack

\- Backend: Java 21, Spring Boot 3, Spring WebFlux (reactive/non-blocking)

\- Database: PostgreSQL + pgvector (vector similarity search)

\- AI: Ollama (local LLM) - llama3.2:1b for chat, nomic-embed-text for embeddings

\- Frontend: React + TypeScript, Vite

\- Streaming: Server-Sent Events (SSE)

\- CI: GitHub Actions

\- Containerization: Docker Compose



\## Architecture

1\. Upload a document -> text extracted -> split into chunks -> embedded via Ollama

2\. Ask a question -> question embedded -> top-k similar chunks retrieved via pgvector

3\. Chunks + question sent to LLM -> response streamed token by token via SSE



\## Run Locally

```bash

docker compose up -d

docker exec ragchat-ollama ollama pull nomic-embed-text

docker exec ragchat-ollama ollama pull llama3.2:1b

cd backend \&\& mvn spring-boot:run

cd frontend \&\& npm install \&\& npm run dev

```

Open http://localhost:5173

