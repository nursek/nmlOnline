import { createReducer, on } from '@ngrx/store';
import { User } from '../../models';
import { AuthActions } from './auth.actions';

export interface AuthState {
  user: User | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  loading: boolean;
  error: string | null;
  initialized: boolean;
}

export const initialState: AuthState = {
  user: null,
  accessToken: null,
  isAuthenticated: false,
  loading: true, // Loading until initSession completes
  error: null,
  initialized: false,
};

export const authReducer = createReducer(
  initialState,

  // === LOGIN ===
  on(AuthActions.login, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(AuthActions.loginSuccess, (state, { response }) => ({
    ...state,
    loading: false,
    isAuthenticated: true,
    accessToken: response.token,
    user: { id: response.id, username: response.name },
    error: null,
    initialized: true,
  })),

  on(AuthActions.loginFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // === LOGOUT ===
  on(AuthActions.logoutSuccess, (state) => ({
    ...state,
    user: null,
    accessToken: null,
    isAuthenticated: false,
    error: null,
    initialized: true,
  })),

  // === CLEAR ERROR ===
  on(AuthActions.clearError, (state) => ({
    ...state,
    error: null,
  })),

  // === INIT SESSION ===
  on(AuthActions.initSession, (state) => ({
    ...state,
    loading: true,
  })),

  on(AuthActions.initSessionSuccess, (state, { token, id, username }) => ({
    ...state,
    loading: false,
    isAuthenticated: true,
    accessToken: token,
    user: { id, username },
    initialized: true,
  })),

  on(AuthActions.initSessionFailure, (state) => ({
    ...state,
    loading: false,
    isAuthenticated: false,
    accessToken: null,
    user: null,
    initialized: true,
  })),
);
