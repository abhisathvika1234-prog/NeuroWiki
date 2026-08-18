import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
} from 'react';

import { User, LoginCredentials, RegisterData } from '../types/user';
import { authService } from '../services/authService';
import {
  getStoredToken,
  removeStoredToken,
  setUnauthorizedHandler,
} from '../api/apiClient';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;

  login: (
    credentials: LoginCredentials
  ) => Promise<{ ok: boolean; message?: string }>;

  register: (
    data: RegisterData
  ) => Promise<{ ok: boolean; message?: string }>;

  logout: () => void;

  updateUser: (updatedUser: User) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{
  children: React.ReactNode;
}> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

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
      const response = await authService.getCurrentUser();

      if (response.ok && response.data) {
        setUser(response.data);
        setIsAuthenticated(true);
      } else {
        logout();
      }
    } catch (error) {
      console.error('Session restore failed:', error);
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

  const login = async (
    credentials: LoginCredentials
  ): Promise<{ ok: boolean; message?: string }> => {
    setIsLoading(true);

    try {
      const response = await authService.login(credentials);

      if (response.ok && response.data) {
        setUser(response.data.user);
        setIsAuthenticated(true);

        return {
          ok: true,
        };
      }

      return {
        ok: false,
        message: response.message || 'Login failed',
      };
    } catch (error) {
      console.error('Login error:', error);

      return {
        ok: false,
        message:
          error instanceof Error
            ? error.message
            : 'Login failed',
      };
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (
    data: RegisterData
  ): Promise<{ ok: boolean; message?: string }> => {
    setIsLoading(true);

    try {
      const response = await authService.register(data);

      if (response.ok && response.data) {
        setUser(response.data.user);
        setIsAuthenticated(true);

        return {
          ok: true,
        };
      }

      return {
        ok: false,
        message: response.message || 'Registration failed',
      };
    } catch (error) {
      console.error('Registration error:', error);

      return {
        ok: false,
        message:
          error instanceof Error
            ? error.message
            : 'Registration failed',
      };
    } finally {
      setIsLoading(false);
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

  if (context === undefined) {
    throw new Error(
      'useAuth must be used within an AuthProvider'
    );
  }

  return context;
};