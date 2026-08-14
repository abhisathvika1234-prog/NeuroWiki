const TOKEN_KEY = 'neurowiki_jwt';
const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'https://neurowiki.onrender.com';

type UnauthorizedHandler = () => void;
let onUnauthorized: UnauthorizedHandler | null = null;

export function setUnauthorizedHandler(handler: UnauthorizedHandler) {
  onUnauthorized = handler;
}

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setStoredToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function removeStoredToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

export interface ApiResponse<T = any> {
  data?: T;
  message?: string;
  errors?: Record<string, string>;
  status: number;
  ok: boolean;
}

export async function apiFetch<T = any>(
  endpoint: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  const url = endpoint.startsWith('http') ? endpoint : `${BASE_URL}${endpoint}`;
  const token = getStoredToken();

  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string> || {}),
  };

  if (!(options.body instanceof FormData)) {
    if (!headers['Content-Type']) {
      headers['Content-Type'] = 'application/json';
    }
  }

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  try {
    const response = await fetch(url, {
      ...options,
      headers,
    });

    const status = response.status;

    if (status === 401) {
      if (onUnauthorized) {
        onUnauthorized();
      }
    }

    let json: any = null;
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      json = await response.json();
    }

    if (!response.ok) {
      return {
        ok: false,
        status,
        message: json?.message || `Request failed with status ${status}`,
        errors: json?.errors,
      };
    }

    return {
      ok: true,
      status,
      data: json as T,
    };
  } catch (error: any) {
    return {
      ok: false,
      status: 0,
      message: error?.message || 'Network error occurred. Please check your connection.',
    };
  }
}
