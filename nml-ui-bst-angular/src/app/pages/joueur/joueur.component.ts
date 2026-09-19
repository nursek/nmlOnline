import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  signal,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
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
import {
  Building,
  Equipment,
  PlayerAction,
  PlayerResource,
  Sector,
  Unit,
  Vehicle,
} from '../../models';
import { PlayerService } from '../../services/player.service';
import { PlayerActionsService } from '../../services/player-actions.service';
import { MovementStateService } from '../../services/movement-state.service';
import { ExchangeService } from '../../services/exchange.service';
import { AllianceStateService } from '../../services/alliance-state.service';
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
import { GroupMoveDialogComponent, GroupMoveDialogData } from './group-move-dialog.component';
import {
  VehicleCrewModalComponent,
  VehicleCrewDialogData,
  VehicleCrewDialogResult,
} from './vehicle-crew-modal.component';
import {
  SellResourceDialogComponent,
  SellResourceDialogData,
} from './sell-resource-dialog.component';
import { ExchangeDialogComponent, ExchangeDialogData } from './exchange-dialog.component';
import {
  CacheEquipmentDialogComponent,
  CacheEquipmentDialogData,
} from './cache-equipment-dialog.component';
import { BankPanelComponent } from './bank-panel.component';
import { WeaponCachePanelComponent } from './weapon-cache-panel.component';
import { HeadquartersPanelComponent } from './headquarters-panel.component';
import { movableEntities } from './movement.helpers';
import { CharacterPanelComponent } from './character-panel.component';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';
import { AllianceComponent } from '../alliance/alliance.component';
import { ExpPipe } from '../../shared/exp.pipe';
import { ResourceCardComponent } from '../../shared/resource-card/resource-card.component';
import {
  buildingStats,
  EconomyBreakdown,
  economyBreakdown,
  equipmentByClass,
  equipmentStackCost,
  incomeTotal,
  playerForces,
  SectorForces,
  totalsStats,
  troopSummaries,
  unitClassCodes,
  unitEquipmentLabel,
  unitStats,
  vehicleLabels,
  vehicleStats,
} from './joueur.helpers';
import { characterStats } from '../../core/stats';

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
    CharacterPanelComponent,
    ResourceCardComponent,
    BankPanelComponent,
    WeaponCachePanelComponent,
    HeadquartersPanelComponent,
  ],
  templateUrl: './joueur.component.html',
  styleUrls: ['./joueur.component.scss'],
})
export class JoueurComponent {
  private readonly playerService = inject(PlayerService);
  private readonly playerActionsService = inject(PlayerActionsService);
  private readonly movementState = inject(MovementStateService);
  private readonly exchangeService = inject(ExchangeService);
  private readonly allianceState = inject(AllianceStateService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
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
    void this.movementState.loadOrders();
    void this.exchangeService.loadOffers();
    void this.allianceState.loadMe();
  }

