import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { documentService } from '../services/documentService';
import { KnowledgeDocument } from '../types/document';
import Card from '../components/Card';
import EmptyState from '../components/EmptyState';
import LoadingSpinner from '../components/LoadingSpinner';
import { FileText, Trash2, UploadCloud, Link as LinkIcon, FileCheck } from 'lucide-react';

export const DocumentsPage: React.FC = () => {
  const [documents, setDocuments] = useState<KnowledgeDocument[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDocuments();
  }, []);

  const fetchDocuments = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await documentService.getAll();
      if (res.ok && res.data) {
        setDocuments(res.data);
      } else {
        setError(res.message || 'Failed to fetch documents');
      }
    } catch (err: any) {
      setError(err?.message || 'Error fetching documents');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Are you sure you want to delete this document?')) return;
    try {
      const res = await documentService.delete(id);
      if (res.ok) {
        setDocuments((prev) => prev.filter((doc) => doc.id !== id));
      }
    } catch (err) {
      console.error('Error deleting document', err);
    }
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Document Repository</h1>
          <p className="page-subtitle">Ingested files, Web URLs, and processed documents</p>
        </div>
        <Link to="/ingest" className="btn btn-primary">
          <UploadCloud size={16} /> Ingest New Document
        </Link>
      </div>

      {error && (
        <div className="alert alert-error margin-bottom-lg">
          <span>{error}</span>
        </div>
      )}

      {loading ? (
        <LoadingSpinner text="Fetching document index..." />
      ) : documents.length === 0 ? (
        <EmptyState
          icon={FileText}
          title="No documents yet."
          description="Upload a PDF file or ingest content from a Web URL to extract neural concepts."
          actionText="Ingest Document"
          onAction={() => window.location.href = '/ingest'}
        />
      ) : (
        <Card className="table-card">
          <div className="table-wrapper">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Source Type</th>
                  <th>Extracted Concepts</th>
                  <th>Size</th>
                  <th>Added Date</th>
                  <th>Status</th>
                  <th className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {documents.map((doc) => (
                  <tr key={doc.id}>
                    <td className="doc-title-cell">
                      <div className="title-wrapper">
                        {doc.type === 'URL' ? (
                          <LinkIcon size={16} className="type-icon url-icon" />
                        ) : (
                          <FileText size={16} className="type-icon pdf-icon" />
                        )}
                        <span className="doc-name">{doc.title}</span>
                      </div>
                    </td>
                    <td>
                      <span className={`type-badge badge-${doc.type.toLowerCase()}`}>
                        {doc.type}
                      </span>
                    </td>
                    <td>
                      <span className="concept-badge">
                        <FileCheck size={12} /> {doc.conceptsExtractedCount} terms
                      </span>
                    </td>
                    <td className="text-muted">{doc.fileSize}</td>
                    <td className="text-muted">
                      {new Date(doc.addedAt).toLocaleDateString()}
                    </td>
                    <td>
                      <span className="status-pill status-processed">{doc.status}</span>
                    </td>
                    <td className="text-right">
                      <button
                        onClick={() => handleDelete(doc.id)}
                        className="icon-btn delete-btn"
                        title="Delete Document"
                      >
                        <Trash2 size={16} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </div>
  );
};

export default DocumentsPage;
