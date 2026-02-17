import { shopReducer, initialState, ShopState } from './shop.reducer';
import { ShopActions } from './shop.actions';
import { Equipment, CartItem } from '../../models';

describe('ShopReducer', () => {
  const mockEquipment: Equipment = {
    name: 'Épée en fer',
    cost: 100,
    category: 'Arme',
    pdfBonus: 5,
    pdcBonus: 0,
    armBonus: 0,
    evasionBonus: 0,
    compatibleClasses: ['Guerrier'],
  };

  const mockEquipment2: Equipment = {
    name: 'Bouclier en bois',
    cost: 50,
    category: 'Bouclier',
    pdfBonus: 0,
    pdcBonus: 3,
    armBonus: 2,
    evasionBonus: 0,
    compatibleClasses: ['Guerrier', 'Défenseur'],
  };

  describe('initial state', () => {
    it('should return the initial state', () => {
      const state = shopReducer(undefined, { type: 'NOOP' } as any);
      expect(state).toEqual(initialState);
    });

    it('should start with empty cart and equipments', () => {
      expect(initialState.cart).toEqual([]);
      expect(initialState.equipments).toEqual([]);
      expect(initialState.loading).toBe(false);
      expect(initialState.error).toBeNull();
    });
  });

  describe('Fetch Equipments', () => {
    it('should set loading on fetchEquipments', () => {
      const state = shopReducer(initialState, ShopActions.fetchEquipments());
      expect(state.loading).toBe(true);
      expect(state.error).toBeNull();
    });

    it('should set equipments on fetchEquipmentsSuccess', () => {
      const equipments = [mockEquipment, mockEquipment2];
      const state = shopReducer(
        { ...initialState, loading: true },
        ShopActions.fetchEquipmentsSuccess({ equipments })
      );
      expect(state.loading).toBe(false);
      expect(state.equipments).toEqual(equipments);
    });

    it('should set error on fetchEquipmentsFailure', () => {
      const state = shopReducer(
        { ...initialState, loading: true },
        ShopActions.fetchEquipmentsFailure({ error: 'Network error' })
      );
      expect(state.loading).toBe(false);
      expect(state.error).toBe('Network error');
    });
  });

  describe('Load Cart', () => {
    it('should load cart from action', () => {
      const cart: CartItem[] = [{ equipment: mockEquipment, quantity: 2 }];
      const state = shopReducer(
        initialState,
        ShopActions.loadCartSuccess({ cart })
      );
      expect(state.cart).toEqual(cart);
    });
  });

  describe('Cart Operations', () => {
    it('should add a new item to cart', () => {
      const state = shopReducer(
        initialState,
        ShopActions.addToCart({ equipment: mockEquipment })
      );
      expect(state.cart).toHaveLength(1);
      expect(state.cart[0].equipment.name).toBe('Épée en fer');
      expect(state.cart[0].quantity).toBe(1);
    });

    it('should increment quantity for existing item', () => {
      const stateWithItem: ShopState = {
        ...initialState,
        cart: [{ equipment: mockEquipment, quantity: 1 }],
      };

      const state = shopReducer(
        stateWithItem,
        ShopActions.addToCart({ equipment: mockEquipment })
      );
      expect(state.cart).toHaveLength(1);
      expect(state.cart[0].quantity).toBe(2);
    });

    it('should remove an item from cart', () => {
      const stateWithItems: ShopState = {
        ...initialState,
        cart: [
          { equipment: mockEquipment, quantity: 2 },
          { equipment: mockEquipment2, quantity: 1 },
        ],
      };

      const state = shopReducer(
        stateWithItems,
        ShopActions.removeFromCart({ name: 'Épée en fer' })
      );
      expect(state.cart).toHaveLength(1);
      expect(state.cart[0].equipment.name).toBe('Bouclier en bois');
    });

    it('should update cart item quantity', () => {
      const stateWithItem: ShopState = {
        ...initialState,
        cart: [{ equipment: mockEquipment, quantity: 1 }],
      };

      const state = shopReducer(
        stateWithItem,
        ShopActions.updateCartItemQuantity({ name: 'Épée en fer', quantity: 5 })
      );
      expect(state.cart[0].quantity).toBe(5);
    });

    it('should remove item when quantity set to 0', () => {
      const stateWithItem: ShopState = {
        ...initialState,
        cart: [{ equipment: mockEquipment, quantity: 3 }],
      };

      const state = shopReducer(
        stateWithItem,
        ShopActions.updateCartItemQuantity({ name: 'Épée en fer', quantity: 0 })
      );
      expect(state.cart).toHaveLength(0);
    });

    it('should clear the entire cart', () => {
      const stateWithItems: ShopState = {
        ...initialState,
        cart: [
          { equipment: mockEquipment, quantity: 2 },
          { equipment: mockEquipment2, quantity: 1 },
        ],
      };

      const state = shopReducer(stateWithItems, ShopActions.clearCart());
      expect(state.cart).toEqual([]);
    });
  });

  describe('Clear Shop Error', () => {
    it('should clear the shop error', () => {
      const errorState: ShopState = {
        ...initialState,
        error: 'Something went wrong',
      };

      const state = shopReducer(errorState, ShopActions.clearShopError());
      expect(state.error).toBeNull();
    });
  });

  describe('Reducer purity', () => {
    it('should not modify the original state on addToCart', () => {
      const original = { ...initialState, cart: [] as CartItem[] };
      shopReducer(original, ShopActions.addToCart({ equipment: mockEquipment }));
      expect(original.cart).toEqual([]);
    });

    it('should not modify the original state on clearCart', () => {
      const original: ShopState = {
        ...initialState,
        cart: [{ equipment: mockEquipment, quantity: 1 }],
      };
      const originalCart = original.cart;
      shopReducer(original, ShopActions.clearCart());
      expect(original.cart).toBe(originalCart);
    });
  });
});
