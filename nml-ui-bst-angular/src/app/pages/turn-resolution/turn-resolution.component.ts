import { ChangeDetectionStrategy, Component, inject, effect } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';
import { TurnResolutionService } from '../../services/turn-resolution.service';

/**
 * Page admin dédiée à la résolution de fin de tour pas-à-pas, hop par hop.
 *
 * <p>Flux : Démarrer → [Hop suivant → résoudre chaque bataille] × N → Finaliser.
 * L'état (hop courant, conflits en attente, batailles résolues) vient du
 * {@link TurnResolutionService}. Les confirmations destructives/finales passent
 * par {@link ConfirmDialogComponent}.</p>
 */
@Component({
  selector: 'app-turn-resolution',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatSnackBarModule,
    MatCardModule,
    MatTooltipModule,
    MatDividerModule,
    MatDialogModule,
  ],
  templateUrl: './turn-resolution.component.html',
  styleUrls: ['./turn-resolution.component.scss'],
})
export class TurnResolutionComponent {
  private readonly resolution = inject(TurnResolutionService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly state = this.resolution.state;
  readonly busy = this.resolution.busy;
  readonly loading = this.resolution.loading;
  readonly error = this.resolution.error;
  readonly lastReport = this.resolution.lastReport;
  readonly finalizeResult = this.resolution.finalizeResult;
  readonly active = this.resolution.active;
  readonly devScenarioAvailable = this.resolution.devScenarioAvailable;
  readonly seeding = this.resolution.seeding;
  readonly seedReport = this.resolution.seedReport;

  constructor() {
    void this.resolution.loadState();

    // Feedback snackbar sur le dernier rapport de bataille / finalisation.
    effect(() => {
      const report = this.lastReport();
      if (report) {
        const msg = report.success
          ? `Secteur ${report.sectorNumber}: ${report.defenderCasualties} pertes déf., ${report.attackerInjured} blessé(s) attaquant`
          : `Secteur ${report.sectorNumber}: ${report.message ?? 'échec'}`;
        this.snackBar.open(msg, 'OK', { duration: 4000, panelClass: 'toast-info' });
        this.resolution.clearLastReport();
      }
    });
    effect(() => {
      const fin = this.finalizeResult();
      if (fin) {
        this.snackBar.open(fin.message ?? `Tour ${fin.newTurn} démarré`, 'OK', {
          duration: 5000,
          panelClass: 'toast-success',
        });
        this.resolution.clearFinalizeResult();
      }
    });
    effect(() => {
      const seed = this.seedReport();
      if (seed) {
        this.snackBar.open(
          `Scénario prêt — ${seed.attacker.name} → secteur ${seed.route.at(-1)} (${seed.defender.name})`,
          'OK',
          { duration: 6000, panelClass: 'toast-success' },
        );
        this.resolution.clearSeedReport();
      }
    });
  }

  onStart(): void {
    void this.resolution.startSession().catch(() => {
      /* le service positionne déjà le signal d'erreur */
    });
  }

  onAdvanceHop(): void {
    void this.resolution.advanceHop().catch(() => {});
  }

  onResolveBattle(conflictId: number): void {
    void this.resolution.resolveBattle(conflictId).catch(() => {});
  }

  onFinalize(): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          title: 'Finaliser le tour',
          message:
            'Marquer les ordres comme résolus et passer au tour suivant ? Cette action est irréversible.',
          confirmLabel: 'Finaliser le tour',
          cancelLabel: 'Annuler',
        },
      })
      .afterClosed()
      .subscribe((confirmed: boolean) => {
        if (confirmed) {
          void this.resolution.finalizeResolution().catch(() => {});
        }
      });
  }

  onAbort(): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          title: 'Abandonner la session',
          message:
            'Libérer le verrou et fermer la session. Les unités déjà déplacées et les combats déjà résolus restent en place (pas de rollback).',
          confirmLabel: 'Abandonner',
          cancelLabel: 'Continuer la résolution',
        },
      })
      .afterClosed()
      .subscribe((confirmed: boolean) => {
        if (confirmed) {
          void this.resolution.abort().catch(() => {});
        }
      });
  }

  onSeedScenario(): void {
    void this.resolution.seedDevScenario().catch(() => {
      /* service positionne déjà le signal d'erreur */
    });
  }
}
