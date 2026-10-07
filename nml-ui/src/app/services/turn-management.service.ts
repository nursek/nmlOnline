import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import { ResolvedBattle, TurnFinalizeResult, TurnResolutionState } from '../models';
import { httpErrorMessage } from '../core/http-error.interceptor';

/**
 * État de la page « Gestion du tour » : tour courant (navigation dev incluse) et
 * session de résolution pas-à-pas par hop. En prod, `navigateToTurn` renvoie 404
 * (endpoint `@Profile("dev")`).
 */
@Injectable({ providedIn: 'root' })
export class TurnManagementService {
  private readonly api = inject(ApiService);

  private readonly _state = signal<TurnResolutionState | null>(null);
  private readonly _loading = signal(false);
  private readonly _busy = signal(false);
  private readonly _error = signal<string | null>(null);
  private readonly _lastReport = signal<ResolvedBattle | null>(null);
  private readonly _finalizeResult = signal<TurnFinalizeResult | null>(null);
  private readonly _currentTurn = signal<number | null>(null);
  private readonly _navigating = signal(false);

  readonly state = this._state.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly busy = this._busy.asReadonly();
  readonly error = this._error.asReadonly();
  readonly lastReport = this._lastReport.asReadonly();
  readonly finalizeResult = this._finalizeResult.asReadonly();
  readonly currentTurn = this._currentTurn.asReadonly();
  readonly navigating = this._navigating.asReadonly();

  readonly active = computed(() => this._state()?.active ?? false);

  async loadState(): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      this._state.set(await firstValueFrom(this.api.adminGetResolutionState()));
    } catch (error) {
      this._error.set(httpErrorMessage(error, 'Erreur lors du chargement de la session'));
    } finally {
      this._loading.set(false);
    }
  }

  async loadCurrentTurn(): Promise<void> {
    try {
      const res = await firstValueFrom(this.api.adminGetCurrentTurn());
      this._currentTurn.set(res.currentTurn);
    } catch (error) {
      this._error.set(httpErrorMessage(error, 'Erreur lors de la récupération du tour courant'));
    }
  }

  async navigateToTurn(turn: number): Promise<void> {
    this._navigating.set(true);
    this._error.set(null);
    try {
      const res = await firstValueFrom(this.api.adminNavigateToTurn(turn));
      this._currentTurn.set(res.currentTurn);
      await this.loadState();
    } catch (error) {
      this._error.set(httpErrorMessage(error, 'Erreur lors de la navigation vers le tour'));
    } finally {
      this._navigating.set(false);
    }
  }

  async startSession(): Promise<void> {
    await this.mutate(
      () => this.api.adminStartResolution(),
      'Erreur lors du démarrage de la session',
    );
  }

  async advanceHop(): Promise<void> {
    await this.mutate(() => this.api.adminAdvanceHop(), 'Erreur lors du passage au hop suivant');
  }

  async resolveBattle(conflictId: number): Promise<void> {
    this._busy.set(true);
    this._error.set(null);
    this._lastReport.set(null);
    try {
      const report = await firstValueFrom(this.api.adminResolveBattle(conflictId));
      this._lastReport.set(report);
      // Le combat ne renvoie que son compte-rendu : on recharge l'état pour
      // rafraîchir les conflits en attente et l'historique des batailles résolues.
      await this.loadState();
    } catch (error) {
      this._error.set(httpErrorMessage(error, 'Erreur lors de la résolution de la bataille'));
    } finally {
      this._busy.set(false);
    }
  }

  async finalizeResolution(): Promise<void> {
    this._busy.set(true);
    this._error.set(null);
    try {
      const result = await firstValueFrom(this.api.adminFinalizeResolution());
      this._finalizeResult.set(result);
      this._currentTurn.set(result.newTurn);
      this._lastReport.set(null);
      await this.loadState();
    } catch (error) {
      this._error.set(httpErrorMessage(error, 'Erreur lors de la finalisation du tour'));
    } finally {
      this._busy.set(false);
    }
  }

  async abort(): Promise<void> {
    this._busy.set(true);
    this._error.set(null);
    try {
      await firstValueFrom(this.api.adminAbortResolution());
      this._lastReport.set(null);
      await this.loadState();
    } catch (error) {
      this._error.set(httpErrorMessage(error, "Erreur lors de l'abandon de la session"));
    } finally {
      this._busy.set(false);
    }
  }

  clearLastReport(): void {
    this._lastReport.set(null);
  }

  clearFinalizeResult(): void {
    this._finalizeResult.set(null);
  }

  clearError(): void {
    this._error.set(null);
  }

  private async mutate(
    call: () => ReturnType<ApiService['adminStartResolution']>,
    fallback: string,
  ): Promise<void> {
    this._busy.set(true);
    this._error.set(null);
    try {
      this._state.set(await firstValueFrom(call()));
    } catch (error) {
      this._error.set(httpErrorMessage(error, fallback));
    } finally {
      this._busy.set(false);
    }
  }
}
