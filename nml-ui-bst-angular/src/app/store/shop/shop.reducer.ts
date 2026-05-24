import { createReducer, on } from '@ngrx/store';
import { Equipment, CartItem, VehicleTypeInfo, VehicleCartItem, SellCartItem } from '../../models';
import { ShopActions } from './shop.actions';

/**
 * Normalise une quantité : retourne un entier >= `min` (défaut 1).
 * Protège contre NaN, Infinity, valeurs négatives et décimales.
 * Pour les actions de mise à jour, utiliser min=0 (la valeur 0 déclenchera la suppression).
 */
function safeQty(qty: number, min = 1): number {
  const n = Math.trunc(qty);
  return Number.isNaN(n) || !Number.isFinite(n) ? min : Math.max(n, min);
}

export interface ShopState {
  equipments: Equipment[];
  cart: CartItem[];
  vehicleTypes: VehicleTypeInfo[];
  vehicleCart: VehicleCartItem[];
  sellCart: SellCartItem[];
  equipmentsLoading: boolean;
  vehicleTypesLoading: boolean;
  purchaseLoading: boolean;
  error: string | null;
}

export const initialState: ShopState = {
  equipments: [],
  cart: [],
  vehicleTypes: [],
  vehicleCart: [],
  sellCart: [],
  equipmentsLoading: false,
  vehicleTypesLoading: false,
  purchaseLoading: false,
  error: null,
};

