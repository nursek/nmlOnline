import { createReducer, on } from '@ngrx/store';
import { Equipment, CartItem } from '../../models';
import { ShopActions } from './shop.actions';

export interface ShopState {
  equipments: Equipment[];
  cart: CartItem[];
  loading: boolean;
  error: string | null;
}

export const initialState: ShopState = {
  equipments: [],
  cart: [],
  loading: false,
  error: null,
};

export const shopReducer = createReducer(
  initialState,

  // Fetch equipments
  on(ShopActions.fetchEquipments, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(ShopActions.fetchEquipmentsSuccess, (state, { equipments }) => ({
    ...state,
    loading: false,
    equipments,
  })),

  on(ShopActions.fetchEquipmentsFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Load cart from localStorage
  on(ShopActions.loadCartSuccess, (state, { cart }) => ({
    ...state,
    cart,
  })),

  // Cart actions (pure - no side effects)
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
    const newCart = quantity <= 0
      ? state.cart.filter((item) => item.equipment.name !== name)
      : state.cart.map((item) =>
          item.equipment.name === name ? { ...item, quantity } : item
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
);
