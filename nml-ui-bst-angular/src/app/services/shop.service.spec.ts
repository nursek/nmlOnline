import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ShopService } from './shop.service';
import { CartStorageService } from './cart-storage.service';
import { PlayerService } from './player.service';
import { AuthService } from './auth.service';

function flushSessionStorage(): void {
  sessionStorage.clear();
}

describe('ShopService (carts + signals)', () => {
  beforeEach(() => {
    flushSessionStorage();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ShopService,
        CartStorageService,
        {
          provide: PlayerService,
          useValue: { loadCurrent: jest.fn(), loadVehicles: jest.fn() },
        },
        {
          provide: AuthService,
          useValue: { user: () => ({ id: 1, username: 'tester' }) },
        },
      ],
    });
  });

  function getShop(): ShopService {
    return TestBed.inject(ShopService);
  }

  it('starts with empty carts and zero totals', () => {
    const shop = getShop();
    expect(shop.cart()).toEqual([]);
    expect(shop.vehicleCart()).toEqual([]);
    expect(shop.sellCart()).toEqual([]);
    expect(shop.cartTotalItems()).toBe(0);
    expect(shop.cartTotalPrice()).toBe(0);
    expect(shop.vehicleCartTotalItems()).toBe(0);
    expect(shop.vehicleCartTotalPrice()).toBe(0);
  });

  it('adds a new equipment to the cart', () => {
    const shop = getShop();
    shop.addToCart({
      name: 'Épée',
      cost: 100,
      category: 'Arme',
      pdfBonus: 5,
      pdcBonus: 0,
      armBonus: 0,
      evasionBonus: 0,
      compatibleClass: [],
    });
    expect(shop.cart()).toHaveLength(1);
    expect(shop.cart()[0].quantity).toBe(1);
    expect(shop.cartTotalItems()).toBe(1);
    expect(shop.cartTotalPrice()).toBe(100);
  });

  it('increments quantity when the same equipment is added twice', () => {
    const shop = getShop();
    const sword = {
      name: 'Épée',
      cost: 100,
      category: 'Arme',
      pdfBonus: 5,
      pdcBonus: 0,
      armBonus: 0,
      evasionBonus: 0,
      compatibleClass: [],
    };
    shop.addToCart(sword);
    shop.addToCart(sword);
    expect(shop.cart()).toHaveLength(1);
    expect(shop.cart()[0].quantity).toBe(2);
    expect(shop.cartTotalItems()).toBe(2);
    expect(shop.cartTotalPrice()).toBe(200);
  });

  it('removes an item from the cart', () => {
    const shop = getShop();
    const sword = {
      name: 'Épée',
      cost: 100,
      category: 'Arme',
      pdfBonus: 5,
      pdcBonus: 0,
      armBonus: 0,
      evasionBonus: 0,
      compatibleClass: [],
    };
    shop.addToCart(sword);
    shop.addToCart(sword);
    shop.removeFromCart('Épée');
    expect(shop.cart()).toHaveLength(0);
    expect(shop.cartTotalItems()).toBe(0);
  });

  it('updates a cart item quantity (0 removes the item)', () => {
    const shop = getShop();
    const sword = {
      name: 'Épée',
      cost: 100,
      category: 'Arme',
      pdfBonus: 5,
      pdcBonus: 0,
      armBonus: 0,
      evasionBonus: 0,
      compatibleClass: [],
    };
    shop.addToCart(sword);
    shop.updateCartItemQuantity('Épée', 5);
    expect(shop.cart()[0].quantity).toBe(5);
    shop.updateCartItemQuantity('Épée', 0);
    expect(shop.cart()).toHaveLength(0);
  });

  it('clears the entire equipment cart', () => {
    const shop = getShop();
    shop.addToCart({
      name: 'A',
      cost: 10,
      category: 'X',
      pdfBonus: 0,
      pdcBonus: 0,
      armBonus: 0,
      evasionBonus: 0,
      compatibleClass: [],
    });
    shop.addToCart({
      name: 'B',
      cost: 20,
      category: 'X',
      pdfBonus: 0,
      pdcBonus: 0,
      armBonus: 0,
      evasionBonus: 0,
      compatibleClass: [],
    });
    shop.clearCart();
    expect(shop.cart()).toEqual([]);
  });

  it('adds vehicle to cart with explicit quantity', () => {
    const shop = getShop();
    const vt = {
      name: 'Tank',
      displayName: 'Char',
      cost: 50,
      basePdf: 0,
      baseDefense: 0,
      speed: 1,
      capacity: 4,
      resistance: 0,
      firesInTransit: false,
      aerial: false,
    };
    shop.addVehicleToCart(vt, 3);
    expect(shop.vehicleCart()).toHaveLength(1);
    expect(shop.vehicleCart()[0].quantity).toBe(3);
    expect(shop.vehicleCartTotalItems()).toBe(3);
    expect(shop.vehicleCartTotalPrice()).toBe(150);
  });

  it('adds to vehicle cart quantity when re-adding the same type', () => {
    const shop = getShop();
    const vt = {
      name: 'Tank',
      displayName: 'Char',
      cost: 50,
      basePdf: 0,
      baseDefense: 0,
      speed: 1,
      capacity: 4,
      resistance: 0,
      firesInTransit: false,
      aerial: false,
    };
    shop.addVehicleToCart(vt, 1);
    shop.addVehicleToCart(vt, 2);
    expect(shop.vehicleCart()[0].quantity).toBe(3);
  });

  it('removes a vehicle from the cart by type name', () => {
    const shop = getShop();
    const vt = {
      name: 'Tank',
      displayName: 'Char',
      cost: 50,
      basePdf: 0,
      baseDefense: 0,
      speed: 1,
      capacity: 4,
      resistance: 0,
      firesInTransit: false,
      aerial: false,
    };
    shop.addVehicleToCart(vt, 1);
    shop.removeVehicleFromCart('Tank');
    expect(shop.vehicleCart()).toHaveLength(0);
  });

  it('clamps sell-cart quantity against the resource max', () => {
    const shop = getShop();
    const resource = { id: 7, name: 'Bois', quantity: 5, baseValue: 2 };
    shop.addToSellCart(resource, 3);
    expect(shop.sellCart()[0].quantity).toBe(3);
    // Re-adding more than available clamps to resource.quantity.
    shop.addToSellCart(resource, 100);
    expect(shop.sellCart()[0].quantity).toBe(5);
  });

  it('computes the total sale value of the sell cart (with multiplier)', () => {
    const shop = getShop();
    const resource = { id: 7, name: 'Bois', quantity: 5, baseValue: 2 };
    shop.addToSellCart(resource, 4);
    // 2 × saleMultiplier(4) = 2 × 9 = 18 (non-linéaire côté backend).
    expect(shop.sellCartTotalValue()).toBe(18);
  });

  it('removes a resource from the sell cart by id', () => {
    const shop = getShop();
    const resource = { id: 7, name: 'Bois', quantity: 5, baseValue: 2 };
    shop.addToSellCart(resource, 4);
    shop.removeFromSellCart(7);
    expect(shop.sellCart()).toHaveLength(0);
  });
});
