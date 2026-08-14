import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { knowledgeService } from '../services/knowledgeService';
import { KnowledgePage as IKnowledgePage } from '../types/knowledge';
import LoadingSpinner from '../components/LoadingSpinner';
import Card from '../components/Card';
import { ArrowLeft, Save, Star, Trash2, Tag, Calendar } from 'lucide-react';

export const KnowledgeDetailsPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [page, setPage] = useState<IKnowledgePage | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [isEditing, setIsEditing] = useState<boolean>(false);

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState('');
  const [tags, setTags] = useState('');
  const [favorite, setFavorite] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    if (id) {
      fetchPage(parseInt(id, 10));
    }
  }, [id]);

  const fetchPage = async (pageId: number) => {
    setLoading(true);
    setError(null);
    try {
      const res = await knowledgeService.getById(pageId);
      if (res.ok && res.data) {
        setPage(res.data);
        setTitle(res.data.title);
        setContent(res.data.content);
        setCategory(res.data.category || 'General');
        setTags(res.data.tags || '');
        setFavorite(res.data.favorite);
      } else {
        setError(res.message || 'Knowledge page not found');
      }
    } catch (err: any) {
      setError(err?.message || 'Error loading page details');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !title.trim() || !content.trim()) return;

    setIsSaving(true);
    try {
      const res = await knowledgeService.update(parseInt(id, 10), {
        title,
        content,
        category,
        tags,
        favorite,
      });

      if (res.ok && res.data) {
        setPage(res.data);
        setIsEditing(false);
      } else {
        setError(res.message || 'Failed to update page');
      }
    } catch (err: any) {
      setError(err?.message || 'Error updating page');
    } finally {
      setIsSaving(false);
    }
  };

  const handleToggleFavorite = async () => {
    if (!page) return;
    try {
      const res = await knowledgeService.toggleFavorite(page.id);
      if (res.ok && res.data) {
        setPage(res.data);
        setFavorite(res.data.favorite);
      }
    } catch (err) {
      console.error('Error toggling favorite', err);
    }
  };

  const handleDelete = async () => {
    if (!page || !window.confirm('Are you sure you want to delete this page?')) return;
    try {
      const res = await knowledgeService.delete(page.id);
      if (res.ok) {
        navigate('/knowledge');
      }
    } catch (err) {
      console.error('Error deleting page', err);
    }
  };

  if (loading) {
    return <LoadingSpinner text="Loading knowledge details..." />;
  }

  if (error || !page) {
    return (
      <div className="page-container">
        <Link to="/knowledge" className="back-link">
          <ArrowLeft size={16} /> Back to Knowledge Base
        </Link>
        <div className="alert alert-error margin-top-md">
          <span>{error || 'Page not found'}</span>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <Link to="/knowledge" className="back-link">
          <ArrowLeft size={16} /> Back to Knowledge Base
        </Link>

        <div className="header-actions">
          <button
            onClick={handleToggleFavorite}
            className={`btn btn-secondary ${favorite ? 'btn-starred' : ''}`}
          >
            <Star size={16} className={favorite ? 'filled' : ''} />
            <span>{favorite ? 'Favorited' : 'Favorite'}</span>
          </button>

          {!isEditing ? (
            <button onClick={() => setIsEditing(true)} className="btn btn-primary">
              Edit Page
            </button>
          ) : (
            <button onClick={() => setIsEditing(false)} className="btn btn-secondary">
              Cancel Edit
            </button>
          )}

          <button onClick={handleDelete} className="btn btn-danger">
            <Trash2 size={16} /> Delete
          </button>
        </div>
      </div>

      {isEditing ? (
        <Card className="details-edit-card">
          <form onSubmit={handleUpdate}>
            <div className="form-group">
              <label className="form-label">Page Title</label>
              <input
                type="text"
                className="form-input"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                required
              />
            </div>

            <div className="form-row">
              <div className="form-group flex-1">
                <label className="form-label">Category</label>
                <input
                  type="text"
                  className="form-input"
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                />
              </div>

              <div className="form-group flex-1">
                <label className="form-label">Tags</label>
                <input
                  type="text"
                  className="form-input"
                  value={tags}
                  onChange={(e) => setTags(e.target.value)}
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Content</label>
              <textarea
                className="form-textarea"
                rows={12}
                value={content}
                onChange={(e) => setContent(e.target.value)}
                required
              />
            </div>

            <div className="form-actions">
              <button type="submit" className="btn btn-primary" disabled={isSaving}>
                <Save size={16} /> {isSaving ? 'Saving Changes...' : 'Save Changes'}
              </button>
            </div>
          </form>
        </Card>
      ) : (
        <Card className="details-view-card">
          <div className="details-meta-bar">
            <span className="category-pill">{page.category || 'General'}</span>
            <span className="date-meta">
              <Calendar size={14} /> Created: {new Date(page.createdAt).toLocaleDateString()}
            </span>
            <span className="date-meta">
              Updated: {new Date(page.updatedAt).toLocaleDateString()}
            </span>
          </div>

          <h1 className="details-title">{page.title}</h1>

          {page.tags && (
            <div className="details-tags">
              <Tag size={14} className="tag-icon" />
              <span>{page.tags}</span>
            </div>
          )}

          <div className="details-body">
            {page.content.split('\n').map((paragraph, idx) => (
              <p key={idx}>{paragraph}</p>
            ))}
          </div>
        </Card>
      )}
    </div>
  );
};

export default KnowledgeDetailsPage;
