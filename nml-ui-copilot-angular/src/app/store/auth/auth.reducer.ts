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

// Charger depuis localStorage avec validation
function loadUserFromLocalStorage(): User | null {
  const storedUser = localStorage.getItem('user');
  if (!storedUser) return null;

  try {
    const parsed = JSON.parse(storedUser);
    // Valider que l'objet a les propriétés requises
    if (parsed && typeof parsed.id === 'number' && typeof parsed.username === 'string') {
      return parsed as User;
    }
    console.warn('Invalid user object in localStorage, removing it');
    localStorage.removeItem('user');
    return null;
  } catch (e) {
    console.error('Failed to parse user from localStorage:', e);
    localStorage.removeItem('user');
    return null;
  }
}

const storedUser = loadUserFromLocalStorage();
const storedToken = localStorage.getItem('accessToken');

export const initialState: AuthState = {
  user: storedUser,
  accessToken: storedToken,
  isAuthenticated: !!storedToken && !!storedUser,
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
      id: response.id,
      username: response.name,
    };
    localStorage.setItem('accessToken', response.token);
    localStorage.setItem('user', JSON.stringify(user));
    return {
      ...state,
      loading: false,
      isAuthenticated: true,
      accessToken: response.token,
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

  on(AuthActions.loadUserFromStorage, (state) => {
    const user = loadUserFromLocalStorage();
    const storedToken = localStorage.getItem('accessToken');

    return {
      ...state,
      user,
      accessToken: storedToken,
      isAuthenticated: !!storedToken && !!user,
    };
  }),

  // Init session (refresh au démarrage)
  on(AuthActions.initSession, (state) => ({
    ...state,
    loading: true,
  })),

  on(AuthActions.initSessionSuccess, (state, { token, id, username }) => {
    const user: User = { id, username };
    localStorage.setItem('accessToken', token);
    localStorage.setItem('user', JSON.stringify(user));
    return {
      ...state,
      loading: false,
      isAuthenticated: true,
      accessToken: token,
      user,
    };
  }),

  on(AuthActions.initSessionFailure, (state) => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('user');
    return {
      ...state,
      loading: false,
      isAuthenticated: false,
      accessToken: null,
      user: null,
    };
  }),
);
