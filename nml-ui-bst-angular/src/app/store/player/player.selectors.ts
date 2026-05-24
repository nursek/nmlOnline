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

export const selectPlayerVehicles = createSelector(
  selectPlayerState,
  (state) => state.playerVehicles
);

export const selectUndeployedVehicles = createSelector(
  selectPlayerState,
  (state) => state.playerVehicles.filter((v) => v.sectorNumber === null)
);

export const selectVehiclesLoading = createSelector(
  selectPlayerState,
  (state) => state.vehiclesLoading
);
