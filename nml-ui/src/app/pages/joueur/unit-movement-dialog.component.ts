import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { GameCharacter, MovementOrder, Sector, Unit } from '../../models';
import { ActiveBoardService } from '../../services/active-board.service';
import { MovementStateService } from '../../services/movement-state.service';
import { PlayerDirectoryService } from '../../services/player-directory.service';
import { SniperOrderStateService } from '../../services/sniper-order-state.service';
import { ExpPipe } from '../../shared/exp.pipe';
import { sectorKind, sectorKindLabel, unitMaxHops } from '../../core/movement.helpers';
import { RoutePickerComponent } from './route-picker.component';
import { routeAfterSectorClick } from './route-picker.helpers';
import { SectorMapPreviewComponent } from './sector-map-preview.component';

export interface UnitMovementDialogData {
  unit?: Unit;
  character?: GameCharacter;
  sector: Sector;
  playerId: number;
  allyPlayerIds?: ReadonlySet<number>;
}

@Component({
  selector: 'app-unit-movement-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ExpPipe,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatTooltipModule,
    RoutePickerComponent,
    SectorMapPreviewComponent,
  ],
  templateUrl: './unit-movement-dialog.component.html',
  styleUrls: ['./unit-movement-dialog.component.scss'],
})
export class UnitMovementDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<UnitMovementDialogComponent>);
  readonly data = inject<UnitMovementDialogData>(MAT_DIALOG_DATA);
  private readonly movementState = inject(MovementStateService);
  private readonly sniperState = inject(SniperOrderStateService);
  private readonly playerDirectory = inject(PlayerDirectoryService);
  private readonly activeBoard = inject(ActiveBoardService);
  private readonly snackBar = inject(MatSnackBar);

  readonly unit = this.data.unit ?? null;
  readonly character = this.data.character ?? null;
  readonly sector = this.data.sector;
  readonly sectorNumber = this.data.sector.number ?? 0;
  readonly allyPlayerIds = this.data.allyPlayerIds ?? new Set<number>();
  readonly entityId = this.unit?.id ?? this.character?.id ?? null;
  readonly entityLabel = computed(() =>
    this.unit
      ? `${this.unit.type.name} n°${this.unit.number}`
      : (this.character?.name ?? 'Personnage'),
  );

  readonly busy = signal(false);
  readonly route = signal<number[]>([]);
  private readonly routePicker = viewChild(RoutePickerComponent);

  readonly allSectors = this.activeBoard.sectors;
  readonly maxHops = computed(() => (this.unit ? unitMaxHops(this.unit) : 1));

  readonly pendingOrdersForUnit = computed(() => {
    const entityId = this.entityId;
    if (entityId == null) return [];
    return this.movementState
      .orders()
      .filter((order) => (order.entityIds ?? []).includes(entityId));
  });
  readonly pendingSniperOrdersForUnit = computed(() => {
    const entityId = this.entityId;
    if (entityId == null) return [];
    return this.sniperState.orders().filter((order) => order.unitId === entityId);
  });

  readonly ownerName = computed(() => {
    const ownerId = this.sector.ownerId;
    if (ownerId == null) return 'Territoire neutre';
    return (
      this.playerDirectory.players().find((player) => player.id === ownerId)?.name ??
      `Joueur #${ownerId}`
    );
  });
  readonly ownerColor = computed(() => {
    const ownerId = this.sector.ownerId;
    return ownerId == null
      ? 'var(--muted)'
      : (this.playerDirectory.colorMap().get(ownerId) ?? 'var(--muted)');
  });
  readonly sectorKindText = computed(() =>
    sectorKindLabel(sectorKind(this.sector, this.data.playerId, this.allyPlayerIds)),
  );

  readonly canSubmit = computed(
    () =>
      this.entityId != null &&
      this.route().length >= 2 &&
      !this.busy() &&
      this.pendingOrdersForUnit().length === 0 &&
      this.pendingSniperOrdersForUnit().length === 0,
  );

  constructor() {
    void this.movementState.loadOrders();
    void this.sniperState.loadOrders();
  }

  onMapSectorClick(sectorNumber: number): void {
    if (
      this.busy() ||
      this.pendingOrdersForUnit().length > 0 ||
      this.pendingSniperOrdersForUnit().length > 0
    ) {
      return;
    }
    const result = routeAfterSectorClick(
      this.route(),
      sectorNumber,
      this.sectorNumber,
      this.allSectors(),
      this.maxHops(),
    );
    if (result.error) {
      this.snackBar.open(result.error, 'Fermer', { duration: 2500 });
      return;
    }
    this.route.set(result.route);
    this.routePicker()?.setRoute(result.route);
  }

  async submitMove(): Promise<void> {
    const entityId = this.entityId;
    if (!this.canSubmit() || entityId == null) return;
    const route = this.route();
    this.busy.set(true);
    try {
      const order = await this.movementState.placeFootOrder([entityId], route);
      if (order) {
        this.snackBar.open(`Ordre : secteur ${route.join(' → ')}`, 'OK', { duration: 2500 });
        this.dialogRef.close(true);
      } else {
        const error = this.movementState.error();
        if (error) this.snackBar.open(error, 'Fermer', { duration: 5000 });
      }
    } finally {
      this.busy.set(false);
    }
  }

  async editOrder(order: MovementOrder): Promise<void> {
    if (this.busy()) return;
    this.busy.set(true);
    try {
      const ok = await this.movementState.cancelOrder(order.id);
      if (!ok) {
        const error = this.movementState.error();
        if (error) this.snackBar.open(error, 'Fermer', { duration: 5000 });
        return;
      }
      this.route.set(order.route);
      this.routePicker()?.setRoute(order.route);
      this.snackBar.open('Ordre annulé — modifiez le chemin', 'OK', { duration: 2500 });
    } finally {
      this.busy.set(false);
    }
  }

  async cancelOrder(orderId: number): Promise<void> {
    if (this.busy()) return;
    this.busy.set(true);
    try {
      const ok = await this.movementState.cancelOrder(orderId);
      if (!ok) {
        const error = this.movementState.error();
        if (error) this.snackBar.open(error, 'Fermer', { duration: 5000 });
        return;
      }
      this.routePicker()?.reset();
      this.route.set([]);
      this.snackBar.open('Ordre annulé', 'OK', { duration: 2500 });
    } finally {
      this.busy.set(false);
    }
  }

  close(): void {
    this.dialogRef.close(false);
  }
}
