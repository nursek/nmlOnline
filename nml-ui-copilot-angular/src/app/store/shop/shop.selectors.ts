import { createFeatureSelector, createSelector } from '@ngrx/store';
import { ShopState } from './shop.reducer';

export const selectShopState = createFeatureSelector<ShopState>('shop');

export const selectEquipments = createSelector(
  selectShopState,
  (state) => state.equipments
);

export const selectCart = createSelector(
  selectShopState,
  (state) => state.cart
);

export const selectCartTotalItems = createSelector(
  selectCart,
  (cart) => cart.reduce((sum, item) => sum + item.quantity, 0)
);

export const selectCartTotalPrice = createSelector(
  selectCart,
  (cart) => cart.reduce((sum, item) => sum + item.equipment.cost * item.quantity, 0)
);

export const selectShopLoading = createSelector(
  selectShopState,
  (state) => state.loading
);

export const selectShopError = createSelector(
  selectShopState,
  (state) => state.error
);
