import { Injectable, computed, inject, signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { Observable, firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import { AuthService } from './auth.service';
import { CombatScenarioInfo, ExchangeScenarioSummary, ScenarioSummary } from '../models';
import { httpErrorMessage } from '../core/http-error.interceptor';
import { environment } from '../../environments/environment';

/**
 * Scénarios de test (dev). Le GET de statut sert de probe : en prod, le contrôleur
 * `@Profile("dev")` n'existe pas → 404 → `available` reste faux.
 */
@Injectable({ providedIn: 'root' })
export class DevScenarioService {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);

  // La navbar globale instancie ce service dès /login : ne pas sonder avant une session admin.
  private readonly statusRef = httpResource<{ available: boolean }>(() => {
    if (!this.auth.initialized() || !this.auth.isAdmin()) {
      return undefined;
    }
    return { url: `${environment.apiBaseUrl}/admin/dev/seed-resolution-scenario` };
  });
  readonly available = computed(() => this.statusRef.value()?.available ?? false);
  readonly loading = computed(() => this.statusRef.isLoading());

  private readonly combatScenariosRef = httpResource<CombatScenarioInfo[]>(() => {
    if (!this.auth.initialized() || !this.auth.isAdmin()) {
      return undefined;
    }
    return { url: `${environment.apiBaseUrl}/admin/dev/combat-scenarios` };
  });
  readonly combatScenarios = computed(() => this.combatScenariosRef.value() ?? []);

  private readonly _seeding = signal(false);
  private readonly _error = signal<string | null>(null);
  private readonly _seedReport = signal<ScenarioSummary | ExchangeScenarioSummary | null>(null);

  readonly seeding = this._seeding.asReadonly();
  readonly error = this._error.asReadonly();
  readonly seedReport = this._seedReport.asReadonly();

  async seedDevScenario(): Promise<void> {
    await this.seed(() => this.api.adminSeedDevScenario(), 'Erreur lors du seeding du scénario');
  }

  async seedStandoffScenario(): Promise<void> {
    await this.seed(
      () => this.api.adminSeedStandoffScenario(),
      "Erreur lors du seeding de l'impasse",
    );
  }

  async seedCombatScenario(code: string): Promise<void> {
    await this.seed(
      () => this.api.adminSeedCombatScenario(code),
      'Erreur lors du seeding du scénario de combat',
    );
  }

  async seedExchangeScenario(): Promise<void> {
    await this.seed(
      () => this.api.adminSeedExchangeScenario(),
      "Erreur lors du seeding de l'échange",
    );
  }

  clearSeedReport(): void {
    this._seedReport.set(null);
  }

  clearError(): void {
    this._error.set(null);
  }

  private async seed<T extends ScenarioSummary | ExchangeScenarioSummary>(
    call: () => Observable<T>,
    fallback: string,
  ): Promise<void> {
    this._seeding.set(true);
    this._error.set(null);
    this._seedReport.set(null);
    try {
      this._seedReport.set(await firstValueFrom(call()));
    } catch (error) {
      this._error.set(httpErrorMessage(error, fallback));
    } finally {
      this._seeding.set(false);
    }
  }
}
