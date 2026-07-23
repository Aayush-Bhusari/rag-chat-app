import { useState, useCallback } from 'react';
import { ChatMessage } from '../types';
import { streamChat, getChatHistory } from '../api/client';

/**
 * Custom React hook for managing chat state and SSE streaming.
 *
 * WHAT IS A CUSTOM HOOK?
 * A reusable piece of stateful logic. Instead of putting all the
 * chat state (messages, loading, error) and streaming logic inside
 * a component, we extract it here so:
 * 1. The component stays clean (just UI)
 * 2. This logic could be reused in a different component
 * 3. It's easier to test
 *
 * Returns an object with state + actions:
 *   messages  — the current list of chat messages
 *   isLoading — whether we're currently streaming a response
 *   error     — any error message
 *   sendMessage() — sends a question and handles streaming
 *   loadHistory() — loads existing chat history from the DB
 */
export function useChatStream(documentId: number | null) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /**
   * Load existing chat history from the backend.
   * Called when the user selects a document.
   */
  const loadHistory = useCallback(async () => {
    if (!documentId) return;
    try {
      const history = await getChatHistory(documentId);
      setMessages(history);
    } catch (err) {
      console.error('Failed to load history:', err);
    }
  }, [documentId]);

  /**
   * Send a question and stream the response.
   *
   * Here's what happens step by step:
   * 1. Add the user's message to the messages array immediately
   * 2. Add an empty assistant message (placeholder)
   * 3. Start streaming — each token updates the assistant message
   * 4. When done, the assistant message contains the full response
   *
   * The key trick: we update the LAST message in the array with
   * each new token, which makes the text appear to "type" in real time.
   */
  const sendMessage = useCallback(async (question: string) => {
    if (!documentId) return;

    setError(null);
    setIsLoading(true);

    // Add user message to the UI immediately
    const userMessage: ChatMessage = {
      documentId,
      role: 'user',
      content: question,
    };

    // Add empty assistant message that we'll fill with streaming tokens
    const assistantMessage: ChatMessage = {
      documentId,
      role: 'assistant',
      content: '',
    };

    setMessages(prev => [...prev, userMessage, assistantMessage]);

    await streamChat(
      documentId,
      question,
      // onToken: append each token to the assistant's message
      (token: string) => {
        setMessages(prev => {
          const updated = [...prev];
          const lastMsg = updated[updated.length - 1];
          if (lastMsg.role === 'assistant') {
            updated[updated.length - 1] = {
              ...lastMsg,
              content: lastMsg.content + token,
            };
          }
          return updated;
        });
      },
      // onDone
      () => setIsLoading(false),
      // onError
      (err: Error) => {
        setError(err.message);
        setIsLoading(false);
      },
    );
  }, [documentId]);

  return { messages, isLoading, error, sendMessage, loadHistory, setMessages };
}
