import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { Player, UnitClass, UnitType, MovementStatusFilter } from '../../models';
import { AdminService } from '../../services/admin.service';
import { ApiService } from '../../services/api.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';
import {
  BoardImportDialogComponent,
  BoardImportResult,
} from '../../shared/board-import-dialog/board-import-dialog.component';
import { APP_CONSTANTS } from '../../core/constants';

@Component({
  selector: 'app-admin',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe,
    MatExpansionModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatTooltipModule,
    MatDividerModule,
    MatSnackBarModule,
    MatProgressBarModule,
    MatDialogModule,
  ],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss'],
})
export class AdminComponent {
  private readonly admin = inject(AdminService);
  private readonly api = inject(ApiService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  readonly players = this.admin.players;
  readonly loading = this.admin.loading;
  readonly importing = this.admin.importing;
  readonly successMessage = this.admin.successMessage;
  readonly errorMessage = this.admin.error;
  readonly currentTurn = this.admin.currentTurn;
  readonly advancingTurn = this.admin.advancingTurn;

  // === Ordres du tour courant ===
  readonly orders = this.admin.orders;
  readonly ordersLoading = this.admin.ordersLoading;
  readonly orderStatusFilter = this.admin.orderStatusFilter;

  // === Compte-rendu de résolution ===
  readonly resolutionReport = this.admin.resolutionReport;
  readonly previewing = this.admin.previewing;
  readonly resolving = this.admin.resolving;

  readonly statusFilters: ReadonlyArray<MovementStatusFilter> = [
    'ALL',
    'PENDING',
    'RESOLVED',
    'BLOCKED',
    'CANCELLED',
  ];

  readonly searchQuery = signal('');

  readonly currentTurnLabel = computed(() => {
    const t = this.currentTurn();
    return t === null || t === undefined ? '—' : `Tour ${t}`;
  });

  readonly filteredPlayers = computed(() => {
    const query = this.searchQuery().toLowerCase();
    const all = this.players();
    return query ? all.filter((p) => p.name.toLowerCase().includes(query)) : all;
  });

  constructor() {
    // Bootstrap: trigger the admin players fetch and load the current turn.
    this.admin.reloadPlayers();
    void this.admin.loadCurrentTurn();
    this.admin.reloadOrders();

    // Surface admin success/error messages as snackbars (DOM side effect only).
    effect(() => {
      const msg = this.successMessage();
      if (msg) {
        this.snackBar.open(msg, 'OK', { duration: 4000, panelClass: 'success-snackbar' });
        this.admin.clearMessages();
      }
    });
    effect(() => {
      const err = this.errorMessage();
      if (err) {
        this.snackBar.open(err, 'Fermer', { duration: 6000, panelClass: 'error-snackbar' });
        this.admin.clearMessages();
      }
    });
  }

  onSearchChange(value: string): void {
    this.searchQuery.set(value);
  }

  // === Tour — finir le tour (résout les mouvements + incrémente) ===
  onAdvanceTurn(): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          title: 'Finir le tour',
          message:
            'Terminer le tour courant ? Les ordres de déplacement PENDING seront résolus, puis le compteur passera au tour suivant.',
          confirmLabel: 'Finir le tour',
          cancelLabel: 'Annuler',
        },
      })
      .afterClosed()
      .subscribe((confirmed: boolean) => {
        if (confirmed) {
          void this.admin.advanceTurn().catch(() => {
            // AdminService already set the error signal; ignore here.
          });
        }
      });
  }

  // === Ordres du tour courant : changement de filtre de statut ===
  onOrderStatusChange(status: MovementStatusFilter): void {
    this.orderStatusFilter.set(status);
  }

  // === Aperçu des conflits (dry-run, non mutant) ===
  onPreviewMovements(): void {
    void this.admin.previewMovements().catch(() => {
      // AdminService already set the error signal; ignore here.
    });
  }

  // === Application de la résolution des mouvements (mutant) ===
  onResolveMovements(): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          title: 'Résoudre les mouvements',
          message:
            'Appliquer la résolution des ordres PENDING du tour courant ? Les déplacements seront effectués ' +
            'et les ordres marqués RESOLVED/BLOCKED. Le numéro de tour ne change pas (utiliser « Finir le tour » ensuite).',
          confirmLabel: 'Résoudre',
          cancelLabel: 'Annuler',
        },
      })
      .afterClosed()
      .subscribe((confirmed: boolean) => {
        if (confirmed) {
          void this.admin.resolveMovements().catch(() => {
            // AdminService already set the error signal; ignore here.
          });
        }
      });
  }

  clearResolutionReport(): void {
    this.admin.clearResolutionReport();
  }

  // === Import joueur ===
  triggerImport(): void {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.json';
    input.onchange = (event) => {
      const file = (event.target as HTMLInputElement).files?.[0];
      if (!file) {
        return;
      }
      // ponytail: window.prompt au lieu d'un dialog MatDialog — un seul champ,
      // pas assez de valeur pour un composant dédié. Upgrade: dialog custom si
      // on veut validation/policy de mot de passe (longueur min, confirmation).
      const password = window.prompt(
        `Mot de passe pour le compte lié au joueur "${file.name.replace(/\.json$/i, '')}" ?\n` +
          '(Laisser vide = importer sans compte de connexion.)',
        '',
      );
      // prompt renvoie null si l'utilisateur annule : on abandonne l'import.
      if (password === null) {
        return;
      }
      void this.admin.importPlayer(file, password === '' ? undefined : password).catch(() => {
        // AdminService already set the error signal; ignore here.
      });
    };
    input.click();
  }

  // === Import board (board.json + assets optionnels) ===
  triggerBoardImport(): void {
    this.dialog
      .open(BoardImportDialogComponent)
      .afterClosed()
      .subscribe((result: BoardImportResult | undefined) => {
        if (!result) {
          return;
        }
        void this.admin.importBoard(result.file, result.mapImage, result.svgOverlay).catch(() => {
          // AdminService already set the error signal; ignore here.
        });
      });
  }

  // === Export ===
  async exportPlayer(player: Player): Promise<void> {
    if (player.id === null) return;
    try {
      const data = await firstValueFrom(this.api.adminExportPlayer(player.id));
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${player.name}.json`;
      a.click();
      URL.revokeObjectURL(url);
      this.snackBar.open(`Export de ${player.name} téléchargé`, 'OK', {
        duration: APP_CONSTANTS.SNACKBAR_SHORT_DURATION_MS,
      });
    } catch {
      this.snackBar.open("Erreur lors de l'export", 'Fermer', { duration: 4000 });
    }
  }

  // === Delete ===
  confirmDelete(player: Player): void {
    if (player.id === null) return;
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Supprimer le joueur',
        message: `Supprimer définitivement "${player.name}" ? Ses secteurs redeviendront neutres.`,
        confirmLabel: 'Supprimer',
        cancelLabel: 'Annuler',
      },
    });
    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed && player.id != null) {
        void this.admin.deletePlayer(player.id).catch(() => {});
      }
    });
  }

  // === Display helpers ===
  getTotalArmySize(player: Player): number {
    return player.sectors?.reduce((sum, s) => sum + (s.army?.length || 0), 0) || 0;
  }

  formatMoney(amount: number): string {
    if (amount >= 1_000_000) return (amount / 1_000_000).toFixed(1) + 'M';
    if (amount >= 1_000) return (amount / 1_000).toFixed(1) + 'k';
    return amount.toFixed(0);
  }

  getEquipmentCount(player: Player): number {
    return player.equipments?.reduce((sum, eq) => sum + eq.quantity, 0) || 0;
  }

  getUnitTypeLabel(type: UnitType | string | null | undefined): string {
    if (!type) return '?';
    return typeof type === 'string' ? type : type.name || '?';
  }

  getClassNames(classes: UnitClass[] | string[] | null | undefined): string {
    if (!classes || classes.length === 0) return '';
    return classes.map((c) => (typeof c === 'string' ? c : c.name || c.code || '?')).join(' / ');
  }
}
