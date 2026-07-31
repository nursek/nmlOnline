import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import { MovementOrder } from '../models';
import { httpErrorMessage } from '../core/http-error.interceptor';

/**
 * État partagé des ordres de déplacement du joueur courant (PENDING du tour
 * en cours). Pure signals — pas d'état global dans les composants (AGENTS.md).
 */
@Injectable({ providedIn: 'root' })
export class MovementStateService {
  private readonly api = inject(ApiService);

  private readonly _orders = signal<MovementOrder[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly orders = this._orders.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  /** Ordres PENDING concernés par une unité donnée. */
  pendingForUnit(unitId: number): MovementOrder[] {
    return this._orders().filter((o) => o.entityIds?.includes(unitId));
  }

  /** Charge les ordres en attente du tour courant depuis TurnService (via le backend). */
  async loadOrders(): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const orders = await firstValueFrom(this.api.getPlayerMovementOrders());
      this._orders.set(orders ?? []);
    } catch (error) {
      this._error.set(
        httpErrorMessage(error, 'Erreur lors de la récupération des ordres de déplacement'),
      );
    } finally {
      this._loading.set(false);
    }
  }

  /** Crée un ordre de déplacement à pied pour une unité ; recharge les ordres après succès. */
  async placeFootOrder(unitId: number, route: number[]): Promise<MovementOrder | null> {
    this._error.set(null);
    try {
      const order = await firstValueFrom(this.api.placeFootOrder([unitId], route));
      await this.loadOrders();
      return order;
    } catch (error) {
      this._error.set(
        httpErrorMessage(error, "Erreur lors de la création de l'ordre de déplacement"),
      );
      return null;
    }
  }

  /** Annule un ordre ; recharge les ordres après succès. */
  async cancelOrder(orderId: number): Promise<boolean> {
    this._error.set(null);
    try {
      await firstValueFrom(this.api.cancelMovementOrder(orderId));
      await this.loadOrders();
      return true;
    } catch (error) {
      this._error.set(httpErrorMessage(error, "Erreur lors de l'annulation de l'ordre"));
      return false;
    }
  }

  clear(): void {
    this._orders.set([]);
    this._error.set(null);
  }
}
