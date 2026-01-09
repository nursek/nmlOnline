export interface LoginRequest {
  username: string;
  password: string;
  rememberMe?: boolean;
}

export interface LoginResponse {
  token: string;
  id: number;
  username: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
}

export interface RefreshResponse {
  valid: boolean;
  token?: string;
  id?: number;
  name?: string;
}
