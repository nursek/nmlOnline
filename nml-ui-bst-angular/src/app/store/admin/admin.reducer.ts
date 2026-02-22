import { createReducer, on } from '@ngrx/store';
import { Player } from '../../models';
import { AdminActions } from './admin.actions';

export interface AdminState {
  players: Player[];
  loading: boolean;
  importing: boolean;
  error: string | null;
  successMessage: string | null;
}

export const initialState: AdminState = {
  players: [],
  loading: false,
  importing: false,
  error: null,
  successMessage: null,
};

export const adminReducer = createReducer(
  initialState,

  // Fetch all players
  on(AdminActions.fetchAdminPlayers, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(AdminActions.fetchAdminPlayersSuccess, (state, { players }) => ({
    ...state,
    loading: false,
    players,
  })),

  on(AdminActions.fetchAdminPlayersFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Import player
  on(AdminActions.importPlayer, (state) => ({
    ...state,
    importing: true,
    error: null,
    successMessage: null,
  })),

  on(AdminActions.importPlayerSuccess, (state, { player }) => ({
    ...state,
    importing: false,
    successMessage: `Joueur "${player.name}" importé avec succès`,
  })),

  on(AdminActions.importPlayerFailure, (state, { error }) => ({
    ...state,
    importing: false,
    error,
  })),

  // Delete player
  on(AdminActions.deletePlayer, (state) => ({
    ...state,
    loading: true,
    error: null,
    successMessage: null,
  })),

  on(AdminActions.deletePlayerSuccess, (state, { playerId }) => ({
    ...state,
    loading: false,
    players: state.players.filter(p => p.id !== playerId),
    successMessage: 'Joueur supprimé avec succès',
  })),

  on(AdminActions.deletePlayerFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Clear messages
  on(AdminActions.clearAdminMessages, (state) => ({
    ...state,
    error: null,
    successMessage: null,
  })),
);
