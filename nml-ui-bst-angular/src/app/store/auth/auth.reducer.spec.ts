import { authReducer, initialState, AuthState } from './auth.reducer';
import { AuthActions } from './auth.actions';
import { AuthResponse } from '../../models';

describe('AuthReducer', () => {
  const mockResponse: AuthResponse = {
    token: 'test-jwt-token',
    id: 1,
    name: 'TestPlayer',
  };

  describe('initial state', () => {
    it('should return the initial state', () => {
      const state = authReducer(undefined, { type: 'NOOP' } as any);
      expect(state).toEqual(initialState);
    });

    it('should start with loading true and initialized false', () => {
      expect(initialState.loading).toBe(true);
      expect(initialState.initialized).toBe(false);
      expect(initialState.isAuthenticated).toBe(false);
      expect(initialState.user).toBeNull();
      expect(initialState.accessToken).toBeNull();
    });
  });

  describe('Login', () => {
    it('should set loading to true on login', () => {
      const state = authReducer(
        initialState,
        AuthActions.login({ credentials: { username: 'test', password: 'pass', rememberMe: false } })
      );
      expect(state.loading).toBe(true);
      expect(state.error).toBeNull();
    });

    it('should set user and token on loginSuccess', () => {
      const state = authReducer(
        { ...initialState, loading: true },
        AuthActions.loginSuccess({ response: mockResponse })
      );
      expect(state.loading).toBe(false);
      expect(state.isAuthenticated).toBe(true);
      expect(state.accessToken).toBe('test-jwt-token');
      expect(state.user).toEqual({ id: 1, username: 'TestPlayer' });
      expect(state.error).toBeNull();
      expect(state.initialized).toBe(true);
    });

    it('should set error on loginFailure', () => {
      const state = authReducer(
        { ...initialState, loading: true },
        AuthActions.loginFailure({ error: 'Invalid credentials' })
      );
      expect(state.loading).toBe(false);
      expect(state.error).toBe('Invalid credentials');
      expect(state.isAuthenticated).toBe(false);
    });
  });

  describe('Logout', () => {
    it('should clear state on logoutSuccess', () => {
      const authenticatedState: AuthState = {
        user: { id: 1, username: 'TestPlayer' },
        accessToken: 'some-token',
        isAuthenticated: true,
        loading: false,
        error: null,
        initialized: true,
      };

      const state = authReducer(authenticatedState, AuthActions.logoutSuccess());
      expect(state.user).toBeNull();
      expect(state.accessToken).toBeNull();
      expect(state.isAuthenticated).toBe(false);
      expect(state.initialized).toBe(true);
    });
  });

  describe('Clear Error', () => {
    it('should clear the error', () => {
      const errorState: AuthState = {
        ...initialState,
        error: 'Some error',
      };

      const state = authReducer(errorState, AuthActions.clearError());
      expect(state.error).toBeNull();
    });
  });

  describe('Init Session', () => {
    it('should set loading on initSession', () => {
      const state = authReducer(initialState, AuthActions.initSession());
      expect(state.loading).toBe(true);
    });

    it('should restore user on initSessionSuccess', () => {
      const state = authReducer(
        initialState,
        AuthActions.initSessionSuccess({ token: 'refreshed-token', id: 2, username: 'Player2' })
      );
      expect(state.loading).toBe(false);
      expect(state.isAuthenticated).toBe(true);
      expect(state.accessToken).toBe('refreshed-token');
      expect(state.user).toEqual({ id: 2, username: 'Player2' });
      expect(state.initialized).toBe(true);
    });

    it('should clear state on initSessionFailure', () => {
      const state = authReducer(initialState, AuthActions.initSessionFailure());
      expect(state.loading).toBe(false);
      expect(state.isAuthenticated).toBe(false);
      expect(state.accessToken).toBeNull();
      expect(state.user).toBeNull();
      expect(state.initialized).toBe(true);
    });
  });

  describe('Reducer purity', () => {
    it('should not modify the original state', () => {
      const original = { ...initialState };
      authReducer(
        original,
        AuthActions.loginSuccess({ response: mockResponse })
      );
      expect(original).toEqual(initialState);
    });
  });
});
