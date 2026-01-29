import { createFeatureSelector, createSelector } from '@ngrx/store';
import { PlayerState } from './player.reducer';

export const selectPlayerState = createFeatureSelector<PlayerState>('player');

export const selectCurrentPlayer = createSelector(
  selectPlayerState,
  (state) => state.currentPlayer
);

export const selectAllPlayers = createSelector(
  selectPlayerState,
  (state) => state.players
);

export const selectPlayerLoading = createSelector(
  selectPlayerState,
  (state) => state.loading
);

export const selectPlayerError = createSelector(
  selectPlayerState,
  (state) => state.error
);
