import {
  ChangeDetectionStrategy,
  Component,
  computed,
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
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { Player, UnitClass, UnitType } from '../../models';
import { AdminService } from '../../services/admin.service';
import { ApiService } from '../../services/api.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';
import { APP_CONSTANTS } from '../../core/constants';
import { ExpPipe } from '../../shared/exp.pipe';
import { groupPlayersByRace } from './player-management.helpers';
import { PlayerEditDialogComponent } from './player-edit-dialog/player-edit-dialog.component';

@Component({
  selector: 'app-player-management',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe,
    ExpPipe,
    MatExpansionModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatDividerModule,
    MatSnackBarModule,
    MatProgressBarModule,
    MatDialogModule,
  ],
  templateUrl: './player-management.component.html',
  styleUrls: ['./player-management.component.scss'],
})
export class PlayerManagementComponent {
  private readonly admin = inject(AdminService);
  private readonly api = inject(ApiService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  readonly players = this.admin.players;
  readonly playersTruncated = this.admin.playersTruncated;
  readonly loading = this.admin.loading;
  readonly importing = this.admin.importing;

  readonly searchQuery = signal('');

  readonly filteredPlayers = computed(() => {
    const query = this.searchQuery().toLowerCase();
    const all = this.players();
    return query ? all.filter((p) => p.name.toLowerCase().includes(query)) : all;
  });

  readonly playerGroups = computed(() => groupPlayersByRace(this.filteredPlayers()));

  constructor() {
    this.admin.reloadPlayers();
  }

  onSearchChange(value: string): void {
    this.searchQuery.set(value);
  }

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
        // Le service affiche déjà l'erreur.
      });
    };
    input.click();
  }

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

  openEdit(player: Player): void {
    if (player.id === null) return;
    this.dialog
      .open(PlayerEditDialogComponent, {
        width: '90vw',
        maxWidth: '1200px',
        maxHeight: '92vh',
        data: { playerId: player.id, playerName: player.name },
      })
      .afterClosed()
      .subscribe(() => this.admin.closeEdit());
  }

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
