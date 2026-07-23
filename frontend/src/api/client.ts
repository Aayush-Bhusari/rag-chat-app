import { Document, ChatMessage } from '../types';

const BASE_URL = '/api';

/**
 * Upload a document (PDF or text file) to the backend.
 *
 * FormData is the browser-native way to send files — it builds
 * a multipart/form-data request body that the server expects.
 */
export async function uploadDocument(file: File): Promise<Document> {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(`${BASE_URL}/documents/upload`, {
    method: 'POST',
    body: formData,
    // Don't set Content-Type header — the browser sets it
    // automatically with the correct multipart boundary.
  });

  if (!response.ok) {
    throw new Error(`Upload failed: ${response.statusText}`);
  }

  return response.json();
}

/** Fetch all uploaded documents */
export async function listDocuments(): Promise<Document[]> {
  const response = await fetch(`${BASE_URL}/documents`);
  return response.json();
}

/** Fetch chat history for a specific document */
export async function getChatHistory(documentId: number): Promise<ChatMessage[]> {
  const response = await fetch(`${BASE_URL}/chat/history?documentId=${documentId}`);
  return response.json();
}

/**
 * Stream a chat response via SSE (Server-Sent Events).
 *
 * HOW THIS WORKS:
 * 1. We POST the question to /api/chat/stream
 * 2. The server keeps the connection open and sends tokens as SSE events
 * 3. We use fetch() + ReadableStream (not EventSource) because
 *    EventSource only supports GET requests, and we need POST
 *    to send the question in the request body
 * 4. Each SSE event looks like:
 *      event: token
 *      data: Hello
 * 5. We parse each line, extract the data, and call onToken()
 *
 * @param documentId which document to query against
 * @param question   the user's question
 * @param onToken    callback fired for each token as it arrives
 * @param onDone     callback fired when streaming is complete
 * @param onError    callback fired on error
 */
export async function streamChat(
  documentId: number,
  question: string,
  onToken: (token: string) => void,
  onDone: () => void,
  onError: (error: Error) => void,
): Promise<void> {
  try {
    const response = await fetch(
      `${BASE_URL}/chat/stream?documentId=${documentId}`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ question }),
      }
    );

    if (!response.ok) {
      throw new Error(`Chat failed: ${response.statusText}`);
    }

    // ReadableStream lets us read the response incrementally
    // as the server sends it, rather than waiting for the full response
    const reader = response.body?.getReader();
    if (!reader) throw new Error('No readable stream');

    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      // Decode the bytes to text and add to buffer
      buffer += decoder.decode(value, { stream: true });

      // SSE format: each event is separated by double newlines
      // Each event has lines like "event: token\ndata: Hello\n\n"
      const lines = buffer.split('\n');
      buffer = lines.pop() || ''; // Keep incomplete last line in buffer

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.slice(5); // Remove "data:" prefix
          if (data === '[DONE]') {
            onDone();
            return;
          }
          onToken(data);
        }
      }
    }

    onDone();
  } catch (error) {
    onError(error instanceof Error ? error : new Error(String(error)));
  }
}
