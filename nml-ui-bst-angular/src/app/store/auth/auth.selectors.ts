import { createFeatureSelector, createSelector } from '@ngrx/store';
import { AuthState } from './auth.reducer';

export const selectAuthState = createFeatureSelector<AuthState>('auth');

export const selectUser = createSelector(
  selectAuthState,
  (state) => state.user
);

export const selectIsAuthenticated = createSelector(
  selectAuthState,
  (state) => state.isAuthenticated
);

export const selectAuthLoading = createSelector(
  selectAuthState,
  (state) => state.loading
);

export const selectAuthError = createSelector(
  selectAuthState,
  (state) => state.error
);

export const selectAccessToken = createSelector(
  selectAuthState,
  (state) => state.accessToken
);

export const selectIsInitialized = createSelector(
  selectAuthState,
  (state) => state.initialized
);

/**
 * Selector qui combine loading et initialized pour savoir si l'app est prête.
 * Utile pour afficher un spinner global au démarrage.
 */
export const selectIsAppReady = createSelector(
  selectAuthState,
  (state) => state.initialized && !state.loading
);

