import { ChatMessage } from '../types';

interface MessageBubbleProps {
  message: ChatMessage;
}

/**
 * Displays a single chat message with different styling
 * based on whether it's from the user or the assistant.
 */
export function MessageBubble({ message }: MessageBubbleProps) {
  const isUser = message.role === 'user';

  return (
    <div
      style={{
        display: 'flex',
        justifyContent: isUser ? 'flex-end' : 'flex-start',
        marginBottom: '12px',
      }}
    >
      <div
        style={{
          maxWidth: '75%',
          padding: '12px 16px',
          borderRadius: '16px',
          backgroundColor: isUser ? '#3b82f6' : '#f3f4f6',
          color: isUser ? '#ffffff' : '#1f2937',
          fontSize: '15px',
          lineHeight: '1.5',
          whiteSpace: 'pre-wrap',       // Preserve line breaks from the LLM
          wordBreak: 'break-word',
        }}
      >
        <div style={{
          fontSize: '11px',
          fontWeight: 600,
          marginBottom: '4px',
          opacity: 0.7,
        }}>
          {isUser ? 'You' : 'Assistant'}
        </div>
        {message.content || (
          <span style={{ opacity: 0.5 }}>Thinking...</span>
        )}
      </div>
    </div>
  );
}
