import { Injectable, computed, inject, signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { Observable, firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import { AuthService } from './auth.service';
import { ActiveBoardService } from './active-board.service';
import { PlayerDirectoryService } from './player-directory.service';
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
  private readonly activeBoard = inject(ActiveBoardService);
  private readonly playerDirectory = inject(PlayerDirectoryService);

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

  // La démo ne se joue que depuis le tour 1 : le bouton se désactive dès qu'un tour a été résolu.
  private readonly turnRef = httpResource<{ currentTurn: number }>(() => {
    if (!this.auth.initialized() || !this.auth.isAdmin()) {
      return undefined;
    }
    return { url: `${environment.apiBaseUrl}/admin/turn/current` };
  });
  readonly demoAvailable = computed(() => this.turnRef.value()?.currentTurn === 1);

  private readonly _seeding = signal(false);
  private readonly _error = signal<string | null>(null);
  private readonly _seedReport = signal<ScenarioSummary | ExchangeScenarioSummary | null>(null);
  private readonly _demoReport = signal<ScenarioSummary | null>(null);

  readonly seeding = this._seeding.asReadonly();
  readonly error = this._error.asReadonly();
  readonly seedReport = this._seedReport.asReadonly();
  readonly demoReport = this._demoReport.asReadonly();

  async seedDevScenario(): Promise<void> {
    await this.seed(() => this.api.adminSeedDevScenario(), 'Erreur lors du seeding du scénario');
  }

  async playDemoTurns(): Promise<void> {
    this._seeding.set(true);
    this._error.set(null);
    this._demoReport.set(null);
    try {
      this._demoReport.set(await firstValueFrom(this.api.adminPlayDemoTurns()));
    } catch (error) {
      this._error.set(httpErrorMessage(error, 'Erreur lors du scénario de démo'));
    } finally {
      // Une démo interrompue a déjà écrit : on relit l'état même en échec.
      this.activeBoard.reload();
      this.playerDirectory.reload();
      this.turnRef.reload();
      this._seeding.set(false);
    }
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

  async seedAllianceScenario(): Promise<void> {
    await this.seed(
      () => this.api.adminSeedAllianceScenario(),
      "Erreur lors du seeding de l'alliance",
    );
  }

  async seedResourceSaleScenario(): Promise<void> {
    await this.seed(
      () => this.api.adminSeedResourceSaleScenario(),
      'Erreur lors du seeding de la vente',
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
    this._demoReport.set(null);
    try {
      this._seedReport.set(await firstValueFrom(call()));
    } catch (error) {
      this._error.set(httpErrorMessage(error, fallback));
    } finally {
      this._seeding.set(false);
    }
  }
}
