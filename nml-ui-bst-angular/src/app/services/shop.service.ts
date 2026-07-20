import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import { CartStorageService } from './cart-storage.service';
import { PlayerService } from './player.service';
import { AuthService } from './auth.service';
import { Equipment, PageResult, PlayerResource, Vehicle, VehicleTypeInfo } from '../models';
import { CartItem, SellCartItem, VehicleCartItem } from '../models/shop.model';
import { httpErrorMessage } from '../core/http-error.interceptor';
import { environment } from '../../environments/environment';

/**
 * Normalise a quantity: returns an integer >= `min` (default 1).
 * Protects against NaN, Infinity, negatives and decimals.
 * Pass `min=0` for update operations where 0 means "remove".
 */
function safeQty(qty: number, min = 1): number {
  const n = Math.trunc(qty);
  return Number.isNaN(n) || !Number.isFinite(n) ? min : Math.max(n, min);
}

/**
 * Shop state: equipment catalog & cart for equipment, vehicles and resource-sales.
 * Pure signals + services — no NgRx.
 *
 * Read-only catalogs (`equipments`, `vehicleTypes`) are exposed via `httpResource`
 * for reactive GETs tied to signals; carts are persisted to `CartStorageService`
 * through an effect.
 */
@Injectable({ providedIn: 'root' })
export class ShopService {
  private readonly api = inject(ApiService);
  private readonly cartStorage = inject(CartStorageService);
  private readonly player = inject(PlayerService);
  private readonly auth = inject(AuthService);

  // --- Read-only catalogs via httpResource (gated on authentication) ---
  // Returning `undefined` as the URL defers the request until the user is
  // authenticated, avoiding the 401 storm on app bootstrap.
  private readonly equipmentsRef = httpResource<PageResult<Equipment>>(() =>
    this.auth.isAuthenticated()
      ? { url: `${environment.apiBaseUrl}/equipment`, params: { page: '0', size: '100' } }
      : undefined,
  );
  private readonly vehicleTypesRef = httpResource<VehicleTypeInfo[]>(() =>
    this.auth.isAuthenticated() ? { url: `${environment.apiBaseUrl}/vehicles/types` } : undefined,
  );

  readonly equipments = computed(() => this.equipmentsRef.value()?.content ?? []);
  readonly equipmentsLoading = computed(
    () => this._error() === null && this.equipmentsRef.isLoading(),
  );
  readonly vehicleTypes = computed(() => this.vehicleTypesRef.value() ?? []);
  readonly vehicleTypesLoading = computed(
    () => this._error() === null && this.vehicleTypesRef.isLoading(),
  );

  // --- Carts (signal state, persisted via effect) ------------------------
  private readonly _cart = signal<CartItem[]>(this.cartStorage.loadCart());
  private readonly _vehicleCart = signal<VehicleCartItem[]>(this.cartStorage.loadVehicleCart());
  private readonly _sellCart = signal<SellCartItem[]>([]);
  private readonly _purchaseLoading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly cart = this._cart.asReadonly();
  readonly vehicleCart = this._vehicleCart.asReadonly();
  readonly sellCart = this._sellCart.asReadonly();
  readonly purchaseLoading = this._purchaseLoading.asReadonly();
  readonly error = this._error.asReadonly();

  readonly cartTotalItems = computed(() =>
    this._cart().reduce((sum, item) => sum + item.quantity, 0),
  );
  readonly cartTotalPrice = computed(() =>
    this._cart().reduce((sum, item) => sum + item.equipment.cost * item.quantity, 0),
  );
  readonly vehicleCartTotalItems = computed(() =>
    this._vehicleCart().reduce((sum, item) => sum + item.quantity, 0),
  );
  readonly vehicleCartTotalPrice = computed(() =>
    this._vehicleCart().reduce((sum, item) => sum + item.vehicleType.cost * item.quantity, 0),
  );
  readonly sellCartTotalValue = computed(() =>
    this._sellCart().reduce((sum, item) => sum + (item.resource.baseValue ?? 0) * item.quantity, 0),
  );

  constructor() {
    // Persist cart & vehicle cart only — DOM side-effect via storage service.
    effect(() => {
      this.cartStorage.saveCart(this._cart());
    });
    effect(() => {
      this.cartStorage.saveVehicleCart(this._vehicleCart());
    });
  }

  // --- Equipment cart mutators ------------------------------------------
  addToCart(equipment: Equipment): void {
    this._cart.update((cart) => {
      const idx = cart.findIndex((i) => i.equipment.name === equipment.name);
      if (idx >= 0) {
        return cart.map((item, i) => (i === idx ? { ...item, quantity: item.quantity + 1 } : item));
      }
      return [...cart, { equipment, quantity: 1 }];
    });
  }

  removeFromCart(name: string): void {
    this._cart.update((cart) => cart.filter((i) => i.equipment.name !== name));
  }

  updateCartItemQuantity(name: string, quantity: number): void {
    const qty = safeQty(quantity, 0);
    this._cart.update((cart) =>
      qty <= 0
        ? cart.filter((i) => i.equipment.name !== name)
        : cart.map((i) => (i.equipment.name === name ? { ...i, quantity: qty } : i)),
    );
  }

