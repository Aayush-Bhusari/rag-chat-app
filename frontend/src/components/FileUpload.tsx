import { useState, useRef } from 'react';
import { Document } from '../types';
import { uploadDocument } from '../api/client';

interface FileUploadProps {
  onDocumentUploaded: (doc: Document) => void;
}

/**
 * File upload component with drag-and-drop support.
 *
 * useRef gives us a reference to the hidden <input type="file">
 * element so we can trigger it programmatically when the user
 * clicks the styled upload area.
 */
export function FileUpload({ onDocumentUploaded }: FileUploadProps) {
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [dragActive, setDragActive] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const handleFile = async (file: File) => {
    // Validate file type
    const allowedTypes = ['application/pdf', 'text/plain'];
    if (!allowedTypes.includes(file.type) && !file.name.endsWith('.txt')) {
      setError('Only PDF and text files are supported');
      return;
    }

    setError(null);
    setIsUploading(true);

    try {
      const doc = await uploadDocument(file);
      onDocumentUploaded(doc);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Upload failed');
    } finally {
      setIsUploading(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragActive(false);
    if (e.dataTransfer.files[0]) {
      handleFile(e.dataTransfer.files[0]);
    }
  };

  return (
    <div
      onDragOver={(e) => { e.preventDefault(); setDragActive(true); }}
      onDragLeave={() => setDragActive(false)}
      onDrop={handleDrop}
      onClick={() => inputRef.current?.click()}
      style={{
        border: `2px dashed ${dragActive ? '#3b82f6' : '#d1d5db'}`,
        borderRadius: '12px',
        padding: '32px',
        textAlign: 'center',
        cursor: 'pointer',
        backgroundColor: dragActive ? '#eff6ff' : '#f9fafb',
        transition: 'all 0.2s',
      }}
    >
      <input
        ref={inputRef}
        type="file"
        accept=".pdf,.txt,text/plain,application/pdf"
        style={{ display: 'none' }}
        onChange={(e) => e.target.files?.[0] && handleFile(e.target.files[0])}
      />

      {isUploading ? (
        <p style={{ color: '#6b7280' }}>Uploading and processing... (this may take a minute)</p>
      ) : (
        <>
          <p style={{ fontSize: '18px', margin: '0 0 8px' }}>
            📄 Drop a PDF or text file here, or click to browse
          </p>
          <p style={{ color: '#9ca3af', fontSize: '14px', margin: 0 }}>
            The file will be chunked, embedded, and ready for questions
          </p>
        </>
      )}

      {error && (
        <p style={{ color: '#ef4444', marginTop: '12px' }}>{error}</p>
      )}
    </div>
  );
}
