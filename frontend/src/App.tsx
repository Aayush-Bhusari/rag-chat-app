import { useState, useEffect } from 'react';
import { Document } from './types';
import { listDocuments } from './api/client';
import { FileUpload } from './components/FileUpload';
import { ChatWindow } from './components/ChatWindow';

/**
 * Root application component.
 *
 * Layout:
 * ┌─────────────────────────────────────────┐
 * │  Header                                 │
 * ├──────────────┬──────────────────────────┤
 * │  Sidebar     │  Main area               │
 * │  (documents) │  (upload or chat)        │
 * │              │                          │
 * └──────────────┴──────────────────────────┘
 */
export default function App() {
  const [documents, setDocuments] = useState<Document[]>([]);
  const [selectedDoc, setSelectedDoc] = useState<Document | null>(null);

  // Load existing documents on mount
  useEffect(() => {
    listDocuments()
      .then(setDocuments)
      .catch(console.error);
  }, []);

  const handleDocumentUploaded = (doc: Document) => {
    setDocuments(prev => [...prev, doc]);
    setSelectedDoc(doc);
  };

  return (
    <div style={{
      height: '100vh',
      display: 'flex',
      flexDirection: 'column',
      fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
      backgroundColor: '#ffffff',
    }}>
      {/* Header */}
      <header style={{
        padding: '16px 24px',
        borderBottom: '1px solid #e5e7eb',
        display: 'flex',
        alignItems: 'center',
        gap: '12px',
      }}>
        <h1 style={{ margin: 0, fontSize: '20px', fontWeight: 700 }}>
          📚 RAG Chat
        </h1>
        <span style={{ color: '#6b7280', fontSize: '14px' }}>
          Upload a document and ask questions about it
        </span>
      </header>

      {/* Body */}
      <div style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>

        {/* Sidebar — document list */}
        <aside style={{
          width: '260px',
          borderRight: '1px solid #e5e7eb',
          padding: '16px',
          overflowY: 'auto',
          backgroundColor: '#f9fafb',
        }}>
          <h2 style={{ fontSize: '14px', fontWeight: 600, color: '#6b7280', marginTop: 0 }}>
            DOCUMENTS
          </h2>
          {documents.length === 0 && (
            <p style={{ color: '#9ca3af', fontSize: '13px' }}>
              No documents yet. Upload one to get started.
            </p>
          )}
          {documents.map(doc => (
            <div
              key={doc.id}
              onClick={() => setSelectedDoc(doc)}
              style={{
                padding: '10px 12px',
                marginBottom: '4px',
                borderRadius: '8px',
                cursor: 'pointer',
                backgroundColor: selectedDoc?.id === doc.id ? '#dbeafe' : 'transparent',
                fontSize: '14px',
                wordBreak: 'break-word',
              }}
            >
              <div style={{ fontWeight: 500 }}>{doc.filename}</div>
              <div style={{ fontSize: '12px', color: '#9ca3af' }}>
                {doc.totalChunks} chunks
              </div>
            </div>
          ))}
        </aside>

        {/* Main area */}
        <main style={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          padding: selectedDoc ? '0' : '24px',
        }}>
          {!selectedDoc ? (
            <div style={{ maxWidth: '600px', margin: '40px auto', width: '100%' }}>
              <FileUpload onDocumentUploaded={handleDocumentUploaded} />
            </div>
          ) : (
            <ChatWindow document={selectedDoc} />
          )}
        </main>
      </div>
    </div>
  );
}
