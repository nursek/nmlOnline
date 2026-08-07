import { Injectable, computed, inject, signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import { PageResult, Player, Board } from '../models';
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
  private readonly _advancingTurn = signal(false);
  private readonly _currentTurn = signal<number | null>(null);

  readonly importing = this._importing.asReadonly();
  readonly error = this._error.asReadonly();
  readonly successMessage = this._successMessage.asReadonly();
  readonly advancingTurn = this._advancingTurn.asReadonly();
  readonly currentTurn = this._currentTurn.asReadonly();

  reloadPlayers(): void {
    this.playersRef.reload();
  }

  /** Charge le tour courant depuis la source unique de vérité (TurnService). */
  async loadCurrentTurn(): Promise<void> {
    try {
      const res = await firstValueFrom(this.api.adminGetCurrentTurn());
      this._currentTurn.set(res.currentTurn);
    } catch (error) {
      this._error.set(httpErrorMessage(error, 'Erreur lors de la récupération du tour courant'));
    }
  }

  /** Termine le tour courant : résout les mouvements PENDING puis incrémente. */
  async advanceTurn(): Promise<void> {
    this._advancingTurn.set(true);
    this._error.set(null);
    this._successMessage.set(null);
    try {
      const res = await firstValueFrom(this.api.adminAdvanceTurn());
      this._currentTurn.set(res.currentTurn);
      this._successMessage.set(`Tour ${res.currentTurn} en cours — mouvements résolus.`);
      this.reloadPlayers();
    } catch (error) {
      this._error.set(httpErrorMessage(error, 'Erreur lors du passage au tour suivant'));
    } finally {
      this._advancingTurn.set(false);
    }
  }

  /** Import a player JSON file; reloads the catalog on success. */
  async importPlayer(file: File, password?: string): Promise<Player> {
    this._importing.set(true);
    this._error.set(null);
    this._successMessage.set(null);
    try {
      const player = await firstValueFrom(this.api.adminImportPlayer(file, password));
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

  /**
   * Importe le Board depuis un board.json. Si mapImage + svgOverlay sont fournis,
   * ils sont uploadés d'abord (POST /boards/assets) et les URLs renvoyées sont
   * passées à l'import. Réutilise le signal `_importing` commun au header.
   */
  async importBoard(file: File, mapImage?: File, svgOverlay?: File): Promise<Board> {
    this._importing.set(true);
    this._error.set(null);
    this._successMessage.set(null);
    try {
      let mapImageUrl: string | undefined;
      let svgOverlayUrl: string | undefined;
      if (mapImage && svgOverlay) {
        const assets = await firstValueFrom(this.api.adminUploadBoardAssets(mapImage, svgOverlay));
        mapImageUrl = assets.mapImageUrl;
        svgOverlayUrl = assets.svgOverlayUrl;
      }
      const board = await firstValueFrom(
        this.api.adminImportBoard(file, mapImageUrl, svgOverlayUrl),
      );
      this._successMessage.set(
        `Board "${board.name}" importé (${Object.keys(board.sectors ?? {}).length} secteurs)`,
      );
      return board;
    } catch (error) {
      const message = httpErrorMessage(error, "Erreur lors de l'import du board");
      this._error.set(message);
      throw new Error(message);
    } finally {
      this._importing.set(false);
    }
  }

  clearMessages(): void {
    this._error.set(null);
    this._successMessage.set(null);
  }
}
