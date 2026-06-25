import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DecimalPipe, NgTemplateOutlet } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Player, Sector, Unit, Vehicle } from '../../models';
import { PlayerService } from '../../services/player.service';
import {
  VehiclePlacementModalComponent,
  VehiclePlacementDialogData,
} from './vehicle-placement-modal.component';

@Component({
  selector: 'app-joueur',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe,
    NgTemplateOutlet,
    MatCardModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatTooltipModule,
    MatButtonModule,
    MatDialogModule,
  ],
  templateUrl: './joueur.component.html',
  styleUrls: ['./joueur.component.scss'],
})
export class JoueurComponent {
  private readonly playerService = inject(PlayerService);
  private readonly dialog = inject(MatDialog);

  readonly player = this.playerService.player;
  readonly loading = this.playerService.loading;
  readonly error = this.playerService.error;
  readonly undeployedVehicles = this.playerService.undeployedVehicles;
  readonly vehiclesLoading = this.playerService.vehiclesLoading;

  constructor() {
    // Bootstrap: fetch the player profile & vehicles for the logged-in user.
    void this.playerService.loadCurrent();
    void this.playerService.loadVehicles();
  }

  // Display mode: 'list' or 'tile'.
  readonly viewMode = signal<'list' | 'tile'>('list');

  readonly playerCharacter = computed(() => this.player()?.character ?? null);
  readonly allBuildings = computed(() => this.player()?.buildings ?? []);

  /** Vehicles owned by the player, aggregated across their sectors. */
  readonly allVehicles = computed(() => {
    const p = this.player();
    if (!p) return [];
    const result: { vehicle: Vehicle; sectorName: string }[] = [];
    p.sectors.forEach((s) =>
      s.vehicles?.forEach((v) => result.push({ vehicle: v, sectorName: s.name })),
    );
    return result;
  });

  // Filters.
  readonly showFilters = signal(false);
  readonly selectedTypeFilter = signal<string>('all');
  readonly selectedLocationFilter = signal<string>('all');
  readonly selectedStatusFilter = signal<string>('all');

  readonly expandedUnitIds = signal<Set<number>>(new Set());

  readonly unitTypes = computed(() => {
    const p = this.player();
    if (!p) return [];
    const types = new Set<string>();
    p.sectors.forEach((s) => s.army?.forEach((u) => types.add(u.type.name)));
    return Array.from(types).sort((a, b) => a.localeCompare(b));
  });

  readonly playerSectors = computed(() => {
    const p = this.player();
    if (!p) return [];
    return p.sectors.filter((s) => s.army && s.army.length > 0);
  });

  readonly filteredUnits = computed(() => {
    const p = this.player();
    if (!p) return [];
    let units = this.getAllUnitsWithLocation(p);

    const typeFilter = this.selectedTypeFilter();
    if (typeFilter !== 'all') {
      units = units.filter((u) => u.unit.type.name === typeFilter);
    }

    const locationFilter = this.selectedLocationFilter();
    if (locationFilter !== 'all') {
      units = units.filter((u) => u.sectorNumber === Number.parseInt(locationFilter, 10));
    }

    const statusFilter = this.selectedStatusFilter();
    if (statusFilter === 'injured') {
      units = units.filter((u) => u.unit.isInjured);
    } else if (statusFilter === 'healthy') {
      units = units.filter((u) => !u.unit.isInjured);
    }

    return units;
  });

  readonly activeFiltersCount = computed(() => {
    let count = 0;
    if (this.selectedTypeFilter() !== 'all') count++;
    if (this.selectedLocationFilter() !== 'all') count++;
    if (this.selectedStatusFilter() !== 'all') count++;
    return count;
  });

  /** Open the vehicle-placement modal and dispatch the placement on confirm. */
  openPlacementModal(vehicle: Vehicle): void {
    const ownedSectors: Sector[] = this.player()?.sectors ?? [];
    const dialogData: VehiclePlacementDialogData = { vehicle, ownedSectors };
    const dialogRef = this.dialog.open(VehiclePlacementModalComponent, {
      width: '420px',
      data: dialogData,
    });

    dialogRef
      .afterClosed()
      .pipe(takeUntilDestroyed())
      .subscribe((sector: Sector | null) => {
        if (sector && vehicle.id != null && sector.boardId != null && sector.number != null) {
          void this.playerService.placeVehicle(vehicle.id, sector.boardId, sector.number);
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
      { label: 'Territoires', value: player.sectors.length, icon: 'place', color: '#8b5cf6' },
    ];
  }

  getAllUnitsWithLocation(
    player: Player,
  ): { unit: Unit; sectorName: string; sectorNumber: number }[] {
    const result: { unit: Unit; sectorName: string; sectorNumber: number }[] = [];
    player.sectors.forEach((sector) => {
      sector.army?.forEach((unit) => {
        result.push({ unit, sectorName: sector.name, sectorNumber: sector.number ?? 0 });
      });
    });
    return result.sort((a, b) => {
      const typeCompare = a.unit.type.name.localeCompare(b.unit.type.name);
      if (typeCompare !== 0) return typeCompare;
      return a.unit.number - b.unit.number;
    });
  }

  setViewMode(mode: 'list' | 'tile'): void {
    this.viewMode.set(mode);
  }

  toggleFilters(): void {
    this.showFilters.update((v) => !v);
  }

  resetFilters(): void {
    this.selectedTypeFilter.set('all');
    this.selectedLocationFilter.set('all');
    this.selectedStatusFilter.set('all');
  }

  toggleUnitExpand(unitId: number): void {
    this.expandedUnitIds.update((set) => {
      const newSet = new Set(set);
      if (newSet.has(unitId)) newSet.delete(unitId);
      else newSet.add(unitId);
      return newSet;
    });
  }

  isUnitExpanded(unitId: number): boolean {
    return this.expandedUnitIds().has(unitId);
  }

  expandAll(): void {
    this.expandedUnitIds.set(new Set(this.filteredUnits().map((u) => u.unit.id)));
  }

  collapseAll(): void {
    this.expandedUnitIds.set(new Set());
  }
}
