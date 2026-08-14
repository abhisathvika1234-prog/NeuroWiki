import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { dashboardService } from '../services/dashboardService';
import { DashboardStats } from '../types/dashboard';
import Card from '../components/Card';
import EmptyState from '../components/EmptyState';
import LoadingSpinner from '../components/LoadingSpinner';
import { 
  BookOpen, 
  FileText, 
  Star, 
  Bot, 
  Plus, 
  UploadCloud, 
  MessageSquare, 
  Clock,
  ArrowRight
} from 'lucide-react';

export const Dashboard: React.FC = () => {
  const { user } = useAuth();
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchStats();
  }, []);

  const fetchStats = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await dashboardService.getStats();
      if (res.ok && res.data) {
        setStats(res.data);
      } else {
        setError(res.message || 'Failed to load dashboard data');
      }
    } catch (err: any) {
      setError(err?.message || 'Error fetching dashboard stats');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <LoadingSpinner text="Loading your neural statistics..." />;
  }

  return (
    <div className="page-container">
      <div className="dashboard-header-banner">
        <div className="banner-content">
          <h1 className="welcome-title">Welcome back, {user?.username}! 👋</h1>
          <p className="welcome-subtitle">
            Your personal neural knowledge base is connected to PostgreSQL.
          </p>
        </div>
        <div className="banner-actions">
          <Link to="/knowledge" className="btn btn-primary">
            <Plus size={16} /> New Knowledge
          </Link>
          <Link to="/ingest" className="btn btn-secondary">
            <UploadCloud size={16} /> Ingest Data
          </Link>
        </div>
      </div>

      {error && (
        <div className="alert alert-error margin-bottom-lg">
          <span>{error}</span>
        </div>
      )}

      <div className="stats-grid">
        <Card className="stat-card stat-purple">
          <div className="stat-icon-wrapper">
            <BookOpen size={24} />
          </div>
          <div className="stat-info">
            <span className="stat-label">Knowledge Pages</span>
            <span className="stat-value">{stats?.knowledgeCount ?? 0}</span>
          </div>
        </Card>

        <Card className="stat-card stat-blue">
          <div className="stat-icon-wrapper">
            <FileText size={24} />
          </div>
          <div className="stat-info">
            <span className="stat-label">Documents</span>
            <span className="stat-value">{stats?.documentCount ?? 0}</span>
          </div>
        </Card>

        <Card className="stat-card stat-amber">
          <div className="stat-icon-wrapper">
            <Star size={24} />
          </div>
          <div className="stat-info">
            <span className="stat-label">Favorites</span>
            <span className="stat-value">{stats?.favoritesCount ?? 0}</span>
          </div>
        </Card>

        <Card className="stat-card stat-emerald">
          <div className="stat-icon-wrapper">
            <Bot size={24} />
          </div>
          <div className="stat-info">
            <span className="stat-label">AI Questions</span>
            <span className="stat-value">{stats?.aiQuestionsCount ?? 0}</span>
          </div>
        </Card>
      </div>

      <div className="dashboard-content-grid">
        <div className="section-column">
          <div className="section-header">
            <div className="title-with-icon">
              <Clock size={20} className="section-icon" />
              <h2>Recent Knowledge Pages</h2>
            </div>
            <Link to="/knowledge" className="view-all-link">
              View All <ArrowRight size={14} />
            </Link>
          </div>

          {!stats?.recentKnowledge || stats.recentKnowledge.length === 0 ? (
            <EmptyState
              icon={BookOpen}
              title="No knowledge yet."
              description="Create your first knowledge page to start populating your neural graph."
            />
          ) : (
            <div className="recent-list">
              {stats.recentKnowledge.map((item) => (
                <Link key={item.id} to={`/knowledge/${item.id}`} className="recent-item-card">
                  <div className="item-header">
                    <h4>{item.title}</h4>
                    {item.favorite && <Star size={14} className="star-icon filled" />}
                  </div>
                  <p className="item-excerpt">
                    {item.content.length > 100 ? item.content.slice(0, 100) + '...' : item.content}
                  </p>
                  <div className="item-footer">
                    <span className="tag-pill">{item.category || 'General'}</span>
                    <span className="date-text">
                      {new Date(item.updatedAt).toLocaleDateString()}
                    </span>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </div>

        <div className="section-column">
          <div className="section-header">
            <div className="title-with-icon">
              <FileText size={20} className="section-icon" />
              <h2>Recent Documents</h2>
            </div>
            <Link to="/documents" className="view-all-link">
              View All <ArrowRight size={14} />
            </Link>
          </div>

          {!stats?.recentDocuments || stats.recentDocuments.length === 0 ? (
            <EmptyState
              icon={FileText}
              title="No documents yet."
              description="Ingest web URLs or upload PDF files to extract structured concepts."
            />
          ) : (
            <div className="recent-list">
              {stats.recentDocuments.map((doc) => (
                <div key={doc.id} className="recent-item-card">
                  <div className="item-header">
                    <h4>{doc.title}</h4>
                    <span className={`type-badge badge-${doc.type.toLowerCase()}`}>
                      {doc.type}
                    </span>
                  </div>
                  <div className="item-meta">
                    <span>Concepts: {doc.conceptsExtractedCount}</span>
                    <span>Size: {doc.fileSize}</span>
                  </div>
                  <div className="item-footer">
                    <span className="status-indicator active">{doc.status}</span>
                    <span className="date-text">
                      {new Date(doc.addedAt).toLocaleDateString()}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
