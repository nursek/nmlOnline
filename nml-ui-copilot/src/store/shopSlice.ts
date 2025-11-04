import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import apiClient from '../services/api';
import { Equipment, CartItem } from '../types';

interface ShopState {
  equipments: Equipment[];
  cart: CartItem[];
  loading: boolean;
  error: string | null;
}

const initialState: ShopState = {
  equipments: [],
  cart: JSON.parse(localStorage.getItem('cart') || '[]'),
  loading: false,
  error: null,
};

export const fetchEquipments = createAsyncThunk(
  'shop/fetchEquipments',
  async (_, { rejectWithValue }) => {
    try {
      const response = await apiClient.get<Equipment[]>('/equipment');
      return response.data;
    } catch (error: any) {
      return rejectWithValue(error.response?.data || 'Erreur lors de la récupération des équipements');
    }
  }
);

const shopSlice = createSlice({
  name: 'shop',
  initialState,
  reducers: {
    addToCart: (state, action: PayloadAction<Equipment>) => {
      const existingItem = state.cart.find(
        (item) => item.equipment.name === action.payload.name
      );

      if (existingItem) {
        existingItem.quantity += 1;
      } else {
        state.cart.push({ equipment: action.payload, quantity: 1 });
      }

      localStorage.setItem('cart', JSON.stringify(state.cart));
    },
    removeFromCart: (state, action: PayloadAction<string>) => {
      state.cart = state.cart.filter(
        (item) => item.equipment.name !== action.payload
      );
      localStorage.setItem('cart', JSON.stringify(state.cart));
    },
    updateCartItemQuantity: (
      state,
      action: PayloadAction<{ name: string; quantity: number }>
    ) => {
      const item = state.cart.find(
        (item) => item.equipment.name === action.payload.name
      );

      if (item) {
        if (action.payload.quantity <= 0) {
          state.cart = state.cart.filter(
            (item) => item.equipment.name !== action.payload.name
          );
        } else {
          item.quantity = action.payload.quantity;
        }
        localStorage.setItem('cart', JSON.stringify(state.cart));
      }
    },
    clearCart: (state) => {
      state.cart = [];
      localStorage.removeItem('cart');
    },
    clearShopError: (state) => {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchEquipments.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchEquipments.fulfilled, (state, action: PayloadAction<Equipment[]>) => {
        state.loading = false;
        state.equipments = action.payload;
      })
      .addCase(fetchEquipments.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });
  },
});

export const {
  addToCart,
  removeFromCart,
  updateCartItemQuantity,
  clearCart,
  clearShopError,
} = shopSlice.actions;

export default shopSlice.reducer;

