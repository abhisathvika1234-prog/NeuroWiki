import React from 'react';
import { Link } from 'react-router-dom';
import {
  Brain,
  Network,
  Sparkles,
  ArrowRight,
  BookOpen,
  Database,
} from 'lucide-react';

const HomePage: React.FC = () => {
  return (
    <div className="landing-page">
      <nav className="landing-navbar">
        <div className="landing-logo">
          <img src="/assets/images/logo.jpg" alt="NeuroWiki Logo" className="landing-logo-img" style={{ width: '40px', height: '40px', borderRadius: '10px', objectFit: 'cover' }} />

          <div>
            <h2>NeuroWiki</h2>
            <span>Neural Knowledge</span>
          </div>
        </div>

        <div className="landing-nav-actions">
          <Link to="/login" className="landing-signin">
            Sign In
          </Link>

          <Link to="/register" className="landing-get-started">
            Get Started
            <ArrowRight size={17} />
          </Link>
        </div>
      </nav>

      <main className="landing-hero">
        <div className="landing-glow landing-glow-one" />
        <div className="landing-glow landing-glow-two" />

        <div className="landing-hero-content">
          <div className="landing-badge">
            <Sparkles size={16} />
            Intelligent Knowledge Platform
          </div>

          <h1>
            Your Knowledge.
            <br />
            <span>Connected Intelligently.</span>
          </h1>

          <p>
            NeuroWiki transforms your knowledge and documents into an
            intelligent, searchable neural knowledge ecosystem powered by
            AI and Retrieval-Augmented Generation.
          </p>

          <div className="landing-buttons">
            <Link to="/register" className="landing-primary-btn">
              Get Started
              <ArrowRight size={19} />
            </Link>

            <Link to="/login" className="landing-secondary-btn">
              Sign In
            </Link>
          </div>

          <div className="landing-features">
            <div className="landing-feature">
              <div className="feature-icon">
                <Brain size={21} />
              </div>
              <div>
                <strong>AI Powered</strong>
                <span>Intelligent answers</span>
              </div>
            </div>

            <div className="landing-feature">
              <div className="feature-icon">
                <Network size={21} />
              </div>
              <div>
                <strong>Knowledge Graph</strong>
                <span>Connected concepts</span>
              </div>
            </div>

            <div className="landing-feature">
              <div className="feature-icon">
                <BookOpen size={21} />
              </div>
              <div>
                <strong>Knowledge Base</strong>
                <span>Organized knowledge</span>
              </div>
            </div>

            <div className="landing-feature">
              <div className="feature-icon">
                <Database size={21} />
              </div>
              <div>
                <strong>RAG Search</strong>
                <span>Grounded responses</span>
              </div>
            </div>
          </div>
        </div>

        <div className="landing-visual" style={{ position: 'relative', borderRadius: '24px', overflow: 'hidden', border: '1px solid rgba(255, 255, 255, 0.1)', boxShadow: '0 20px 40px rgba(0, 0, 0, 0.5)' }}>
          <img src="/assets/images/hero.jpg" alt="Neural Ecosystem Illustration" style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }} />

          <div className="neural-card card-one" style={{ backdropFilter: 'blur(8px)', background: 'rgba(15, 23, 42, 0.85)' }}>
            <Network size={18} />
            <span>Knowledge Graph</span>
          </div>

          <div className="neural-card card-two" style={{ backdropFilter: 'blur(8px)', background: 'rgba(15, 23, 42, 0.85)' }}>
            <Sparkles size={18} />
            <span>AI Intelligence</span>
          </div>

          <div className="neural-card card-three" style={{ backdropFilter: 'blur(8px)', background: 'rgba(15, 23, 42, 0.85)' }}>
            <BookOpen size={18} />
            <span>Knowledge Base</span>
          </div>
        </div>
      </main>

      <footer className="landing-footer">
        <span>© 2026 NeuroWiki</span>
        <span>Neural Knowledge Platform</span>
      </footer>
    </div>
  );
};

export default HomePage;