import { createReducer, on } from '@ngrx/store';
import { User } from '../../models';
import { AuthActions } from './auth.actions';

export interface AuthState {
  user: User | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  loading: boolean;
  error: string | null;
  initialized: boolean; // Indique si la session a été vérifiée au démarrage
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

// État initial : si on a un token stocké, on marque comme potentiellement authentifié
// mais loading=true car on doit vérifier avec le serveur
export const initialState: AuthState = {
  user: storedUser,
  accessToken: storedToken,
  isAuthenticated: false, // Sera mis à true après vérification serveur
  loading: !!storedToken, // Loading si on a un token à vérifier
  error: null,
  initialized: !storedToken, // Déjà initialisé si pas de token
};

export const authReducer = createReducer(
  initialState,

  // === LOGIN ===
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
    // Note: Le stockage localStorage est fait dans l'effet ou le service
    localStorage.setItem('accessToken', response.token);
    localStorage.setItem('user', JSON.stringify(user));
    return {
      ...state,
      loading: false,
      isAuthenticated: true,
      accessToken: response.token,
      user,
      error: null,
      initialized: true,
    };
  }),

  on(AuthActions.loginFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // === LOGOUT ===
  on(AuthActions.logoutSuccess, (state) => {
    // Nettoyer le localStorage
    localStorage.removeItem('accessToken');
    localStorage.removeItem('user');
    return {
      ...state,
      user: null,
      accessToken: null,
      isAuthenticated: false,
      error: null,
      initialized: true,
    };
  }),

  // === CLEAR ERROR ===
  on(AuthActions.clearError, (state) => ({
    ...state,
    error: null,
  })),

  // === INIT SESSION (refresh au démarrage) ===
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
      initialized: true,
    };
  }),

  on(AuthActions.initSessionFailure, (state) => {
    // Nettoyer le localStorage en cas d'échec
    localStorage.removeItem('accessToken');
    localStorage.removeItem('user');
    return {
      ...state,
      loading: false,
      isAuthenticated: false,
      accessToken: null,
      user: null,
      initialized: true,
    };
  }),
);
