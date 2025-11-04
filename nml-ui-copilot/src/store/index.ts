import { configureStore } from '@reduxjs/toolkit';
import authReducer from './authSlice';
import playerReducer from './playerSlice';
import shopReducer from './shopSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    player: playerReducer,
    shop: shopReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;

