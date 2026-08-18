import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { aiService } from '../services/aiService';
import { AiResponse } from '../types/ai';
import Card from '../components/Card';
import EmptyState from '../components/EmptyState';
import LoadingSpinner from '../components/LoadingSpinner';
import { Bot, Send, AlertTriangle, History, BookOpen, FileText, ExternalLink } from 'lucide-react';

export const AIPage: React.FC = () => {
  const navigate = useNavigate();
  const [history, setHistory] = useState<AiResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const [question, setQuestion] = useState('');
  const [isAsking, setIsAsking] = useState(false);

  useEffect(() => {
    fetchHistory();
  }, []);

  const fetchHistory = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await aiService.getHistory();
      if (res.ok && res.data) {
        setHistory(res.data);
      } else {
        setError(res.message || 'Failed to fetch AI history');
      }
    } catch (err: any) {
      setError(err?.message || 'Error fetching AI history');
    } finally {
      setLoading(false);
    }
  };

  const handleAsk = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!question.trim()) return;

    setIsAsking(true);
    setError(null);
    try {
      const res = await aiService.askQuestion({ question });
      if (res.ok && res.data) {
        setHistory((prev) => [res.data!, ...prev]);
        setQuestion('');
      } else {
        setError(res.message || 'AI request failed');
      }
    } catch (err: any) {
      setError(err?.message || 'Error communicating with AI service');
    } finally {
      setIsAsking(false);
    }
  };

  const handleSourceClick = (type: string, id: number) => {
    if (type === 'KNOWLEDGE') {
      navigate(`/knowledge/${id}`);
    } else {
      navigate('/documents');
    }
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Neural RAG AI Assistant</h1>
          <p className="page-subtitle">Retrieval-Augmented Generation over your NeuroWiki knowledge base and documents</p>
        </div>
      </div>

      {history.length > 0 && !history[0].serviceConfigured && (
        <div className="alert alert-warning margin-bottom-lg">
          <AlertTriangle size={18} />
          <span>
            AI service is not configured. (Set <code>AI_API_KEY</code> in <code>application.properties</code> or environment variables).
          </span>
        </div>
      )}

      {error && (
        <div className="alert alert-error margin-bottom-lg">
          <span>{error}</span>
        </div>
      )}

      <Card className="ai-chat-card margin-bottom-xl">
        <form onSubmit={handleAsk} className="ai-input-form">
          <div className="ai-input-wrapper">
            <input
              type="text"
              className="form-input ai-input"
              placeholder="Ask a question about your knowledge or documents..."
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              disabled={isAsking}
              required
            />
            <button
              type="submit"
              className="btn btn-primary ai-send-btn"
              disabled={isAsking || !question.trim()}
              style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
            >
              <Send size={16} /> {isAsking ? 'Searching & Thinking...' : 'Ask AI'}
            </button>
          </div>
        </form>
      </Card>

      <div className="history-section margin-top-xl">
        <div className="section-header">
          <div className="title-with-icon">
            <History size={20} className="section-icon" />
            <h2>RAG AI Conversation History</h2>
          </div>
        </div>

        {loading ? (
          <LoadingSpinner text="Retrieving conversation history..." />
        ) : history.length === 0 ? (
          <EmptyState
            icon={Bot}
            title="Ask your first RAG question."
            description="Type a query above to query your neural knowledge base. Grounded RAG answers and cited sources will be saved here."
          />
        ) : (
          <div className="chat-history-list" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            {history.map((item, index) => (
              <Card key={item.id || index} className="chat-bubble-card">
                <div className="user-query" style={{ display: 'flex', gap: '12px', marginBottom: '14px', alignItems: 'flex-start' }}>
                  <img src="/assets/images/user-avatar.jpg" alt="User" style={{ width: '32px', height: '32px', borderRadius: '50%', objectFit: 'cover' }} />
                  <div className="query-text" style={{ fontSize: '16px', fontWeight: 600, color: '#f8fafc', paddingTop: '4px' }}>{item.question}</div>
                </div>

                <div className="ai-response" style={{ display: 'flex', gap: '12px', alignItems: 'flex-start', background: '#0f172a', padding: '16px', borderRadius: '12px', border: '1px solid #1e293b' }}>
                  <img src="/assets/images/ai-avatar.jpg" alt="AI Assistant" style={{ width: '36px', height: '36px', borderRadius: '50%', objectFit: 'cover', border: '1px solid #6366f1' }} />
                  <div className="response-text" style={{ flex: 1 }}>
                    <div style={{ fontSize: '15px', lineHeight: '1.6', color: '#e2e8f0', whiteSpace: 'pre-wrap' }}>
                      {item.answer}
                    </div>

                    {item.sources && item.sources.length > 0 && (
                      <div className="sources-section" style={{ marginTop: '16px', paddingTop: '12px', borderTop: '1px solid #334155' }}>
                        <div style={{ fontSize: '12px', fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '8px' }}>
                          Retrieved RAG Sources
                        </div>
                        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                          {item.sources.map((src, sIdx) => {
                            const isDoc = src.type === 'PDF';
                            const Icon = isDoc ? FileText : BookOpen;
                            return (
                              <button
                                key={sIdx}
                                onClick={() => handleSourceClick(src.type, src.id)}
                                style={{
                                  display: 'inline-flex',
                                  alignItems: 'center',
                                  gap: '6px',
                                  background: isDoc ? '#1e293b' : '#312e81',
                                  color: isDoc ? '#93c5fd' : '#c7d2fe',
                                  border: `1px solid ${isDoc ? '#3b82f6' : '#6366f1'}`,
                                  borderRadius: '6px',
                                  padding: '4px 10px',
                                  fontSize: '13px',
                                  cursor: 'pointer',
                                  transition: 'all 0.2s'
                                }}
                                title={`Open ${src.type} source`}
                              >
                                <Icon size={14} />
                                <span>{src.title}</span>
                                <ExternalLink size={12} style={{ opacity: 0.7 }} />
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    )}

                    {!item.serviceConfigured && (
                      <div className="unconfigured-badge margin-top-xs" style={{ display: 'inline-block', marginTop: '10px', color: '#f59e0b', fontSize: '12px', fontWeight: 600 }}>
                        ⚠️ AI Service Not Configured
                      </div>
                    )}
                  </div>
                </div>

                <div className="chat-meta" style={{ fontSize: '12px', color: '#64748b', marginTop: '8px', textAlign: 'right' }}>
                  {new Date(item.timestamp).toLocaleString()}
                </div>
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default AIPage;
