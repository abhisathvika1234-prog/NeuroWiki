export interface User {
  id: number;
  username: string;
  email: string;
}

export interface RegisterData {
  username: string;
  email: string;
  password: string;
}

export interface LoginCredentials {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface ProfileUpdateData {
  username: string;
  email: string;
}

export interface PasswordChangeData {
  currentPassword: string;
  newPassword: string;
}
