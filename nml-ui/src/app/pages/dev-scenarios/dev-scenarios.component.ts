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
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
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
    MatFormFieldModule,
    MatSelectModule,
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
  readonly error = this.scenarios.error;
  readonly combatScenarios = this.scenarios.combatScenarios;
  readonly selectedCombatScenario = signal('MIXED_5050');
  readonly selectedScenarioInfo = computed(
    () =>
      this.combatScenarios().find((scenario) => scenario.code === this.selectedCombatScenario()) ??
      null,
  );

  constructor() {
    effect(() => {
      const seed = this.seedReport();
      if (!seed) {
        return;
      }
      const msg =
        'standoff' in seed
          ? seed.standoff
            ? `Impasse prête — ${seed.defender.name} défend le secteur ${seed.orders?.at(0)?.route.at(-1) ?? '?'}`
            : `Scénario prêt — ${seed.attacker?.name} → secteur ${seed.route?.at(-1)} (${seed.defender.name})`
          : seed.message;
      this.snackBar.open(msg, 'OK', { duration: 6000, panelClass: 'toast-success' });
      this.scenarios.clearSeedReport();
    });
  }

  onSeedScenario(): void {
    void this.scenarios.seedDevScenario().catch(() => {
      /* le service positionne déjà le signal d'erreur */
    });
  }

  onSeedStandoffScenario(): void {
    void this.scenarios.seedStandoffScenario().catch(() => {});
  }

  onSeedCombatScenario(): void {
    void this.scenarios.seedCombatScenario(this.selectedCombatScenario()).catch(() => {});
  }

  onSeedExchangeScenario(): void {
    void this.scenarios.seedExchangeScenario().catch(() => {});
  }
}
