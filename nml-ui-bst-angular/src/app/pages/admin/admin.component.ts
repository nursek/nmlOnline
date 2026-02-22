import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Store } from '@ngrx/store';
import { toSignal } from '@angular/core/rxjs-interop';
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
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { Player } from '../../models';
import {
  AdminActions,
  selectAdminPlayers,
  selectAdminLoading,
  selectAdminImporting,
  selectAdminError,
  selectAdminSuccessMessage
} from '../../store';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
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
  ],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss']
})
export class AdminComponent implements OnInit, OnDestroy {
  private readonly store = inject(Store);
  private readonly snackBar = inject(MatSnackBar);
  private readonly apiService = inject(ApiService);

  readonly players = toSignal(this.store.select(selectAdminPlayers), { initialValue: [] });
  readonly loading = toSignal(this.store.select(selectAdminLoading), { initialValue: false });
  readonly importing = toSignal(this.store.select(selectAdminImporting), { initialValue: false });

  searchQuery = signal('');

  filteredPlayers = computed(() => {
    const query = this.searchQuery().toLowerCase();
    const allPlayers = this.players();
    if (!query) return allPlayers;
    return allPlayers.filter(p => p.name.toLowerCase().includes(query));
  });

  private subscriptions: Subscription[] = [];

  ngOnInit(): void {
    this.store.dispatch(AdminActions.fetchAdminPlayers());

    // Écouter les messages de succès
    this.subscriptions.push(
      this.store.select(selectAdminSuccessMessage).pipe(
        filter((msg): msg is string => msg !== null)
      ).subscribe(msg => {
        this.snackBar.open(msg, 'OK', { duration: 4000, panelClass: 'success-snackbar' });
        this.store.dispatch(AdminActions.clearAdminMessages());
      })
    );

    // Écouter les erreurs
    this.subscriptions.push(
      this.store.select(selectAdminError).pipe(
        filter((err): err is string => err !== null)
      ).subscribe(err => {
        this.snackBar.open(err, 'Fermer', { duration: 6000, panelClass: 'error-snackbar' });
        this.store.dispatch(AdminActions.clearAdminMessages());
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  onSearchChange(value: string): void {
    this.searchQuery.set(value);
  }

  // === Import ===
  triggerImport(): void {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.json';
    input.onchange = (event) => {
      const file = (event.target as HTMLInputElement).files?.[0];
      if (file) {
        this.store.dispatch(AdminActions.importPlayer({ file }));
      }
    };
    input.click();
  }

  // === Export ===
  exportPlayer(player: Player): void {
    if (player.id === null) return;
    this.apiService.adminExportPlayer(player.id).subscribe({
      next: (data) => {
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${player.name}.json`;
        a.click();
        URL.revokeObjectURL(url);
        this.snackBar.open(`Export de ${player.name} téléchargé`, 'OK', { duration: 3000 });
      },
      error: () => {
        this.snackBar.open('Erreur lors de l\'export', 'Fermer', { duration: 4000 });
      }
    });
  }

  // === Delete ===
  confirmDelete(player: Player): void {
    if (player.id === null) return;
    if (confirm(`Supprimer définitivement "${player.name}" ? Ses secteurs redeviendront neutres.`)) {
      this.store.dispatch(AdminActions.deletePlayer({ playerId: player.id }));
    }
  }

  // === Helpers d'affichage ===
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

  getUnitTypeLabel(type: any): string {
    if (!type) return '?';
    return typeof type === 'string' ? type : type.name || '?';
  }

  getClassNames(classes: any[]): string {
    if (!classes || classes.length === 0) return '';
    return classes.map(c => typeof c === 'string' ? c : c.name || c.code || '?').join(' / ');
  }

  getUnitEquipmentNames(unit: any): string {
    if (!unit.equipments || unit.equipments.length === 0) return 'Aucun';
    return unit.equipments.map((e: any) => typeof e === 'string' ? e : e.name).join(', ');
  }
}
