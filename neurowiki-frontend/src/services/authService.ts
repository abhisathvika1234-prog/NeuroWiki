import { apiFetch, setStoredToken, removeStoredToken } from '../api/apiClient';
import { User, LoginCredentials, RegisterData, AuthResponse, ProfileUpdateData, PasswordChangeData } from '../types/user';

export const authService = {
  async register(data: RegisterData) {
    const res = await apiFetch<AuthResponse>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(data),
    });
    if (res.ok && res.data?.token) {
      setStoredToken(res.data.token);
    }
    return res;
  },

  async login(credentials: LoginCredentials) {
    const res = await apiFetch<AuthResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(credentials),
    });
    if (res.ok && res.data?.token) {
      setStoredToken(res.data.token);
    }
    return res;
  },

  async getCurrentUser() {
    return apiFetch<User>('/api/auth/me', {
      method: 'GET',
    });
  },

  async updateProfile(data: ProfileUpdateData) {
    return apiFetch<User>('/api/auth/profile', {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  async changePassword(data: PasswordChangeData) {
    return apiFetch<{ message: string }>('/api/auth/password', {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  logout() {
    removeStoredToken();
  },
};
