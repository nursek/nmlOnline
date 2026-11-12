import { Injectable, effect, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import { AuthService } from './auth.service';
import { SniperOrder } from '../models';
import { httpErrorMessage } from '../core/http-error.interceptor';

/** Ordres sniper PENDING du joueur courant (même cycle de vie que MovementStateService). */
@Injectable({ providedIn: 'root' })
export class SniperOrderStateService {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);

  private readonly _orders = signal<SniperOrder[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly orders = this._orders.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  constructor() {
    effect(() => {
      if (this.auth.initialized() && !this.auth.user()) {
        this.clear();
      }
    });
  }

  async loadOrders(): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      this._orders.set((await firstValueFrom(this.api.getPlayerSniperOrders())) ?? []);
    } catch (error) {
      this._error.set(httpErrorMessage(error, 'Erreur lors de la récupération des ordres sniper'));
    } finally {
      this._loading.set(false);
    }
  }

  async placeOrder(unitId: number, targetSectorNumber: number): Promise<SniperOrder | null> {
    this._error.set(null);
    try {
      const order = await firstValueFrom(this.api.placeSniperOrder(unitId, targetSectorNumber));
      await this.loadOrders();
      return order;
    } catch (error) {
      this._error.set(httpErrorMessage(error, "Erreur lors de la création de l'ordre sniper"));
      return null;
    }
  }

  async cancelOrder(orderId: number): Promise<boolean> {
    this._error.set(null);
    try {
      await firstValueFrom(this.api.cancelSniperOrder(orderId));
      await this.loadOrders();
      return true;
    } catch (error) {
      this._error.set(httpErrorMessage(error, "Erreur lors de l'annulation de l'ordre sniper"));
      return false;
    }
  }

  clear(): void {
    this._orders.set([]);
    this._error.set(null);
  }
}
