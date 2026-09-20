import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import { PlayerService } from './player.service';
import { PlayerAction, HarvestChoice } from '../models';
import { httpErrorMessage } from '../core/http-error.interceptor';

@Injectable({ providedIn: 'root' })
export class PlayerActionsService {
  private readonly api = inject(ApiService);
  private readonly playerService = inject(PlayerService);

  private readonly _actions = signal<PlayerAction[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly actions = this._actions.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly hasActions = computed(() => this._actions().length > 0);

  async loadActions(): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      this._actions.set(await firstValueFrom(this.api.getPlayerActions()));
    } catch (error) {
      this._error.set(httpErrorMessage(error, "Erreur lors du chargement des actions"));
    } finally {
      this._loading.set(false);
    }
  }

  async undoFrom(actionId: number): Promise<boolean> {
    return this.runMutation(
      () => this.api.undoPlayerActions(actionId),
      "Erreur lors de l'annulation de l'action",
    );
  }

  async undoAll(): Promise<boolean> {
    return this.runMutation(
      () => this.api.undoAllPlayerActions(),
      "Erreur lors de l'annulation de l'action",
    );
  }

  async harvest(choice: HarvestChoice, sectorNumbers: number[]): Promise<boolean> {
    return this.runMutation(
      () => this.api.harvest(choice, sectorNumbers),
      'Erreur lors de la récolte',
    );
  }

  private async runMutation(
    request: () => ReturnType<ApiService['getPlayerActions']>,
    errorMessage: string,
  ): Promise<boolean> {
    this._error.set(null);
    this._loading.set(true);
    try {
      this._actions.set(await firstValueFrom(request()));
      void this.playerService.loadCurrent();
      void this.playerService.loadVehicles();
      return true;
    } catch (error) {
      this._error.set(httpErrorMessage(error, errorMessage));
      return false;
    } finally {
      this._loading.set(false);
    }
  }
}
