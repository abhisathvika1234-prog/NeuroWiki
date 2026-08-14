import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { User, LoginCredentials, RegisterData } from '../types/user';
import { authService } from '../services/authService';
import { getStoredToken, removeStoredToken, setUnauthorizedHandler } from '../api/apiClient';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginCredentials) => Promise<{ ok: boolean; message?: string }>;
  register: (data: RegisterData) => Promise<{ ok: boolean; message?: string }>;
  logout: () => void;
  updateUser: (updatedUser: User) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const logout = useCallback(() => {
    removeStoredToken();
    setUser(null);
    setIsAuthenticated(false);
  }, []);

  const restoreSession = useCallback(async () => {
    const token = getStoredToken();
    if (!token) {
      setUser(null);
      setIsAuthenticated(false);
      setIsLoading(false);
      return;
    }

    try {
      const res = await authService.getCurrentUser();
      if (res.ok && res.data) {
        setUser(res.data);
        setIsAuthenticated(true);
      } else {
        logout();
      }
    } catch (err) {
      logout();
    } finally {
      setIsLoading(false);
    }
  }, [logout]);

  useEffect(() => {
    setUnauthorizedHandler(() => {
      logout();
    });
    restoreSession();
  }, [restoreSession, logout]);

  const login = async (credentials: LoginCredentials) => {
    setIsLoading(true);
    try {
      const res = await authService.login(credentials);
      if (res.ok && res.data) {
        setUser(res.data.user);
        setIsAuthenticated(true);
        setIsLoading(false);
        return { ok: true };
      }
      setIsLoading(false);
      return { ok: false, message: res.message || 'Login failed' };
    } catch (err: any) {
      setIsLoading(false);
      return { ok: false, message: err?.message || 'Login failed' };
    }
  };

  const register = async (data: RegisterData) => {
    setIsLoading(true);
    try {
      const res = await authService.register(data);
      if (res.ok && res.data) {
        setUser(res.data.user);
        setIsAuthenticated(true);
        setIsLoading(false);
        return { ok: true };
      }
      setIsLoading(false);
      return { ok: false, message: res.message || 'Registration failed' };
    } catch (err: any) {
      setIsLoading(false);
      return { ok: false, message: err?.message || 'Registration failed' };
    }
  };

  const updateUser = (updatedUser: User) => {
    setUser(updatedUser);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated,
        isLoading,
        login,
        register,
        logout,
        updateUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
