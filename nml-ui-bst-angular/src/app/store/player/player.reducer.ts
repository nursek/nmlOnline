import { createReducer, on } from '@ngrx/store';
import { Player, Vehicle } from '../../models';
import { PlayerActions } from './player.actions';

export interface PlayerState {
  currentPlayer: Player | null;
  players: Player[];
  playerVehicles: Vehicle[];
  loading: boolean;
  vehiclesLoading: boolean;
  error: string | null;
}

export const initialState: PlayerState = {
  currentPlayer: null,
  players: [],
  playerVehicles: [],
  loading: false,
  vehiclesLoading: false,
  error: null,
};

export const playerReducer = createReducer(
  initialState,

  // Fetch current player
  on(PlayerActions.fetchCurrentPlayer, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(PlayerActions.fetchCurrentPlayerSuccess, (state, { player }) => ({
    ...state,
    loading: false,
    currentPlayer: player,
  })),

  on(PlayerActions.fetchCurrentPlayerFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Fetch all players
  on(PlayerActions.fetchAllPlayers, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(PlayerActions.fetchAllPlayersSuccess, (state, { players }) => ({
    ...state,
    loading: false,
    players,
  })),

  on(PlayerActions.fetchAllPlayersFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  on(PlayerActions.clearPlayerError, (state) => ({
    ...state,
    error: null,
  })),

  // Reset complet du state (lors du logout)
  on(PlayerActions.reset, () => initialState),

  // Véhicules du joueur
  on(PlayerActions.fetchPlayerVehicles, (state) => ({
    ...state,
    vehiclesLoading: true,
    error: null,
  })),

  on(PlayerActions.fetchPlayerVehiclesSuccess, (state, { vehicles }) => ({
    ...state,
    vehiclesLoading: false,
    playerVehicles: vehicles,
  })),

  on(PlayerActions.fetchPlayerVehiclesFailure, (state, { error }) => ({
    ...state,
    vehiclesLoading: false,
    error,
  })),

  on(PlayerActions.placeVehicle, (state) => ({
    ...state,
    vehiclesLoading: true,
    error: null,
  })),

  on(PlayerActions.placeVehicleSuccess, (state, { vehicle }) => ({
    ...state,
    vehiclesLoading: false,
    playerVehicles: state.playerVehicles.map((v) =>
      v.id === vehicle.id ? vehicle : v
    ),
  })),

  on(PlayerActions.placeVehicleFailure, (state, { error }) => ({
    ...state,
    vehiclesLoading: false,
    error,
  })),
);
