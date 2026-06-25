import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import { AuthService } from './auth.service';
import { Player, Vehicle } from '../models';
import { httpErrorMessage } from '../core/http-error.interceptor';

/**
 * Reactive state of the currently logged-in player: profile and vehicles.
 * Pure signals + service — no NgRx.
 */
@Injectable({ providedIn: 'root' })
export class PlayerService {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);

  private readonly _player = signal<Player | null>(null);
  private readonly _vehicles = signal<Vehicle[]>([]);
  private readonly _loading = signal(false);
  private readonly _vehiclesLoading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly player = this._player.asReadonly();
  readonly vehicles = this._vehicles.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly vehiclesLoading = this._vehiclesLoading.asReadonly();
  readonly error = this._error.asReadonly();

  readonly undeployedVehicles = computed(() =>
    this._vehicles().filter((v) => v.sectorNumber === null),
  );

  /** Load the profile of the currently logged-in user (by username). */
  async loadCurrent(): Promise<void> {
    const username = this.auth.user()?.username;
    if (!username) return;
    this._loading.set(true);
    this._error.set(null);
    try {
      const player = await firstValueFrom(this.api.getPlayer(username));
      this._player.set(player);
    } catch (error) {
      this._error.set(this.messageFor(error, 'Erreur lors de la récupération du joueur'));
    } finally {
      this._loading.set(false);
    }
  }

  /** Load vehicles owned by the current player. */
  async loadVehicles(): Promise<void> {
    this._vehiclesLoading.set(true);
    this._error.set(null);
    try {
      const vehicles = await firstValueFrom(this.api.getPlayerVehicles());
      this._vehicles.set(vehicles);
    } catch (error) {
      this._error.set(this.messageFor(error, 'Erreur lors de la récupération des véhicules'));
    } finally {
      this._vehiclesLoading.set(false);
    }
  }

  /** Place a vehicle on a board sector; reloads vehicles + player on success. */
  async placeVehicle(
    vehicleId: number,
    boardId: number,
    sectorNumber: number,
  ): Promise<Vehicle | null> {
    this._vehiclesLoading.set(true);
    this._error.set(null);
    try {
      const vehicle = await firstValueFrom(this.api.placeVehicle(vehicleId, boardId, sectorNumber));
      this._vehicles.update((list) => list.map((v) => (v.id === vehicle.id ? vehicle : v)));
      void this.loadCurrent();
      return vehicle;
    } catch (error) {
      this._error.set(this.messageFor(error, 'Erreur lors du déploiement du véhicule'));
      return null;
    } finally {
      this._vehiclesLoading.set(false);
    }
  }

  private messageFor(error: unknown, fallback: string): string {
    if ((error as { status?: number })?.status === 404) {
      const username = this.auth.user()?.username ?? '';
      return `Aucun profil de joueur trouvé pour "${username}". Créez un joueur avec ce nom dans le jeu.`;
    }
    return httpErrorMessage(error, fallback);
  }
}
