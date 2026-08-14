import React from 'react';
import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Sidebar from './components/Sidebar';
import Navbar from './components/Navbar';

// Pages
import HomePage from './pages/HomePage';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import KnowledgePage from './pages/KnowledgePage';
import KnowledgeDetailsPage from './pages/KnowledgeDetailsPage';
import DocumentsPage from './pages/DocumentsPage';
import IngestionPage from './pages/IngestionPage';
import GraphPage from './pages/GraphPage';
import AIPage from './pages/AIPage';
import SettingsPage from './pages/SettingsPage';

const AuthenticatedLayout: React.FC = () => {
  return (
    <div className="app-layout">
      <Sidebar />

      <div className="main-content">
        <Navbar />
        <Outlet />
      </div>
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>

          {/* Landing Page */}
          <Route path="/" element={<HomePage />} />

          {/* Public Authentication Routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          {/* Protected Application Routes */}
          <Route element={<ProtectedRoute />}>
            <Route element={<AuthenticatedLayout />}>

              <Route path="/dashboard" element={<Dashboard />} />

              <Route
                path="/knowledge"
                element={<KnowledgePage />}
              />

              <Route
                path="/knowledge/:id"
                element={<KnowledgeDetailsPage />}
              />

              <Route
                path="/documents"
                element={<DocumentsPage />}
              />

              <Route
                path="/ingest"
                element={<IngestionPage />}
              />

              <Route
                path="/graph"
                element={<GraphPage />}
              />

              <Route
                path="/ai"
                element={<AIPage />}
              />

              <Route
                path="/settings"
                element={<SettingsPage />}
              />

            </Route>
          </Route>

          {/* Unknown URL */}
          <Route
            path="*"
            element={<Navigate to="/" replace />}
          />

        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
};

export default App;