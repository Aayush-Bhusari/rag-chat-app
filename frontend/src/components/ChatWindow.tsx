import { useEffect, useRef } from 'react';
import { Document } from '../types';
import { useChatStream } from '../hooks/useChatStream';
import { MessageBubble } from './MessageBubble';
import { ChatInput } from './ChatInput';

interface ChatWindowProps {
  document: Document;
}

/**
 * Main chat window component.
 *
 * Loads existing chat history when a document is selected,
 * then allows the user to send new questions and see
 * streaming responses.
 *
 * The useEffect with loadHistory runs whenever the document
 * changes (user picks a different document).
 *
 * The scrollRef + second useEffect auto-scrolls to the bottom
 * whenever new messages arrive (so you always see the latest token).
 */
export function ChatWindow({ document }: ChatWindowProps) {
  const { messages, isLoading, error, sendMessage, loadHistory } = useChatStream(document.id);
  const scrollRef = useRef<HTMLDivElement>(null);

  // Load chat history when document changes
  useEffect(() => {
    loadHistory();
  }, [loadHistory]);

  // Auto-scroll to bottom on new messages
  useEffect(() => {
    scrollRef.current?.scrollTo({
      top: scrollRef.current.scrollHeight,
      behavior: 'smooth',
    });
  }, [messages]);

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      height: '100%',
      borderRadius: '12px',
      border: '1px solid #e5e7eb',
      overflow: 'hidden',
    }}>
      {/* Header */}
      <div style={{
        padding: '12px 16px',
        borderBottom: '1px solid #e5e7eb',
        backgroundColor: '#f9fafb',
      }}>
        <strong>{document.filename}</strong>
        <span style={{ color: '#6b7280', marginLeft: '8px', fontSize: '13px' }}>
          {document.totalChunks} chunks indexed
        </span>
      </div>

      {/* Messages area */}
      <div
        ref={scrollRef}
        style={{
          flex: 1,
          overflowY: 'auto',
          padding: '16px',
        }}
      >
        {messages.length === 0 && (
          <p style={{ color: '#9ca3af', textAlign: 'center', marginTop: '40px' }}>
            Ask a question about this document to get started.
          </p>
        )}
        {messages.map((msg, idx) => (
          <MessageBubble key={idx} message={msg} />
        ))}
      </div>

      {/* Error display */}
      {error && (
        <div style={{
          padding: '8px 16px',
          backgroundColor: '#fef2f2',
          color: '#dc2626',
          fontSize: '14px',
        }}>
          Error: {error}
        </div>
      )}

      {/* Input */}
      <ChatInput onSend={sendMessage} disabled={isLoading} />
    </div>
  );
}
