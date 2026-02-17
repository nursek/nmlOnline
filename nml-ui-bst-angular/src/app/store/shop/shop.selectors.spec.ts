import {
  selectShopState,
  selectEquipments,
  selectCart,
  selectCartTotalItems,
  selectCartTotalPrice,
  selectShopLoading,
  selectShopError,
} from './shop.selectors';
import { ShopState } from './shop.reducer';
import { Equipment, CartItem } from '../../models';

describe('Shop Selectors', () => {
  const sword: Equipment = {
    id: 1,
    name: 'Épée en fer',
    cost: 100,
    category: 'ARME',
    pdfBonus: 5,
    pdcBonus: 0,
    armBonus: 0,
    evasionBonus: 0,
    compatibleClasses: ['GUERRIER'],
  } as Equipment;

  const shield: Equipment = {
    id: 2,
    name: 'Bouclier en bois',
    cost: 50,
    category: 'BOUCLIER',
    pdfBonus: 0,
    pdcBonus: 0,
    armBonus: 3,
    evasionBonus: 0,
    compatibleClasses: ['GUERRIER'],
  } as Equipment;

  const stateWithCart: { shop: ShopState } = {
    shop: {
      equipments: [sword, shield],
      cart: [
        { equipment: sword, quantity: 2 },
        { equipment: shield, quantity: 3 },
      ],
      loading: false,
      error: null,
    },
  };

  const emptyState: { shop: ShopState } = {
    shop: {
      equipments: [],
      cart: [],
      loading: false,
      error: null,
    },
  };

  const loadingState: { shop: ShopState } = {
    shop: {
      equipments: [],
      cart: [],
      loading: true,
      error: null,
    },
  };

  const errorState: { shop: ShopState } = {
    shop: {
      equipments: [],
      cart: [],
      loading: false,
      error: 'Network error',
    },
  };

  it('should select shop state', () => {
    expect(selectShopState(stateWithCart as any)).toEqual(stateWithCart.shop);
  });

  it('should select equipments', () => {
    expect(selectEquipments(stateWithCart as any)).toEqual([sword, shield]);
    expect(selectEquipments(emptyState as any)).toEqual([]);
  });

  it('should select cart', () => {
    expect(selectCart(stateWithCart as any)).toHaveLength(2);
    expect(selectCart(emptyState as any)).toEqual([]);
  });

  it('should compute total items in cart', () => {
    expect(selectCartTotalItems(stateWithCart as any)).toBe(5); // 2 + 3
    expect(selectCartTotalItems(emptyState as any)).toBe(0);
  });

  it('should compute total price of cart', () => {
    // sword: 100 * 2 = 200, shield: 50 * 3 = 150 => total = 350
    expect(selectCartTotalPrice(stateWithCart as any)).toBe(350);
    expect(selectCartTotalPrice(emptyState as any)).toBe(0);
  });

  it('should select loading', () => {
    expect(selectShopLoading(loadingState as any)).toBe(true);
    expect(selectShopLoading(stateWithCart as any)).toBe(false);
  });

  it('should select error', () => {
    expect(selectShopError(errorState as any)).toBe('Network error');
    expect(selectShopError(stateWithCart as any)).toBeNull();
  });
});
