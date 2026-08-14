import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ingestionService } from '../services/ingestionService';
import { documentService } from '../services/documentService';
import Card from '../components/Card';
import { UploadCloud, Link as LinkIcon, FileText, CheckCircle2, AlertCircle, FileUp } from 'lucide-react';

export const IngestionPage: React.FC = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<'text' | 'url' | 'pdf'>('text');

  // Text/URL Ingestion State
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [url, setUrl] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // PDF Upload State
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);

  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const handleIngest = async (e: React.FormEvent) => {
    e.preventDefault();
    setMessage(null);

    if (!title.trim()) {
      setMessage({ type: 'error', text: 'Document title is required' });
      return;
    }

    if (activeTab === 'url' && !url.trim()) {
      setMessage({ type: 'error', text: 'Web URL is required' });
      return;
    }

    if (activeTab === 'text' && !content.trim()) {
      setMessage({ type: 'error', text: 'Text content is required' });
      return;
    }

    setIsSubmitting(true);
    try {
      const res = await ingestionService.ingest({
        title,
        content: activeTab === 'text' ? content : undefined,
        url: activeTab === 'url' ? url : undefined,
        sourceType: activeTab === 'url' ? 'URL' : 'TEXT',
      });

      if (res.ok && res.data) {
        setMessage({
          type: 'success',
          text: `Document ingested successfully! Extracted ${res.data.conceptsExtractedCount} concepts.`,
        });
        setTitle('');
        setContent('');
        setUrl('');
        setTimeout(() => navigate('/documents'), 1500);
      } else {
        setMessage({ type: 'error', text: res.message || 'Ingestion failed' });
      }
    } catch (err: any) {
      setMessage({ type: 'error', text: err?.message || 'Ingestion error' });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handlePdfUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    setMessage(null);

    if (!selectedFile) {
      setMessage({ type: 'error', text: 'Please select a PDF file to upload' });
      return;
    }

    setIsUploading(true);
    try {
      const res = await documentService.uploadPdf(selectedFile);
      if (res.ok && res.data) {
        setMessage({
          type: 'success',
          text: `PDF '${res.data.title}' uploaded and parsed! Extracted ${res.data.conceptsExtractedCount} terms.`,
        });
        setSelectedFile(null);
        setTimeout(() => navigate('/documents'), 1500);
      } else {
        setMessage({ type: 'error', text: res.message || 'PDF processing failed' });
      }
    } catch (err: any) {
      setMessage({ type: 'error', text: err?.message || 'PDF upload error' });
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Data Ingestion Engine</h1>
          <p className="page-subtitle">Ingest Web URLs, Raw Text, or parse PDF documents into PostgreSQL</p>
        </div>
      </div>

      {message && (
        <div className={`alert alert-${message.type} margin-bottom-lg`}>
          {message.type === 'success' ? <CheckCircle2 size={18} /> : <AlertCircle size={18} />}
          <span>{message.text}</span>
        </div>
      )}

      <div className="tab-navigation margin-bottom-lg">
        <button
          onClick={() => setActiveTab('text')}
          className={`tab-button ${activeTab === 'text' ? 'active' : ''}`}
        >
          <FileText size={18} /> Raw Text
        </button>
        <button
          onClick={() => setActiveTab('url')}
          className={`tab-button ${activeTab === 'url' ? 'active' : ''}`}
        >
          <LinkIcon size={18} /> Web URL
        </button>
        <button
          onClick={() => setActiveTab('pdf')}
          className={`tab-button ${activeTab === 'pdf' ? 'active' : ''}`}
        >
          <FileUp size={18} /> PDF File Upload
        </button>
      </div>

      {activeTab === 'pdf' ? (
        <Card className="ingestion-card">
          <form onSubmit={handlePdfUpload}>
            <div className="dropzone-area">
              <UploadCloud size={48} className="dropzone-icon" />
              <h3>Select PDF Document</h3>
              <p className="dropzone-text">Upload a .pdf file (up to 10MB) for server-side text parsing</p>

              <input
                type="file"
                accept=".pdf"
                id="pdf-file-input"
                className="hidden-file-input"
                onChange={(e) => {
                  if (e.target.files && e.target.files[0]) {
                    setSelectedFile(e.target.files[0]);
                  }
                }}
              />
              <label htmlFor="pdf-file-input" className="btn btn-secondary margin-top-md">
                Browse PDF File
              </label>

              {selectedFile && (
                <div className="selected-file-info">
                  <FileText size={16} />
                  <span>{selectedFile.name} ({(selectedFile.size / 1024).toFixed(1)} KB)</span>
                </div>
              )}
            </div>

            <div className="form-actions margin-top-lg">
              <button
                type="submit"
                className="btn btn-primary btn-full"
                disabled={!selectedFile || isUploading}
              >
                {isUploading ? 'Parsing & Uploading PDF...' : 'Upload & Parse PDF'}
              </button>
            </div>
          </form>
        </Card>
      ) : (
        <Card className="ingestion-card">
          <form onSubmit={handleIngest}>
            <div className="form-group">
              <label className="form-label">Document Title</label>
              <input
                type="text"
                className="form-input"
                placeholder="e.g. Distributed Consensus Whitepaper"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                required
              />
            </div>

            {activeTab === 'url' ? (
              <div className="form-group">
                <label className="form-label">Web Resource URL</label>
                <input
                  type="url"
                  className="form-input"
                  placeholder="https://example.com/article"
                  value={url}
                  onChange={(e) => setUrl(e.target.value)}
                  required
                />
              </div>
            ) : (
              <div className="form-group">
                <label className="form-label">Content Body</label>
                <textarea
                  className="form-textarea"
                  rows={8}
                  placeholder="Paste or write text content to ingest..."
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  required
                />
              </div>
            )}

            <div className="form-actions margin-top-lg">
              <button
                type="submit"
                className="btn btn-primary btn-full"
                disabled={isSubmitting}
              >
                {isSubmitting ? 'Ingesting Data...' : 'Ingest Document'}
              </button>
            </div>
          </form>
        </Card>
      )}
    </div>
  );
};

export default IngestionPage;
