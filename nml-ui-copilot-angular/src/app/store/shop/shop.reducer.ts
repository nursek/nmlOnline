import { createReducer, on } from '@ngrx/store';
import { Equipment, CartItem } from '../../models';
import { ShopActions } from './shop.actions';

export interface ShopState {
  equipments: Equipment[];
  cart: CartItem[];
  loading: boolean;
  error: string | null;
}

// Charger le panier depuis localStorage
const storedCart = localStorage.getItem('cart');

export const initialState: ShopState = {
  equipments: [],
  cart: storedCart ? JSON.parse(storedCart) : [],
  loading: false,
  error: null,
};

function saveCart(cart: CartItem[]): void {
  localStorage.setItem('cart', JSON.stringify(cart));
}

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

  // Cart actions
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

    saveCart(newCart);
    return { ...state, cart: newCart };
  }),

  on(ShopActions.removeFromCart, (state, { name }) => {
    const newCart = state.cart.filter((item) => item.equipment.name !== name);
    saveCart(newCart);
    return { ...state, cart: newCart };
  }),

  on(ShopActions.updateCartItemQuantity, (state, { name, quantity }) => {
    let newCart: CartItem[];
    if (quantity <= 0) {
      newCart = state.cart.filter((item) => item.equipment.name !== name);
    } else {
      newCart = state.cart.map((item) =>
        item.equipment.name === name ? { ...item, quantity } : item
      );
    }
    saveCart(newCart);
    return { ...state, cart: newCart };
  }),

  on(ShopActions.clearCart, (state) => {
    localStorage.removeItem('cart');
    return { ...state, cart: [] };
  }),

  on(ShopActions.clearShopError, (state) => ({
    ...state,
    error: null,
  })),
);
