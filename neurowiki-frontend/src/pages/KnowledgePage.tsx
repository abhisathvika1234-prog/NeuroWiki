import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { knowledgeService } from '../services/knowledgeService';
import { KnowledgePage as IKnowledgePage } from '../types/knowledge';
import Card from '../components/Card';
import Modal from '../components/Modal';
import EmptyState from '../components/EmptyState';
import LoadingSpinner from '../components/LoadingSpinner';
import { BookOpen, Plus, Star, Trash2, Edit3, Filter, Tag } from 'lucide-react';

export const KnowledgePage: React.FC = () => {
  const [items, setItems] = useState<IKnowledgePage[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const [selectedCategory, setSelectedCategory] = useState<string>('');
  const [onlyFavorites, setOnlyFavorites] = useState<boolean>(false);

  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState('General');
  const [tags, setTags] = useState('');
  const [favorite, setFavorite] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    fetchKnowledge();
  }, [selectedCategory, onlyFavorites]);

  const fetchKnowledge = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await knowledgeService.getAll(selectedCategory, onlyFavorites);
      if (res.ok && res.data) {
        setItems(res.data);
      } else {
        setError(res.message || 'Failed to fetch knowledge pages');
      }
    } catch (err: any) {
      setError(err?.message || 'Error loading knowledge');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) return;

    setIsSubmitting(true);
    try {
      const res = await knowledgeService.create({
        title,
        content,
        category,
        tags,
        favorite,
      });

      if (res.ok && res.data) {
        setIsModalOpen(false);
        setTitle('');
        setContent('');
        setCategory('General');
        setTags('');
        setFavorite(false);
        fetchKnowledge();
      } else {
        setError(res.message || 'Failed to create knowledge page');
      }
    } catch (err: any) {
      setError(err?.message || 'Error creating knowledge page');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleToggleFavorite = async (id: number, e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    try {
      const res = await knowledgeService.toggleFavorite(id);
      if (res.ok && res.data) {
        setItems((prev) =>
          prev.map((item) => (item.id === id ? res.data! : item))
        );
      }
    } catch (err) {
      console.error('Error toggling favorite', err);
    }
  };

  const handleDelete = async (id: number, e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (!window.confirm('Are you sure you want to delete this knowledge page?')) return;

    try {
      const res = await knowledgeService.delete(id);
      if (res.ok) {
        setItems((prev) => prev.filter((item) => item.id !== id));
      }
    } catch (err) {
      console.error('Error deleting knowledge page', err);
    }
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Knowledge Base</h1>
          <p className="page-subtitle">Manage and organize your structured knowledge pages</p>
        </div>
        <button onClick={() => setIsModalOpen(true)} className="btn btn-primary">
          <Plus size={16} /> New Page
        </button>
      </div>

      <div className="filter-bar">
        <div className="filter-group">
          <Filter size={16} className="filter-icon" />
          <select
            value={selectedCategory}
            onChange={(e) => setSelectedCategory(e.target.value)}
            className="filter-select"
          >
            <option value="">All Categories</option>
            <option value="General">General</option>
            <option value="Engineering">Engineering</option>
            <option value="Research">Research</option>
            <option value="Documentation">Documentation</option>
            <option value="Personal">Personal</option>
          </select>
        </div>

        <button
          onClick={() => setOnlyFavorites(!onlyFavorites)}
          className={`btn-filter-toggle ${onlyFavorites ? 'active' : ''}`}
        >
          <Star size={16} className={onlyFavorites ? 'filled' : ''} />
          <span>Favorites Only</span>
        </button>
      </div>

      {error && (
        <div className="alert alert-error margin-bottom-lg">
          <span>{error}</span>
        </div>
      )}

      {loading ? (
        <LoadingSpinner text="Fetching knowledge entries..." />
      ) : items.length === 0 ? (
        <EmptyState
          icon={BookOpen}
          title="No knowledge yet."
          description="Click 'New Page' above to create your first knowledge entry."
          actionText="Create Knowledge Page"
          onAction={() => setIsModalOpen(true)}
        />
      ) : (
        <div className="knowledge-grid">
          {items.map((item) => (
            <Card key={item.id} className="knowledge-card">
              <div className="card-top">
                <span className="category-pill">{item.category || 'General'}</span>
                <div className="card-actions">
                  <button
                    onClick={(e) => handleToggleFavorite(item.id, e)}
                    className="icon-btn star-btn"
                    title="Toggle Favorite"
                  >
                    <Star size={18} className={item.favorite ? 'filled' : ''} />
                  </button>
                  <button
                    onClick={(e) => handleDelete(item.id, e)}
                    className="icon-btn delete-btn"
                    title="Delete"
                  >
                    <Trash2 size={18} />
                  </button>
                </div>
              </div>

              <h3 className="card-title">{item.title}</h3>
              <p className="card-content-preview">
                {item.content.length > 150 ? item.content.slice(0, 150) + '...' : item.content}
              </p>

              {item.tags && (
                <div className="card-tags">
                  <Tag size={12} className="tag-icon" />
                  <span>{item.tags}</span>
                </div>
              )}

              <div className="card-footer">
                <span className="card-date">
                  Updated {new Date(item.updatedAt).toLocaleDateString()}
                </span>
                <Link to={`/knowledge/${item.id}`} className="read-more-link">
                  View Details <Edit3 size={14} />
                </Link>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Create Knowledge Page"
      >
        <form onSubmit={handleCreate} className="modal-form">
          <div className="form-group">
            <label className="form-label">Title</label>
            <input
              type="text"
              className="form-input"
              placeholder="e.g. Neural Architecture Design"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Category</label>
            <input
              type="text"
              className="form-input"
              placeholder="e.g. Engineering, Research, Notes"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label className="form-label">Tags (comma separated)</label>
            <input
              type="text"
              className="form-input"
              placeholder="e.g. springboot, react, postgres"
              value={tags}
              onChange={(e) => setTags(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label className="form-label">Content</label>
            <textarea
              className="form-textarea"
              rows={6}
              placeholder="Write knowledge details here..."
              value={content}
              onChange={(e) => setContent(e.target.value)}
              required
            />
          </div>

          <div className="form-checkbox-group">
            <input
              type="checkbox"
              id="fav-check"
              checked={favorite}
              onChange={(e) => setFavorite(e.target.checked)}
            />
            <label htmlFor="fav-check">Mark as Favorite</label>
          </div>

          <div className="modal-actions">
            <button
              type="button"
              onClick={() => setIsModalOpen(false)}
              className="btn btn-secondary"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Saving...' : 'Save Knowledge'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default KnowledgePage;
