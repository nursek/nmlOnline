import { Injectable, computed, inject, signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import {
  ResolvedBattle,
  ScenarioSummary,
  TurnFinalizeResult,
  TurnResolutionState,
} from '../models';
import { httpErrorMessage } from '../core/http-error.interceptor';
import { environment } from '../../environments/environment';

/**
 * État de la session de résolution pas-à-pas par hop (admin).
 *
 * <p>Pure signals + {@link ApiService} — pas de NgRx. Les lectures viennent d'un
 * signal `_state` rafraîchi après chaque mutation (POST/DELETE) via
 * {@code firstValueFrom}. {@code busy} désactive les boutons d'action pendant
 * une opération en cours.</p>
 */
@Injectable({ providedIn: 'root' })
export class TurnResolutionService {
  private readonly api = inject(ApiService);

  private readonly _state = signal<TurnResolutionState | null>(null);
  private readonly _loading = signal(false);
  private readonly _busy = signal(false);
  private readonly _error = signal<string | null>(null);
  private readonly _lastReport = signal<ResolvedBattle | null>(null);
  private readonly _finalizeResult = signal<TurnFinalizeResult | null>(null);

  readonly state = this._state.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly busy = this._busy.asReadonly();
  readonly error = this._error.asReadonly();
  readonly lastReport = this._lastReport.asReadonly();
  readonly finalizeResult = this._finalizeResult.asReadonly();

  readonly active = computed(() => this._state()?.active ?? false);

  // --- Scénario de test (dev uniquement) ------------------------------
  // Probe : GET /admin/dev/seed-resolution-scenario. En prod le contrôleur
  // @Profile("dev") n'existe pas → 404 → httpResource renvoie undefined.
  private readonly devScenarioRef = httpResource<{ available: boolean }>(() => ({
    url: `${environment.apiBaseUrl}/admin/dev/seed-resolution-scenario`,
  }));
  readonly devScenarioAvailable = computed(() => this.devScenarioRef.value()?.available ?? false);
  private readonly _seeding = signal(false);
  private readonly _seedReport = signal<ScenarioSummary | null>(null);
  readonly seeding = this._seeding.asReadonly();
  readonly seedReport = this._seedReport.asReadonly();

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

  // --- Scénario de test (dev) ----------------------------------------
  async seedDevScenario(): Promise<void> {
    this._seeding.set(true);
    this._error.set(null);
    this._seedReport.set(null);
    try {
      const report = await firstValueFrom(this.api.adminSeedDevScenario());
      this._seedReport.set(report);
      // Un nouveau tour/scénario : on rafraîchit l'état de la session.
      await this.loadState();
    } catch (error) {
      this._error.set(httpErrorMessage(error, 'Erreur lors du seeding du scénario'));
    } finally {
      this._seeding.set(false);
    }
  }

  clearSeedReport(): void {
    this._seedReport.set(null);
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
