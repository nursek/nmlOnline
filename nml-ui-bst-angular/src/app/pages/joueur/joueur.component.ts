import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  signal,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTabsModule } from '@angular/material/tabs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Equipment, Sector, Unit, Vehicle } from '../../models';
import { PlayerService } from '../../services/player.service';
import { slugify } from '../../core/slug';
import {
  VehiclePlacementModalComponent,
  VehiclePlacementDialogData,
} from './vehicle-placement-modal.component';
import { UnitDetailDialogComponent, UnitDetailDialogData } from './unit-detail-dialog.component';
import { ExpPipe } from '../../shared/exp.pipe';
import {
  buildingStats,
  characterStats,
  EconomyBreakdown,
  economyBreakdown,
  equipmentByClass,
  equipmentStackCost,
  incomeTotal,
  playerForces,
  totalsStats,
  troopSummaries,
  unitClassCodes,
  unitEquipmentLabel,
  unitStats,
  vehicleStats,
} from './joueur.helpers';

@Component({
  selector: 'app-joueur',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe,
    ExpPipe,
    MatCardModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatChipsModule,
    MatTooltipModule,
    MatButtonModule,
    MatDialogModule,
    MatExpansionModule,
    MatTabsModule,
  ],
  templateUrl: './joueur.component.html',
  styleUrls: ['./joueur.component.scss'],
})
export class JoueurComponent {
  private readonly playerService = inject(PlayerService);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);

  readonly player = this.playerService.player;
  readonly loading = this.playerService.loading;
  readonly error = this.playerService.error;
  readonly undeployedVehicles = this.playerService.undeployedVehicles;
  readonly vehiclesLoading = this.playerService.vehiclesLoading;
  readonly currentTurn = this.playerService.currentTurn;

  constructor() {
    void this.playerService.loadCurrent();
    void this.playerService.loadVehicles();
    void this.playerService.loadCurrentTurn();
  }

  readonly playerCharacter = computed(() => this.player()?.character ?? null);
  readonly conqueredSectors = computed(() => this.player()?.sectors ?? []);
  readonly income = computed(() => incomeTotal(this.conqueredSectors()));
  readonly troopSummaries = computed(() =>
    troopSummaries(this.conqueredSectors(), this.player()?.id ?? null),
  );
  readonly forces = computed(() =>
    playerForces(this.conqueredSectors(), this.player()?.id ?? null),
  );
  readonly groupedEquipments = computed(() => equipmentByClass(this.player()?.equipments ?? []));
  readonly economy = computed<EconomyBreakdown | null>(() => {
    const p = this.player();
    return p ? economyBreakdown(p, this.income()) : null;
  });
  readonly deployedVehicleCount = computed(() =>
    this.conqueredSectors().reduce((n, s) => n + (s.vehicles?.length ?? 0), 0),
  );

  readonly mainStats = computed(() => {
    const p = this.player();
    if (!p) return [];
    return [
      {
        label: 'Argent',
        value: `${p.stats.money.toFixed(0)} ₡`,
        icon: 'attach_money',
        color: '#b45309',
      },
      {
        label: 'Revenus',
        value: `${this.income().toFixed(0)} ₡/tour`,
        icon: 'trending_up',
        color: '#047857',
      },
      {
        label: 'Puissance globale',
        value: this.forces().globalPower.toFixed(0),
        icon: 'shield',
        color: '#6366f1',
      },
      { label: 'Territoires', value: p.sectors.length, icon: 'place', color: '#8b5cf6' },
    ];
  });

  // Libellés d'affichage des unités (délégués aux helpers purs).
  classCodes(u: Unit): string {
    return unitClassCodes(u);
  }

  equipmentLabel(u: Unit): string {
    return unitEquipmentLabel(u);
  }

  // Stats affichables, zéros masqués (helpers purs).
  unitStats = unitStats;
  characterStats = characterStats;
  buildingStats = buildingStats;
  vehicleStats = vehicleStats;
  totalsStats = totalsStats;
  stackCost = equipmentStackCost;

  // --- Vignettes d'équipement (mêmes assets que la boutique, fallback sur erreur) ---

  private readonly brokenImages = signal(new Set<string>());

  equipmentImageUrl(equipment: Equipment): string {
    return `assets/shop/equipment/${slugify(equipment.name)}.png`;
  }

  hasImage(key: string): boolean {
    return !this.brokenImages().has(key);
  }

  onImgError(key: string): void {
    this.brokenImages.update((set) => new Set(set).add(key));
  }

  /** Ouvre la modale de déploiement d'un véhicule de réserve. */
  openPlacementModal(vehicle: Vehicle): void {
    const ownedSectors: Sector[] = this.player()?.sectors ?? [];
    const dialogData: VehiclePlacementDialogData = { vehicle, ownedSectors };
    const dialogRef = this.dialog.open(VehiclePlacementModalComponent, {
      width: '420px',
      data: dialogData,
    });

    dialogRef
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((sector: Sector | null) => {
        if (sector && vehicle.id != null && sector.boardId != null && sector.number != null) {
          void this.playerService.placeVehicle(vehicle.id, sector.boardId, sector.number);
        }
      });
  }

  /** Ouvre le popup détaillé d'une unité (équipement + ordre de déplacement). */
  openUnitDialog(unit: Unit, sector: Sector): void {
    const dialogData: UnitDetailDialogData = {
      unit,
      sectorNumber: sector.number ?? 0,
      sectorName: sector.name,
    };
    this.dialog.open(UnitDetailDialogComponent, {
      width: '90vw',
      maxWidth: '1100px',
      minWidth: '320px',
      data: dialogData,
    });
  }
}
