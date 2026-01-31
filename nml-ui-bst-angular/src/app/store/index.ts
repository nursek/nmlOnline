import { authReducer, AuthState } from './auth/auth.reducer';
import { playerReducer, PlayerState } from './player/player.reducer';
import { shopReducer, ShopState } from './shop/shop.reducer';

export interface AppState {
  auth: AuthState;
  player: PlayerState;
  shop: ShopState;
}

export const reducers = {
  auth: authReducer,
  player: playerReducer,
  shop: shopReducer,
};

// Re-export all actions and selectors
export * from './auth/auth.actions';
export * from './auth/auth.selectors';
export * from './player/player.actions';
export * from './player/player.selectors';
export * from './shop/shop.actions';
export * from './shop/shop.selectors';
