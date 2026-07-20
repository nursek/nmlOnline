import { Injectable, computed, inject, signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import { PageResult, Player } from '../models';
import { httpErrorMessage } from '../core/http-error.interceptor';
import { environment } from '../../environments/environment';

/**
 * Admin console state: paginated player list + import/delete operations.
 * Pure signals + services — no NgRx.
 */
@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly api = inject(ApiService);

  // Player catalog reactive GET; re-fetched on `reloadPlayers()`.
  private readonly playersRef = httpResource<PageResult<Player>>(() => ({
    url: `${environment.apiBaseUrl}/admin/players`,
    params: { page: '0', size: '50' },
  }));

  readonly players = computed(() => this.playersRef.value()?.content ?? []);
  readonly loading = computed(() => this.playersRef.isLoading());

  private readonly _importing = signal(false);
  private readonly _error = signal<string | null>(null);
  private readonly _successMessage = signal<string | null>(null);

  readonly importing = this._importing.asReadonly();
  readonly error = this._error.asReadonly();
  readonly successMessage = this._successMessage.asReadonly();

  reloadPlayers(): void {
    this.playersRef.reload();
  }

  /** Import a player JSON file; reloads the catalog on success. */
  async importPlayer(file: File): Promise<Player> {
    this._importing.set(true);
    this._error.set(null);
    this._successMessage.set(null);
    try {
      const player = await firstValueFrom(this.api.adminImportPlayer(file));
      this._successMessage.set(`Joueur "${player.name}" importé avec succès`);
      this.reloadPlayers();
      return player;
    } catch (error) {
      const message = httpErrorMessage(error, "Erreur lors de l'import");
      this._error.set(message);
      throw new Error(message);
    } finally {
      this._importing.set(false);
    }
  }

  /** Delete a player by id; updates the local cache optimistically. */
  async deletePlayer(playerId: number): Promise<void> {
    this._error.set(null);
    this._successMessage.set(null);
    try {
      await firstValueFrom(this.api.adminDeletePlayer(playerId));
      this._successMessage.set('Joueur supprimé avec succès');
      this.reloadPlayers();
    } catch (error) {
      const message = httpErrorMessage(error, 'Erreur lors de la suppression');
      this._error.set(message);
      throw new Error(message);
    }
  }

  clearMessages(): void {
    this._error.set(null);
    this._successMessage.set(null);
  }
}