export const shopReducer = createReducer(
  initialState,

  // Fetch equipments
  on(ShopActions.fetchEquipments, (state) => ({
    ...state,
    equipmentsLoading: true,
    error: null,
  })),

  on(ShopActions.fetchEquipmentsSuccess, (state, { equipments }) => ({
    ...state,
    equipmentsLoading: false,
    equipments,
  })),

  on(ShopActions.fetchEquipmentsFailure, (state, { error }) => ({
    ...state,
    equipmentsLoading: false,
    error,
  })),

  // Load cart from localStorage
  on(ShopActions.loadCartSuccess, (state, { cart }) => ({
    ...state,
    cart,
  })),

  // Cart actions équipements
  on(ShopActions.addToCart, (state, { equipment }) => {
    const existingIndex = state.cart.findIndex(
      (item) => item.equipment.name === equipment.name
    );

    let newCart: CartItem[];
    if (existingIndex >= 0) {
      newCart = state.cart.map((item, index) =>
        index === existingIndex
          ? { ...item, quantity: item.quantity + 1 }
          : item
      );
    } else {
      newCart = [...state.cart, { equipment, quantity: 1 }];
    }

    return { ...state, cart: newCart };
  }),

  on(ShopActions.removeFromCart, (state, { name }) => ({
    ...state,
    cart: state.cart.filter((item) => item.equipment.name !== name),
  })),

  on(ShopActions.updateCartItemQuantity, (state, { name, quantity }) => {
    const qty = safeQty(quantity, 0);
    const newCart = qty <= 0
      ? state.cart.filter((item) => item.equipment.name !== name)
      : state.cart.map((item) =>
          item.equipment.name === name ? { ...item, quantity: qty } : item
        );
    return { ...state, cart: newCart };
  }),

  on(ShopActions.clearCart, (state) => ({
    ...state,
    cart: [],
  })),

  on(ShopActions.clearShopError, (state) => ({
    ...state,
    error: null,
  })),

  // Véhicules types
  on(ShopActions.fetchVehicleTypes, (state) => ({ ...state, vehicleTypesLoading: true, error: null })),
  on(ShopActions.fetchVehicleTypesSuccess, (state, { vehicleTypes }) => ({
    ...state,
    vehicleTypesLoading: false,
    vehicleTypes,
  })),
  on(ShopActions.fetchVehicleTypesFailure, (state, { error }) => ({
    ...state,
    vehicleTypesLoading: false,
    error,
  })),

  // Panier véhicules
  on(ShopActions.addVehicleToCart, (state, { vehicleType, quantity }) => {
    const qty = safeQty(quantity);
    const existingIndex = state.vehicleCart.findIndex(
      (item) => item.vehicleType.name === vehicleType.name
    );
    let newCart: VehicleCartItem[];
    if (existingIndex >= 0) {
      newCart = state.vehicleCart.map((item, index) =>
        index === existingIndex ? { ...item, quantity: item.quantity + qty } : item
      );
    } else {
      newCart = [...state.vehicleCart, { vehicleType, quantity: qty }];
    }
    return { ...state, vehicleCart: newCart };
  }),

  on(ShopActions.removeVehicleFromCart, (state, { name }) => ({
    ...state,
    vehicleCart: state.vehicleCart.filter((item) => item.vehicleType.name !== name),
  })),

  on(ShopActions.updateVehicleCartItemQuantity, (state, { name, quantity }) => {
    const qty = safeQty(quantity, 0);
    const newCart = qty <= 0
      ? state.vehicleCart.filter((item) => item.vehicleType.name !== name)
      : state.vehicleCart.map((item) =>
          item.vehicleType.name === name ? { ...item, quantity: qty } : item
        );
    return { ...state, vehicleCart: newCart };
  }),

  on(ShopActions.clearVehicleCart, (state) => ({ ...state, vehicleCart: [] })),

  on(ShopActions.loadVehicleCartSuccess, (state, { cart }) => ({ ...state, vehicleCart: cart })),

  on(ShopActions.checkoutVehicles, (state) => ({ ...state, purchaseLoading: true, error: null })),
  on(ShopActions.checkoutVehiclesSuccess, (state) => ({ ...state, purchaseLoading: false })),
  on(ShopActions.checkoutVehiclesFailure, (state, { error }) => ({
    ...state,
    purchaseLoading: false,
    error,
  })),

  // Checkout équipements
  on(ShopActions.checkoutEquipments, (state) => ({ ...state, purchaseLoading: true, error: null })),
  on(ShopActions.checkoutEquipmentsSuccess, (state) => ({ ...state, purchaseLoading: false })),
  on(ShopActions.checkoutEquipmentsFailure, (state, { error }) => ({
    ...state,
    purchaseLoading: false,
    error,
  })),

  // Panier revente de ressources
  on(ShopActions.addToSellCart, (state, { resource, quantity }) => {
    const qty = safeQty(quantity);
    const existingIndex = state.sellCart.findIndex(
      (item) => item.resource.id === resource.id
    );
    let newCart: SellCartItem[];
    if (existingIndex >= 0) {
      newCart = state.sellCart.map((item, index) =>
        index === existingIndex
          ? { ...item, quantity: Math.min(item.quantity + qty, item.resource.quantity) }
          : item
      );
    } else {
      newCart = [...state.sellCart, { resource, quantity: Math.min(qty, resource.quantity) }];
    }
    return { ...state, sellCart: newCart };
  }),

  on(ShopActions.removeFromSellCart, (state, { resourceId }) => ({
    ...state,
    sellCart: state.sellCart.filter((item) => item.resource.id !== resourceId),
  })),

  on(ShopActions.updateSellCartItemQuantity, (state, { resourceId, quantity }) => {
    const qty = safeQty(quantity, 0);
    const newCart = qty <= 0
      ? state.sellCart.filter((item) => item.resource.id !== resourceId)
      : state.sellCart.map((item) =>
          item.resource.id === resourceId ? { ...item, quantity: Math.min(qty, item.resource.quantity) } : item
        );
    return { ...state, sellCart: newCart };
  }),

  on(ShopActions.clearSellCart, (state) => ({ ...state, sellCart: [] })),

  on(ShopActions.checkoutSellCart, (state) => ({ ...state, purchaseLoading: true, error: null })),
  on(ShopActions.checkoutSellCartSuccess, (state) => ({ ...state, purchaseLoading: false })),
  on(ShopActions.checkoutSellCartFailure, (state, { error }) => ({
    ...state,
    purchaseLoading: false,
    error,
  })),
);
