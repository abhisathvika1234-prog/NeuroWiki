import React from 'react';
import { NavLink } from 'react-router-dom';
import { 
  LayoutDashboard, 
  BookOpen, 
  FileText, 
  UploadCloud, 
  Bot, 
  Network,
  Settings, 
  BrainCircuit,
  LogOut
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const Sidebar: React.FC = () => {
  const { user, logout } = useAuth();

  const navItems = [
    { path: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { path: '/knowledge', label: 'Knowledge Base', icon: BookOpen },
    { path: '/documents', label: 'Documents', icon: FileText },
    { path: '/ingest', label: 'Data Ingestion', icon: UploadCloud },
    { path: '/graph', label: 'Knowledge Graph', icon: Network },
    { path: '/ai', label: 'AI Assistant', icon: Bot },
    { path: '/settings', label: 'Settings', icon: Settings },
  ];

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <div className="brand-icon">
          <BrainCircuit size={28} className="brand-logo-svg" />
        </div>
        <div className="brand-text">
          <h2>NEUROWIKI</h2>
          <span>Neural Intelligence</span>
        </div>
      </div>

      <nav className="sidebar-nav">
        <ul>
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <li key={item.path}>
                <NavLink
                  to={item.path}
                  className={({ isActive }) =>
                    isActive ? 'nav-link active' : 'nav-link'
                  }
                >
                  <Icon size={20} className="nav-icon" />
                  <span>{item.label}</span>
                </NavLink>
              </li>
            );
          })}
        </ul>
      </nav>

      <div className="sidebar-footer">
        <div className="user-profile-badge">
          <div className="avatar-circle">
            {user?.username ? user.username.charAt(0).toUpperCase() : 'U'}
          </div>
          <div className="user-info-text">
            <p className="user-name">{user?.username}</p>
            <p className="user-email">{user?.email}</p>
          </div>
        </div>
        <button onClick={logout} className="logout-btn" title="Logout">
          <LogOut size={18} />
        </button>
      </div>
    </aside>
  );
};

export default Sidebar;
