import { createReducer, on } from '@ngrx/store';
import { User } from '../../models';
import { AuthActions } from './auth.actions';

export interface AuthState {
  user: User | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  loading: boolean;
  error: string | null;
}

// Charger depuis localStorage
const storedUser = localStorage.getItem('user');
const storedToken = localStorage.getItem('accessToken');

export const initialState: AuthState = {
  user: storedUser ? JSON.parse(storedUser) : null,
  accessToken: storedToken,
  isAuthenticated: !!storedToken,
  loading: false,
  error: null,
};

export const authReducer = createReducer(
  initialState,

  on(AuthActions.login, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(AuthActions.loginSuccess, (state, { response }) => {
    const user: User = {
      id: response.userId,
      username: response.username,
    };
    localStorage.setItem('accessToken', response.accessToken);
    localStorage.setItem('user', JSON.stringify(user));
    return {
      ...state,
      loading: false,
      isAuthenticated: true,
      accessToken: response.accessToken,
      user,
    };
  }),

  on(AuthActions.loginFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  on(AuthActions.logoutSuccess, (state) => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('user');
    return {
      ...state,
      user: null,
      accessToken: null,
      isAuthenticated: false,
    };
  }),

  on(AuthActions.clearError, (state) => ({
    ...state,
    error: null,
  })),
);
