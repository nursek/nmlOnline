export interface LoginRequest {
  username: string;
  password: string;
  rememberMe: boolean;
}

// Correspond à AuthResponse du backend (token, id, name)
export interface AuthResponse {
  token: string;
  id: number;
  name: string;
  role: string;
  avatarUrl?: string | null;
}

export interface RefreshResponse {
  valid: boolean;
  token?: string;
  id?: number;
  name?: string;
  role?: string;
  avatarUrl?: string | null;
  error?: string;
}

export interface User {
  id: number;
  username: string;
  role?: string;
  avatarUrl?: string | null;
}
