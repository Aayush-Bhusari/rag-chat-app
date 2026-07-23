/**
 * TypeScript interfaces — define the shape of data objects.
 *
 * WHY INTERFACES?
 * TypeScript catches bugs at compile time by checking that your
 * data has the right fields and types. If the API returns
 * { id: 1, filename: "doc.pdf" } but you try to access
 * response.name, TypeScript will flag it before you even run the code.
 */

/** A document returned from the backend */
export interface Document {
  id: number;
  filename: string;
  contentType: string;
  uploadedAt: string;
  totalChunks: number;
}

/** A chat message (either from the user or the assistant) */
export interface ChatMessage {
  id?: number;
  documentId: number;
  role: 'user' | 'assistant';
  content: string;
  createdAt?: string;
}