  clearCart(): void {
    this._cart.set([]);
  }

  // --- Vehicle cart mutators --------------------------------------------
  addVehicleToCart(vehicleType: VehicleTypeInfo, quantity: number): void {
    const qty = safeQty(quantity);
    this._vehicleCart.update((cart) => {
      const idx = cart.findIndex((i) => i.vehicleType.name === vehicleType.name);
      if (idx >= 0) {
        return cart.map((item, i) =>
          i === idx ? { ...item, quantity: item.quantity + qty } : item,
        );
      }
      return [...cart, { vehicleType, quantity: qty }];
    });
  }

  removeVehicleFromCart(name: string): void {
    this._vehicleCart.update((cart) => cart.filter((i) => i.vehicleType.name !== name));
  }

  updateVehicleCartItemQuantity(name: string, quantity: number): void {
    const qty = safeQty(quantity, 0);
    this._vehicleCart.update((cart) =>
      qty <= 0
        ? cart.filter((i) => i.vehicleType.name !== name)
        : cart.map((i) => (i.vehicleType.name === name ? { ...i, quantity: qty } : i)),
    );
  }

  clearVehicleCart(): void {
    this._vehicleCart.set([]);
  }

  // --- Sell cart mutators -----------------------------------------------
  addToSellCart(resource: PlayerResource, quantity: number): void {
    const qty = safeQty(quantity);
    this._sellCart.update((cart) => {
      const idx = cart.findIndex((i) => i.resource.id === resource.id);
      if (idx >= 0) {
        return cart.map((item, i) =>
          i === idx
            ? { ...item, quantity: Math.min(item.quantity + qty, item.resource.quantity) }
            : item,
        );
      }
      return [...cart, { resource, quantity: Math.min(qty, resource.quantity) }];
    });
  }

  removeFromSellCart(resourceId: number): void {
    this._sellCart.update((cart) => cart.filter((i) => i.resource.id !== resourceId));
  }

  updateSellCartItemQuantity(resourceId: number, quantity: number): void {
    const qty = safeQty(quantity, 0);
    this._sellCart.update((cart) =>
      qty <= 0
        ? cart.filter((i) => i.resource.id !== resourceId)
        : cart.map((i) =>
            i.resource.id === resourceId
              ? { ...i, quantity: Math.min(qty, i.resource.quantity) }
              : i,
          ),
    );
  }

  clearSellCart(): void {
    this._sellCart.set([]);
  }

  // --- Checkouts (mutations through HttpClient) -------------------------
  async checkoutVehicles(): Promise<Vehicle[]> {
    this._purchaseLoading.set(true);
    this._error.set(null);
    try {
      const items = this._vehicleCart().map((i) => ({
        vehicleType: i.vehicleType.name,
        quantity: i.quantity,
      }));
      const vehicles = await firstValueFrom(this.api.buyVehiclesBatch(items));
      this.clearVehicleCart();
      void this.player.loadCurrent();
      void this.player.loadVehicles();
      return vehicles;
    } catch (error) {
      const message = this.purchaseError(
        error,
        402,
        'Fonds insuffisants pour acheter ces véhicules',
        "Erreur lors de l'achat des véhicules",
      );
      this._error.set(message);
      throw new Error(message);
    } finally {
      this._purchaseLoading.set(false);
    }
  }

  async checkoutEquipments(): Promise<void> {
    this._purchaseLoading.set(true);
    this._error.set(null);
    try {
      const items = this._cart().map((item) => ({
        name: item.equipment.name,
        quantity: item.quantity,
      }));
      await firstValueFrom(this.api.buyEquipments(items));
      this.clearCart();
      void this.player.loadCurrent();
    } catch (error) {
      const message = this.purchaseError(
        error,
        402,
        'Fonds insuffisants pour finaliser la commande',
        "Erreur lors de l'achat des équipements",
      );
      this._error.set(message);
      throw new Error(message);
    } finally {
      this._purchaseLoading.set(false);
    }
  }

  async checkoutSellCart(): Promise<number> {
    this._purchaseLoading.set(true);
    this._error.set(null);
    try {
      const items = this._sellCart().map((item) => ({
        playerResourceId: item.resource.id!,
        quantity: item.quantity,
      }));
      const response = await firstValueFrom(this.api.sellResourcesBatch(items));
      this.clearSellCart();
      void this.player.loadCurrent();
      return response.totalValue;
    } catch (error) {
      const message = this.purchaseError(error, 0, '', 'Erreur lors de la vente des ressources');
      this._error.set(message);
      throw new Error(message);
    } finally {
      this._purchaseLoading.set(false);
    }
  }

  private purchaseError(
    error: unknown,
    expectedStatus: number,
    expectedMessage: string,
    fallback: string,
  ): string {
    if (expectedStatus && (error as { status?: number })?.status === expectedStatus) {
      return expectedMessage || fallback;
    }
    return httpErrorMessage(error, fallback);
  }
}
