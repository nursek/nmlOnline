import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import apiClient from '../services/api';
import { Player } from '../types';

interface PlayerState {
  currentPlayer: Player | null;
  players: Player[];
  loading: boolean;
  error: string | null;
}

const initialState: PlayerState = {
  currentPlayer: null,
  players: [],
  loading: false,
  error: null,
};

export const fetchCurrentPlayer = createAsyncThunk(
  'player/fetchCurrent',
  async (username: string, { rejectWithValue }) => {
    try {
      const response = await apiClient.get<Player>(`/players/${username}`);
      return response.data;
    } catch (error: any) {
      return rejectWithValue(error.response?.data || 'Erreur lors de la récupération du joueur');
    }
  }
);

export const fetchAllPlayers = createAsyncThunk(
  'player/fetchAll',
  async (_, { rejectWithValue }) => {
    try {
      const response = await apiClient.get<Player[]>('/players');
      return response.data;
    } catch (error: any) {
      return rejectWithValue(error.response?.data || 'Erreur lors de la récupération des joueurs');
    }
  }
);

const playerSlice = createSlice({
  name: 'player',
  initialState,
  reducers: {
    clearPlayerError: (state) => {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch current player
      .addCase(fetchCurrentPlayer.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchCurrentPlayer.fulfilled, (state, action: PayloadAction<Player>) => {
        state.loading = false;
        state.currentPlayer = action.payload;
      })
      .addCase(fetchCurrentPlayer.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      })
      // Fetch all players
      .addCase(fetchAllPlayers.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchAllPlayers.fulfilled, (state, action: PayloadAction<Player[]>) => {
        state.loading = false;
        state.players = action.payload;
      })
      .addCase(fetchAllPlayers.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });
  },
});

export const { clearPlayerError } = playerSlice.actions;
export default playerSlice.reducer;

