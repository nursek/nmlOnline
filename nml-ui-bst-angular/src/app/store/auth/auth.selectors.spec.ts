import {
  selectAuthState,
  selectUser,
  selectIsAuthenticated,
  selectAuthLoading,
  selectAuthError,
  selectAccessToken,
  selectIsInitialized,
  selectIsAppReady,
} from './auth.selectors';
import { AuthState } from './auth.reducer';

describe('Auth Selectors', () => {
  const authenticatedState: { auth: AuthState } = {
    auth: {
      user: { id: 1, username: 'TestPlayer' },
      accessToken: 'jwt-token',
      isAuthenticated: true,
      loading: false,
      error: null,
      initialized: true,
    },
  };

  const loadingState: { auth: AuthState } = {
    auth: {
      user: null,
      accessToken: null,
      isAuthenticated: false,
      loading: true,
      error: null,
      initialized: false,
    },
  };

  const errorState: { auth: AuthState } = {
    auth: {
      user: null,
      accessToken: null,
      isAuthenticated: false,
      loading: false,
      error: 'Login failed',
      initialized: true,
    },
  };

  it('should select auth state', () => {
    expect(selectAuthState(authenticatedState as any)).toEqual(authenticatedState.auth);
  });

  it('should select user', () => {
    expect(selectUser(authenticatedState as any)).toEqual({ id: 1, username: 'TestPlayer' });
    expect(selectUser(loadingState as any)).toBeNull();
  });

  it('should select isAuthenticated', () => {
    expect(selectIsAuthenticated(authenticatedState as any)).toBe(true);
    expect(selectIsAuthenticated(loadingState as any)).toBe(false);
  });

  it('should select loading', () => {
    expect(selectAuthLoading(loadingState as any)).toBe(true);
    expect(selectAuthLoading(authenticatedState as any)).toBe(false);
  });

  it('should select error', () => {
    expect(selectAuthError(errorState as any)).toBe('Login failed');
    expect(selectAuthError(authenticatedState as any)).toBeNull();
  });

  it('should select accessToken', () => {
    expect(selectAccessToken(authenticatedState as any)).toBe('jwt-token');
    expect(selectAccessToken(loadingState as any)).toBeNull();
  });

  it('should select isInitialized', () => {
    expect(selectIsInitialized(authenticatedState as any)).toBe(true);
    expect(selectIsInitialized(loadingState as any)).toBe(false);
  });

  it('should select isAppReady', () => {
    expect(selectIsAppReady(authenticatedState as any)).toBe(true);
    expect(selectIsAppReady(loadingState as any)).toBe(false);
    // Loading but initialized should not be ready
    const loadingInitialized = {
      auth: { ...authenticatedState.auth, loading: true },
    };
    expect(selectIsAppReady(loadingInitialized as any)).toBe(false);
  });
});