  readonly playerCharacter = computed(() => this.player()?.character ?? null);
  readonly conqueredSectors = computed(() => this.player()?.sectors ?? []);
  readonly buildings = computed(() => this.player()?.buildings ?? []);
  readonly headquarters = computed(
    () => this.buildings().find((building) => building.buildingType === 'HEADQUARTERS') ?? null,
  );
  readonly bank = computed(
    () => this.buildings().find((building) => building.buildingType === 'BANK') ?? null,
  );
  readonly weaponCache = computed(
    () =>
      this.buildings().find(
        (building) => building.buildingType === 'WEAPON_CACHE' && !building.isDestroyed,
      ) ??
      this.buildings().find((building) => building.buildingType === 'WEAPON_CACHE') ??
      null,
  );
  readonly inventoryStacks = computed(() => this.player()?.equipments ?? []);
  readonly startingMoneyRemaining = computed(
    () => this.player()?.stats?.startingMoneyRemaining ?? 0,
  );
  readonly transferableMoney = computed(() =>
    Math.max(0, (this.player()?.stats?.money ?? 0) - this.startingMoneyRemaining()),
  );
  readonly pendingExchangeOffers = this.exchangeService.pendingSentCount;
  readonly characterSector = computed(() => {
    const number = this.playerCharacter()?.sectorNumber;
    return this.conqueredSectors().find((s) => s.number === number) ?? null;
  });
  readonly income = computed(() => incomeTotal(this.conqueredSectors()));
  readonly troopSummaries = computed(() =>
    troopSummaries(this.conqueredSectors(), this.player()?.id ?? null),
  );
  readonly forces = computed(() =>
    playerForces(this.conqueredSectors(), this.player()?.id ?? null),
  );
  readonly vehicleDisplay = computed(() => {
    const labels = new Map<number, string>();
    const pilotTags = new Map<number, string>();
    const passengerTags = new Map<number, string>();
    for (const sf of this.forces().sectors) {
      const sectorLabels = vehicleLabels(sf.vehicles);
      sectorLabels.labels.forEach((label, id) => labels.set(id, label));
      sectorLabels.pilotTags.forEach((tag, id) => pilotTags.set(id, tag));
      sectorLabels.passengerTags.forEach((tag, id) => passengerTags.set(id, tag));
    }
    return { labels, pilotTags, passengerTags };
  });
  readonly movableSectorNumbers = computed<ReadonlySet<number>>(() => {
    const playerId = this.player()?.id;
    if (playerId == null) return new Set();
    const pending = this.movementState.pendingEntityIds();
    const pendingVehicles = this.movementState.pendingVehicleIds();
    const numbers = new Set<number>();
    for (const sf of this.forces().sectors) {
      const number = sf.sector.number;
      if (number == null) continue;
      const { units, character, vehicles } = movableEntities(
        sf.sector,
        playerId,
        pending,
        pendingVehicles,
      );
      if (units.length > 0 || character !== null || vehicles.length > 0) numbers.add(number);
    }
    return numbers;
  });
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
        color: 'var(--sun-deep)',
      },
      {
        label: 'Revenus',
        value: `${this.income().toFixed(0)} ₡/tour`,
        icon: 'trending_up',
        color: 'var(--forest-deep)',
      },
      {
        label: 'Puissance globale',
        value: this.forces().globalPower.toFixed(0),
        icon: 'shield',
        color: 'var(--accent)',
      },
      { label: 'Territoires', value: p.sectors.length, icon: 'place', color: 'var(--sun)' },
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
      case 'SET_VEHICLE_CREW':
        return 'groups';
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
          void this.playerService
            .moveBuilding(buildingId, sector.boardId, sector.number)
            .then((ok) => {
              if (ok) {
                void this.playerActionsService.loadActions();
                this.snackBar.open('Bâtiment déplacé', 'Fermer', { duration: 3000 });
              }
            });
        }
      });
  }

  openAllianceDialog(): void {
    this.dialog.open(AllianceComponent, {
      width: '860px',
      maxWidth: '95vw',
      maxHeight: '90vh',
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

  openGroupMove(sf: SectorForces): void {
    const playerId = this.player()?.id;
    const number = sf.sector.number;
    if (playerId == null || number == null) return;
    const data: GroupMoveDialogData = {
      sector: sf.sector,
      sectorNumber: number,
      playerId,
      allyPlayerIds: this.allianceState.alliedPlayerIds(),
    };
    this.dialog.open(GroupMoveDialogComponent, {
      width: '720px',
      maxWidth: '95vw',
      data,
    });
  }

  openVehicleCrew(vehicle: Vehicle, sector: Sector): void {
    const playerId = this.player()?.id;
    const vehicleId = vehicle.id;
    if (vehicleId == null || playerId == null) return;
    const data: VehicleCrewDialogData = { vehicle, sector, playerId };
    this.dialog
      .open(VehicleCrewModalComponent, { width: '480px', data })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result: VehicleCrewDialogResult | null) => {
        if (!result) return;
        this.applyCrew(vehicleId, result.pilotId, result.passengerIds, 'Équipage mis à jour');
      });
  }

  clearVehicleCrew(vehicle: Vehicle): void {
    const vehicleId = vehicle.id;
    if (vehicleId == null) return;
    this.applyCrew(vehicleId, null, [], 'Véhicule vidé');
  }

  private applyCrew(
    vehicleId: number,
    pilotId: number | null,
    passengerIds: number[],
    message: string,
  ): void {
    void this.playerService.setVehicleCrew(vehicleId, pilotId, passengerIds).then((updated) => {
      if (updated) {
        void this.playerActionsService.loadActions();
        this.snackBar.open(message, 'Fermer', { duration: 3000 });
      } else {
        const error = this.playerService.error();
        if (error) this.snackBar.open(error, 'Fermer', { duration: 5000 });
      }
    });
  }

  openSellDialog(resource: PlayerResource): void {
    const data: SellResourceDialogData = { resource };
    this.dialog.open(SellResourceDialogComponent, {
      width: '420px',
      maxWidth: '95vw',
      data,
    });
  }

  openExchangeDialog(): void {
    const player = this.player();
    if (player?.id == null) return;
    const data: ExchangeDialogData = {
      playerId: player.id,
      resources: player.resources ?? [],
      transferableMoney: this.transferableMoney(),
      startingMoneyRemaining: this.startingMoneyRemaining(),
    };
    this.dialog.open(ExchangeDialogComponent, { width: '640px', maxWidth: '95vw', data });
  }

  openCacheEquipments(): void {
    const cacheId = this.weaponCache()?.id;
    if (cacheId == null) return;
    const data: CacheEquipmentDialogData = { buildingId: cacheId };
    this.dialog.open(CacheEquipmentDialogComponent, {
      width: '560px',
      maxWidth: '95vw',
      data,
    });
  }

  goToShop(): void {
    void this.router.navigate(['/boutique'], { queryParams: { tab: 'revente' } });
  }
}
