import { Injectable } from '@angular/core';
import { CartItem, VehicleCartItem } from '../models';

function isObject(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function isValidPricedItem(item: unknown): boolean {
  if (!isObject(item)) return false;
  const name = item['name'];
  const cost = item['cost'];
  return (
    typeof name === 'string' &&
    name.length > 0 &&
    typeof cost === 'number' &&
    Number.isFinite(cost) &&
    cost >= 0
  );
}

/**
 * Abstraction du stockage du panier.
 * Par défaut utilise sessionStorage pour ne pas persister le panier au-delà de la session.
 */
@Injectable({
  providedIn: 'root',
})
export class CartStorageService {
  private readonly CART_KEY = 'nml_cart';
  private readonly VEHICLE_CART_KEY = 'nml_vehicle_cart';

  loadCart(): CartItem[] {
    const parsed = this.parse<unknown[]>(sessionStorage.getItem(this.CART_KEY)) ?? [];
    return parsed.filter((item): item is CartItem => this.isValidCartItem(item));
  }

  saveCart(cart: CartItem[]): void {
    this.setOrRemove(this.CART_KEY, cart);
  }

  loadVehicleCart(): VehicleCartItem[] {
    const parsed = this.parse<unknown[]>(sessionStorage.getItem(this.VEHICLE_CART_KEY)) ?? [];
    return parsed.filter((item): item is VehicleCartItem => this.isValidVehicleCartItem(item));
  }

  saveVehicleCart(cart: VehicleCartItem[]): void {
    this.setOrRemove(this.VEHICLE_CART_KEY, cart);
  }

  private parse<T>(raw: string | null): T | null {
    if (!raw) return null;
    try {
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? (parsed as T) : null;
    } catch {
      return null;
    }
  }

  private isValidCartItem(item: unknown): item is CartItem {
    if (!isObject(item)) return false;
    const quantity = item['quantity'];
    return (
      isValidPricedItem(item['equipment']) &&
      typeof quantity === 'number' &&
      Number.isInteger(quantity) &&
      quantity > 0
    );
  }

  private isValidVehicleCartItem(item: unknown): item is VehicleCartItem {
    if (!isObject(item)) return false;
    const quantity = item['quantity'];
    return (
      isValidPricedItem(item['vehicleType']) &&
      typeof quantity === 'number' &&
      Number.isInteger(quantity) &&
      quantity > 0
    );
  }

  private setOrRemove<T>(key: string, value: T[]): void {
    if (value.length > 0) {
      sessionStorage.setItem(key, JSON.stringify(value));
    } else {
      sessionStorage.removeItem(key);
    }
  }
}
