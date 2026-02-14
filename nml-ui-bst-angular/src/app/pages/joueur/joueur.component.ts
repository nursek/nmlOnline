import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Store } from '@ngrx/store';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { selectUser, selectCurrentPlayer, selectPlayerLoading, selectPlayerError, PlayerActions } from '../../store';
import { filter, take } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';
import { Player, Unit, PlayerResource } from '../../models';
import { ResourceService } from '../../services/resource.service';

@Component({
  selector: 'app-joueur',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatTooltipModule,
    MatButtonModule,
    MatSnackBarModule,
  ],
  templateUrl: './joueur.component.html',
  styleUrls: ['./joueur.component.scss']
})
export class JoueurComponent implements OnInit {
  private readonly store = inject(Store);
  private readonly resourceService = inject(ResourceService);
  private readonly snackBar = inject(MatSnackBar);

  player$ = this.store.select(selectCurrentPlayer);
  loading$ = this.store.select(selectPlayerLoading);
  error$ = this.store.select(selectPlayerError);

  // Signal pour le player
  player = toSignal(this.player$);

  // Mode d'affichage des unités : 'list' ou 'tile'
  viewMode = signal<'list' | 'tile'>('list');

  // Filtres
  showFilters = signal(false);
  selectedTypeFilter = signal<string>('all');
  selectedLocationFilter = signal<string>('all');
  selectedStatusFilter = signal<string>('all');

  // IDs des unités expandées (pour le mode liste)
  expandedUnitIds = signal<Set<number>>(new Set());

  // Types d'unités disponibles
  unitTypes = computed(() => {
    const p = this.player();
    if (!p) return [];
    const types = new Set<string>();
    p.sectors.forEach(s => s.army?.forEach(u => types.add(u.type.name)));
    return Array.from(types).sort((a, b) => a.localeCompare(b));
  });

  // Secteurs disponibles (pour filtre par localisation)
  playerSectors = computed(() => {
    const p = this.player();
    if (!p) return [];
    return p.sectors.filter(s => s.army && s.army.length > 0);
  });

  // Unités filtrées
  filteredUnits = computed(() => {
    const p = this.player();
    if (!p) return [];

    let units = this.getAllUnitsWithLocation(p);

    // Filtre par type
    const typeFilter = this.selectedTypeFilter();
    if (typeFilter !== 'all') {
      units = units.filter(u => u.unit.type.name === typeFilter);
    }

    // Filtre par localisation
    const locationFilter = this.selectedLocationFilter();
    if (locationFilter !== 'all') {
      units = units.filter(u => u.sectorNumber === Number.parseInt(locationFilter, 10));
    }

    // Filtre par statut (blessé ou non)
    const statusFilter = this.selectedStatusFilter();
    if (statusFilter === 'injured') {
      units = units.filter(u => u.unit.isInjured);
    } else if (statusFilter === 'healthy') {
      units = units.filter(u => !u.unit.isInjured);
    }

    return units;
  });

  // Nombre de filtres actifs
  activeFiltersCount = computed(() => {
    let count = 0;
    if (this.selectedTypeFilter() !== 'all') count++;
    if (this.selectedLocationFilter() !== 'all') count++;
    if (this.selectedStatusFilter() !== 'all') count++;
    return count;
  });

  ngOnInit(): void {
    this.store.select(selectUser).pipe(
      filter((user): user is NonNullable<typeof user> => !!user && !!user.username),
      take(1)
    ).subscribe(user => {
      console.log('Loading player for user:', user.username);
      this.store.dispatch(PlayerActions.fetchCurrentPlayer({ username: user.username }));
    });
  }

  /**
   * Vend une ressource du joueur
   */
  sellResource(resource: PlayerResource, quantity: number): void {
    if (!resource.id) {
      this.snackBar.open('Erreur: ressource invalide', 'Fermer', { duration: 3000 });
      return;
    }

    this.resourceService.sellResource(resource.id, quantity).subscribe({
      next: (response) => {
        this.snackBar.open(
          `✓ ${response.quantitySold}x ${response.resourceName} vendu(s) pour ${response.saleValue.toFixed(2)}$`,
          'Fermer',
          { duration: 4000, panelClass: ['success-snackbar'] }
        );

        // Recharger le joueur pour mettre à jour les données
        this.store.select(selectUser).pipe(take(1)).subscribe(user => {
          if (user?.username) {
            this.store.dispatch(PlayerActions.fetchCurrentPlayer({ username: user.username }));
          }
        });
      },
      error: (error) => {
        console.error('Erreur lors de la vente:', error);
        const message = error.error?.message || error.message || 'Erreur lors de la vente';
        this.snackBar.open(`❌ ${message}`, 'Fermer', {
          duration: 4000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  getMainStats(player: Player) {
    return [
      {
        label: 'Argent',
        value: `${player.stats.money.toFixed(0)} ₡`,
        icon: 'attach_money',
        color: '#f59e0b',
      },
      {
        label: 'Revenus',
        value: `${player.stats.totalIncome.toFixed(0)} ₡/tour`,
        icon: 'trending_up',
        color: '#10b981',
      },
      {
        label: 'Puissance globale',
        value: player.stats.globalPower.toFixed(0),
        icon: 'shield',
        color: '#6366f1',
      },
      {
        label: 'Territoires',
        value: player.sectors.length,
        icon: 'place',
        color: '#8b5cf6',
      },
    ];
  }

  /**
   * Récupère toutes les unités avec leur localisation
   */
  getAllUnitsWithLocation(player: Player): { unit: Unit; sectorName: string; sectorNumber: number }[] {
    const result: { unit: Unit; sectorName: string; sectorNumber: number }[] = [];
    player.sectors.forEach(sector => {
      sector.army?.forEach(unit => {
        result.push({
          unit,
          sectorName: sector.name,
          sectorNumber: sector.number ?? 0
        });
      });
    });
    return result.sort((a, b) => {
      const typeCompare = a.unit.type.name.localeCompare(b.unit.type.name);
      if (typeCompare !== 0) return typeCompare;
      return a.unit.number - b.unit.number;
    });
  }

  // Toggle view mode
  setViewMode(mode: 'list' | 'tile') {
    this.viewMode.set(mode);
  }

  // Toggle filters panel
  toggleFilters() {
    this.showFilters.update(v => !v);
  }

  // Reset all filters
  resetFilters() {
    this.selectedTypeFilter.set('all');
    this.selectedLocationFilter.set('all');
    this.selectedStatusFilter.set('all');
  }

  // Toggle unit expansion (list mode)
  toggleUnitExpand(unitId: number) {
    this.expandedUnitIds.update(set => {
      const newSet = new Set(set);
      if (newSet.has(unitId)) {
        newSet.delete(unitId);
      } else {
        newSet.add(unitId);
      }
      return newSet;
    });
  }

  isUnitExpanded(unitId: number): boolean {
    return this.expandedUnitIds().has(unitId);
  }

  // Expand/collapse all
  expandAll() {
    const allIds = new Set(this.filteredUnits().map(u => u.unit.id));
    this.expandedUnitIds.set(allIds);
  }

  collapseAll() {
    this.expandedUnitIds.set(new Set());
  }
}
