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
  (state) => state.equipmentsLoading || state.vehicleTypesLoading
);

export const selectShopError = createSelector(
  selectShopState,
  (state) => state.error
);

export const selectVehicleTypes = createSelector(
  selectShopState,
  (state) => state.vehicleTypes
);

export const selectPurchaseLoading = createSelector(
  selectShopState,
  (state) => state.purchaseLoading
);

// Panier véhicules
export const selectVehicleCart = createSelector(
  selectShopState,
  (state) => state.vehicleCart
);

export const selectVehicleCartTotalItems = createSelector(
  selectVehicleCart,
  (cart) => cart.reduce((sum, item) => sum + item.quantity, 0)
);

export const selectVehicleCartTotalPrice = createSelector(
  selectVehicleCart,
  (cart) => cart.reduce((sum, item) => sum + item.vehicleType.cost * item.quantity, 0)
);

// Panier revente de ressources
export const selectSellCart = createSelector(
  selectShopState,
  (state) => state.sellCart
);

export const selectSellCartTotalValue = createSelector(
  selectSellCart,
  (cart) => cart.reduce((sum, item) => sum + (item.resource.baseValue ?? 0) * item.quantity, 0)
);
