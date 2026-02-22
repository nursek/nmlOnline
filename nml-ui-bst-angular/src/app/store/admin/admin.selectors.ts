import { createFeatureSelector, createSelector } from '@ngrx/store';
import { AdminState } from './admin.reducer';

export const selectAdminState = createFeatureSelector<AdminState>('admin');

export const selectAdminPlayers = createSelector(
  selectAdminState,
  (state) => state.players
);

export const selectAdminLoading = createSelector(
  selectAdminState,
  (state) => state.loading
);

export const selectAdminImporting = createSelector(
  selectAdminState,
  (state) => state.importing
);

export const selectAdminError = createSelector(
  selectAdminState,
  (state) => state.error
);

export const selectAdminSuccessMessage = createSelector(
  selectAdminState,
  (state) => state.successMessage
);
