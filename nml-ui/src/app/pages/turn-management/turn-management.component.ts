import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  effect,
  signal,
} from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';
import { TurnManagementService } from '../../services/turn-management.service';
import { DevScenarioService } from '../../services/dev-scenario.service';
import { ApiService } from '../../services/api.service';
import { PendingCapture, ResolvedBattle } from '../../models';
import { CombatLogDialogComponent } from './combat-log-dialog.component';

@Component({
  selector: 'app-turn-management',
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
    MatFormFieldModule,
    MatInputModule,
    MatDialogModule,
  ],
  templateUrl: './turn-management.component.html',
  styleUrls: ['./turn-management.component.scss'],
})
export class TurnManagementComponent {
  private readonly turnManagement = inject(TurnManagementService);
  private readonly devScenarios = inject(DevScenarioService);
  private readonly api = inject(ApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly pendingCaptures = signal<PendingCapture[]>([]);
  readonly targetTurn = signal<number | null>(null);

  readonly state = this.turnManagement.state;
  readonly busy = this.turnManagement.busy;
  readonly loading = this.turnManagement.loading;
  readonly error = this.turnManagement.error;
  readonly lastReport = this.turnManagement.lastReport;
  readonly finalizeResult = this.turnManagement.finalizeResult;
  readonly active = this.turnManagement.active;
  readonly currentTurn = this.turnManagement.currentTurn;
  readonly navigating = this.turnManagement.navigating;
  readonly devScenarioAvailable = this.devScenarios.available;

  readonly currentTurnLabel = computed(() => {
    const t = this.currentTurn();
    return t === null || t === undefined ? '—' : `Tour ${t}`;
  });

  readonly canNavigate = computed(() => {
    const t = this.targetTurn();
    return t !== null && t >= 1 && !this.active() && !this.busy() && !this.navigating();
  });

  constructor() {
    void this.turnManagement.loadState();
    void this.turnManagement.loadCurrentTurn();
    void this.loadPendingCaptures();

    effect(() => {
      const report = this.lastReport();
      if (report) {
        let msg: string;
        if (!report.success) {
          msg = `Secteur ${report.sectorNumber}: ${report.message ?? 'échec'}`;
        } else if (report.standoff) {
          const pertes = report.participants?.reduce((sum, p) => sum + p.casualties, 0) ?? 0;
          msg = `Impasse secteur ${report.sectorNumber}: ${pertes} pertes — vainqueur ${report.winnerName ?? 'aucun'}`;
        } else {
          msg = `Secteur ${report.sectorNumber}: ${report.defenderCasualties} pertes déf., ${report.attackerInjured} blessé(s) attaquant`;
        }
        this.snackBar.open(msg, 'OK', { duration: 4000, panelClass: 'toast-info' });
        this.turnManagement.clearLastReport();
      }
    });
    effect(() => {
      const fin = this.finalizeResult();
      if (fin) {
        this.snackBar.open(fin.message ?? `Tour ${fin.newTurn} démarré`, 'OK', {
          duration: 5000,
          panelClass: 'toast-success',
        });
        this.turnManagement.clearFinalizeResult();
      }
    });
  }

  onStart(): void {
    void this.turnManagement.startSession().catch(() => {
      /* le service positionne déjà le signal d'erreur */
    });
  }

  onAdvanceHop(): void {
    void this.turnManagement.advanceHop().catch(() => {});
  }

  onResolveBattle(conflictId: number): void {
    void this.turnManagement.resolveBattle(conflictId).catch(() => {});
  }

  onShowDetail(report: ResolvedBattle): void {
    this.dialog.open(CombatLogDialogComponent, { data: report, width: '720px' });
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
          void this.turnManagement.finalizeResolution().catch(() => {});
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
          void this.turnManagement.abort().catch(() => {});
        }
      });
  }

  onTargetTurnChange(value: string): void {
    const parsed = Number.parseInt(value, 10);
    this.targetTurn.set(Number.isNaN(parsed) ? null : parsed);
  }

  onNavigateToTurn(): void {
    const turn = this.targetTurn();
    if (turn === null || turn < 1) {
      return;
    }
    void this.turnManagement.navigateToTurn(turn).catch(() => {});
  }

  async loadPendingCaptures(): Promise<void> {
    try {
      this.pendingCaptures.set(await firstValueFrom(this.api.adminGetPendingCaptures()));
    } catch {
      // Section secondaire : pas d'erreur bloquante.
    }
  }

  async onResolvePendingCapture(
    pending: PendingCapture,
    playerId: number,
    playerName: string,
  ): Promise<void> {
    try {
      await firstValueFrom(this.api.adminResolvePendingCapture(pending.id, playerId));
      this.snackBar.open(`Secteur ${pending.sectorNumber} attribué à ${playerName}`, 'OK', {
        duration: 4000,
      });
      await this.loadPendingCaptures();
    } catch {
      this.snackBar.open("Échec de l'attribution", 'OK', { duration: 4000 });
    }
  }

  async onDismissPendingCapture(pending: PendingCapture): Promise<void> {
    try {
      await firstValueFrom(this.api.adminDismissPendingCapture(pending.id));
      await this.loadPendingCaptures();
    } catch {
      this.snackBar.open('Échec', 'OK', { duration: 4000 });
    }
  }
}
