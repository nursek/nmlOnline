import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DevScenarioService } from '../../services/dev-scenario.service';

@Component({
  selector: 'app-dev-scenarios',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatSnackBarModule,
  ],
  templateUrl: './dev-scenarios.component.html',
  styleUrls: ['./dev-scenarios.component.scss'],
})
export class DevScenariosComponent {
  private readonly scenarios = inject(DevScenarioService);
  private readonly snackBar = inject(MatSnackBar);

  readonly available = this.scenarios.available;
  readonly loading = this.scenarios.loading;
  readonly seeding = this.scenarios.seeding;
  readonly seedReport = this.scenarios.seedReport;
  readonly demoReport = this.scenarios.demoReport;
  readonly demoAvailable = this.scenarios.demoAvailable;
  readonly error = this.scenarios.error;
  readonly combatScenarios = this.scenarios.combatScenarios;
  readonly sortedCombatScenarios = computed(() =>
    [...this.combatScenarios()].sort(
      (a, b) => Number(b.eliminatesPlayer) - Number(a.eliminatesPlayer),
    ),
  );
  readonly pendingAction = signal<string | null>(null);

  private readonly combatIcons: Record<string, string> = {
    MIXED_5050: 'swords',
    DESTRUCTOR_VS_TANK: 'directions_car',
    CREW_PROTECTED: 'shield',
    CREW_DISEMBARK: 'airport_shuttle',
    AERIAL_HIT: 'flight',
    AERIAL_ANTI_AIR: 'rocket_launch',
    BLDG_CAPTURE: 'domain',
    BLDG_DESTROY: 'gavel',
    CHAR_KILL: 'person_off',
    MAIN_CHARACTER_ELIMINATION: 'dangerous',
    CHAR_REGEN: 'healing',
  };

  constructor() {
    effect(() => {
      const seed = this.seedReport();
      if (!seed) {
        return;
      }
      this.snackBar.open(seed.message ?? 'Scénario prêt', 'OK', {
        duration: 6000,
        panelClass: 'toast-success',
      });
      this.scenarios.clearSeedReport();
    });
  }

  combatIcon(code: string): string {
    return this.combatIcons[code] ?? 'swords';
  }

  onSeedCombatScenario(code: string): void {
    this.run(code, () => this.scenarios.seedCombatScenario(code));
  }

  onSeedExchangeScenario(): void {
    this.run('EXCHANGE', () => this.scenarios.seedExchangeScenario());
  }

  onSeedAllianceScenario(): void {
    this.run('ALLIANCE', () => this.scenarios.seedAllianceScenario());
  }

  onSeedResourceSaleScenario(): void {
    this.run('SALE', () => this.scenarios.seedResourceSaleScenario());
  }

  onSeedScenario(): void {
    this.run('HOP_2', () => this.scenarios.seedDevScenario());
  }

  onSeedStandoffScenario(): void {
    this.run('STANDOFF', () => this.scenarios.seedStandoffScenario());
  }

  onPlayDemoTurns(): void {
    this.run('DEMO', () => this.scenarios.playDemoTurns());
  }

  private run(action: string, call: () => Promise<void>): void {
    this.pendingAction.set(action);
    void call().finally(() => this.pendingAction.set(null));
  }
}
