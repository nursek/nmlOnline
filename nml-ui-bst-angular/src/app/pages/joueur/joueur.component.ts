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
import { MatTabChangeEvent, MatTabsModule } from '@angular/material/tabs';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Building, Equipment, PlayerAction, Sector, Unit, Vehicle } from '../../models';
import { PlayerService } from '../../services/player.service';
import { PlayerActionsService } from '../../services/player-actions.service';
import { slugify } from '../../core/slug';
import {
  VehiclePlacementModalComponent,
  VehiclePlacementDialogData,
} from './vehicle-placement-modal.component';
import {
  BuildingMoveModalComponent,
  BuildingMoveDialogData,
} from './building-move-modal.component';
import { UnitDetailDialogComponent, UnitDetailDialogData } from './unit-detail-dialog.component';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';
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
    MatSnackBarModule,
  ],
  templateUrl: './joueur.component.html',
  styleUrls: ['./joueur.component.scss'],
})
export class JoueurComponent {
  private readonly playerService = inject(PlayerService);
  private readonly playerActionsService = inject(PlayerActionsService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);

  readonly player = this.playerService.player;
  readonly loading = this.playerService.loading;
  readonly error = this.playerService.error;
  readonly undeployedVehicles = this.playerService.undeployedVehicles;
  readonly vehiclesLoading = this.playerService.vehiclesLoading;
  readonly currentTurn = this.playerService.currentTurn;
  readonly actions = this.playerActionsService.actions;
  readonly actionsLoading = this.playerActionsService.loading;
  readonly actionsError = this.playerActionsService.error;

  constructor() {
    void this.playerService.loadCurrent();
    void this.playerService.loadVehicles();
    void this.playerService.loadCurrentTurn();
    void this.playerActionsService.loadActions();
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
    this.forces().sectors.reduce((n, sf) => n + sf.vehicles.length, 0),
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

  classCodes(u: Unit): string {
    return unitClassCodes(u);
  }

  equipmentLabel(u: Unit): string {
    return unitEquipmentLabel(u);
  }

  actionIcon(type: PlayerAction['type']): string {
    switch (type) {
      case 'BUY_EQUIPMENT':
        return 'shopping_cart';
      case 'SELL_RESOURCE':
        return 'sell';
      case 'EQUIP_UNIT':
        return 'build';
      case 'UNEQUIP_UNIT':
        return 'build_circle';
      case 'BUY_VEHICLE':
        return 'directions_car';
      case 'PLACE_VEHICLE':
        return 'place';
      case 'MOVE_BUILDING':
        return 'move_up';
    }
  }

  unitStats = unitStats;
  characterStats = characterStats;
  buildingStats = buildingStats;
  vehicleStats = vehicleStats;
  totalsStats = totalsStats;
  stackCost = equipmentStackCost;

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

  openBuildingMove(building: Building): void {
    const buildingId = building.id;
    if (buildingId == null) return;
    const ownedSectors: Sector[] = this.player()?.sectors ?? [];
    const data: BuildingMoveDialogData = { building, ownedSectors };
    this.dialog
      .open(BuildingMoveModalComponent, { width: '420px', data })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((sector: Sector | null) => {
        if (sector?.boardId != null && sector.number != null) {
          void this.playerService.moveBuilding(buildingId, sector.boardId, sector.number).then((ok) => {
            if (ok) {
              void this.playerActionsService.loadActions();
              this.snackBar.open('Bâtiment déplacé', 'Fermer', { duration: 3000 });
            }
          });
        }
      });
  }

  undoFrom(action: PlayerAction): void {
    this.confirmAndUndo(
      'Annuler les actions',
      `Annuler « ${action.label} » et toutes les actions suivantes de ce tour ?`,
      () => this.playerActionsService.undoFrom(action.id),
    );
  }

  undoAll(): void {
    this.confirmAndUndo(
      'Annuler toutes les actions',
      'Annuler toutes les actions de ce tour ?',
      () => this.playerActionsService.undoAll(),
    );
  }

  // Index 1 = onglet « Actions » (cf. template).
  onTabChange(event: MatTabChangeEvent): void {
    if (event.index === 1) void this.playerActionsService.loadActions();
  }

  private confirmAndUndo(title: string, message: string, undo: () => Promise<boolean>): void {
    this.dialog
      .open(ConfirmDialogComponent, { data: { title, message, confirmLabel: 'Confirmer' } })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((confirmed) => {
        if (!confirmed) return;
        void undo().then((ok) => {
          if (ok) this.snackBar.open('Actions annulées', 'Fermer', { duration: 3000 });
        });
      });
  }
}
