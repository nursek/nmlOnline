import { createReducer, on } from '@ngrx/store';
import { Player } from '../../models';
import { PlayerActions } from './player.actions';

export interface PlayerState {
  currentPlayer: Player | null;
  players: Player[];
  loading: boolean;
  error: string | null;
}

export const initialState: PlayerState = {
  currentPlayer: null,
  players: [],
  loading: false,
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
);
