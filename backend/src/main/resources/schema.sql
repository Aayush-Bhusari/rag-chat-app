-- ============================================================
-- Database Schema for RAG Chat App
-- ============================================================
-- This runs automatically on startup (spring.sql.init.mode=always).
-- CREATE TABLE IF NOT EXISTS = safe to run repeatedly.
-- ============================================================

-- Enable the pgvector extension (must be done once per database)
CREATE EXTENSION IF NOT EXISTS vector;

-- --- Documents table ---
-- Stores metadata about each uploaded file.
CREATE TABLE IF NOT EXISTS documents (
    id              BIGSERIAL PRIMARY KEY,
    filename        VARCHAR(500) NOT NULL,
    content_type    VARCHAR(100),           -- "application/pdf" or "text/plain"
    uploaded_at     TIMESTAMP DEFAULT NOW(),
    total_chunks    INTEGER DEFAULT 0       -- how many chunks were created
);

-- --- Chunks table ---
-- Each document is split into overlapping text chunks.
-- Each chunk has an embedding vector (768 dimensions for nomic-embed-text).
-- The embedding column uses pgvector's VECTOR type.
CREATE TABLE IF NOT EXISTS chunks (
    id              BIGSERIAL PRIMARY KEY,
    document_id     BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index     INTEGER NOT NULL,       -- position within the document (0, 1, 2...)
    content         TEXT NOT NULL,           -- the actual text of this chunk
    embedding       vector(768),            -- 768-dim vector from nomic-embed-text
    created_at      TIMESTAMP DEFAULT NOW()
);

-- Index for fast vector similarity search using cosine distance.
-- IVFFlat = "Inverted File with Flat compression" — a standard
-- approximate nearest-neighbor index. lists=100 is fine for <100k rows.
-- For a portfolio project this is overkill, but shows you know about indexing.
CREATE INDEX IF NOT EXISTS chunks_embedding_idx
    ON chunks USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- --- Chat Messages table ---
-- Stores the conversation history so it persists across page refreshes.
CREATE TABLE IF NOT EXISTS chat_messages (
    id              BIGSERIAL PRIMARY KEY,
    document_id     BIGINT REFERENCES documents(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL,   -- 'user' or 'assistant'
    content         TEXT NOT NULL,
    created_at      TIMESTAMP DEFAULT NOW()
);
