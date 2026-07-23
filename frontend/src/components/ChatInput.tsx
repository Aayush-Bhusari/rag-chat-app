import { useState } from 'react';

interface ChatInputProps {
  onSend: (question: string) => void;
  disabled: boolean;
}

/**
 * Chat input with send button.
 * Submits on Enter key or button click.
 */
export function ChatInput({ onSend, disabled }: ChatInputProps) {
  const [input, setInput] = useState('');

  const handleSubmit = () => {
    const trimmed = input.trim();
    if (!trimmed || disabled) return;
    onSend(trimmed);
    setInput('');
  };

  return (
    <div style={{
      display: 'flex',
      gap: '8px',
      padding: '16px',
      borderTop: '1px solid #e5e7eb',
      backgroundColor: '#ffffff',
    }}>
      <input
        type="text"
        value={input}
        onChange={(e) => setInput(e.target.value)}
        onKeyDown={(e) => e.key === 'Enter' && handleSubmit()}
        placeholder={disabled ? 'Waiting for response...' : 'Ask a question about the document...'}
        disabled={disabled}
        style={{
          flex: 1,
          padding: '12px 16px',
          borderRadius: '8px',
          border: '1px solid #d1d5db',
          fontSize: '15px',
          outline: 'none',
        }}
      />
      <button
        onClick={handleSubmit}
        disabled={disabled || !input.trim()}
        style={{
          padding: '12px 24px',
          borderRadius: '8px',
          border: 'none',
          backgroundColor: disabled ? '#9ca3af' : '#3b82f6',
          color: '#ffffff',
          fontSize: '15px',
          fontWeight: 600,
          cursor: disabled ? 'not-allowed' : 'pointer',
        }}
      >
        Send
      </button>
    </div>
  );
}
