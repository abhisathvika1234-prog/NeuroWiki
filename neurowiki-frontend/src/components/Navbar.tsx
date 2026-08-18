import React, { useState } from 'react';
import { Search, User as UserIcon, Sparkles } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { searchService } from '../services/searchService';
import { SearchResponse } from '../types/api';

export const Navbar: React.FC = () => {
  const { user } = useAuth();
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<SearchResponse | null>(null);
  const [isSearching, setIsSearching] = useState(false);
  const [showDropdown, setShowDropdown] = useState(false);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!searchQuery.trim()) return;

    setIsSearching(true);
    setShowDropdown(true);
    try {
      const res = await searchService.search(searchQuery);
      if (res.ok && res.data) {
        setSearchResults(res.data);
      }
    } catch (err) {
      console.error('Search error', err);
    } finally {
      setIsSearching(false);
    }
  };

  const closeSearch = () => {
    setShowDropdown(false);
  };

  return (
    <header className="top-navbar">
      <div className="search-container">
        <form onSubmit={handleSearch} className="search-form">
          <Search size={18} className="search-icon" />
          <input
            type="text"
            placeholder="Search your neural knowledge and documents..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="search-input"
          />
        </form>

        {showDropdown && (
          <div className="search-results-dropdown">
            <div className="dropdown-header">
              <span>Search Results for "{searchQuery}"</span>
              <button onClick={closeSearch} className="close-dropdown-btn">&times;</button>
            </div>
            {isSearching ? (
              <div className="dropdown-loading">Searching...</div>
            ) : searchResults && (searchResults.knowledge.length > 0 || searchResults.documents.length > 0) ? (
              <div className="dropdown-content">
                {searchResults.knowledge.length > 0 && (
                  <div className="result-section">
                    <h5 className="result-type-title">Knowledge Pages ({searchResults.knowledge.length})</h5>
                    {searchResults.knowledge.map((k) => (
                      <a key={k.id} href={`/knowledge/${k.id}`} className="result-item">
                        <span className="item-title">{k.title}</span>
                        <span className="item-badge">{k.category}</span>
                      </a>
                    ))}
                  </div>
                )}
                {searchResults.documents.length > 0 && (
                  <div className="result-section">
                    <h5 className="result-type-title">Documents ({searchResults.documents.length})</h5>
                    {searchResults.documents.map((d) => (
                      <div key={d.id} className="result-item">
                        <span className="item-title">{d.title}</span>
                        <span className="item-badge">{d.type}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ) : (
              <div className="dropdown-empty">No matching knowledge or documents found.</div>
            )}
          </div>
        )}
      </div>

      <div className="navbar-actions">
        <div className="status-pill">
          <Sparkles size={14} className="sparkle-icon" />
          <span>Neural Engine Active</span>
        </div>
        <div className="user-greeting" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <img src="/assets/images/user-avatar.jpg" alt="User Avatar" style={{ width: '28px', height: '28px', borderRadius: '50%', objectFit: 'cover' }} />
          <span>{user?.username}</span>
        </div>
      </div>
    </header>
  );
};

export default Navbar;
