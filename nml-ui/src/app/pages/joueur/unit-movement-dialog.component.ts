import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
  untracked,
  viewChild,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MovementOrder, Sector, Unit } from '../../models';
import { ActiveBoardService } from '../../services/active-board.service';
import { MovementStateService } from '../../services/movement-state.service';
import { PlayerDirectoryService } from '../../services/player-directory.service';
import { SniperOrderStateService } from '../../services/sniper-order-state.service';
import { ExpPipe } from '../../shared/exp.pipe';
import { sectorKind, sectorKindLabel, unitMaxHops } from '../../core/movement.helpers';
import { resourceIconSrc } from '../carte/carte.helpers';
import { RoutePickerComponent } from './route-picker.component';

export interface UnitMovementDialogData {
  unit: Unit;
  sector: Sector;
  playerId: number;
  allyPlayerIds?: ReadonlySet<number>;
}

@Component({
  selector: 'app-unit-movement-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe,
    ExpPipe,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatTooltipModule,
    RoutePickerComponent,
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

  readonly unit = this.data.unit;
  readonly sector = this.data.sector;
  readonly sectorNumber = this.data.sector.number ?? 0;
  private readonly allyIds = this.data.allyPlayerIds ?? new Set<number>();

  readonly busy = signal(false);
  readonly route = signal<number[]>([]);
  readonly mapError = signal(false);
  private readonly routePicker = viewChild(RoutePickerComponent);

  readonly allSectors = this.activeBoard.sectors;
  readonly maxHops = computed(() => unitMaxHops(this.unit));

  readonly pendingOrdersForUnit = computed(() =>
    this.movementState.orders().filter((order) => (order.entityIds ?? []).includes(this.unit.id)),
  );
  readonly pendingSniperOrdersForUnit = computed(() =>
    this.sniperState.orders().filter((order) => order.unitId === this.unit.id),
  );

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
    sectorKindLabel(sectorKind(this.sector, this.data.playerId, this.allyIds)),
  );
  readonly income = computed(() => this.sector.income ?? 0);
  readonly resourceIcon = computed(() =>
    this.sector.resource ? resourceIconSrc(this.sector.resource) : null,
  );
  readonly forceCounts = computed(() => {
    const parts: string[] = [];
    if (this.sector.character) parts.push(`${this.sector.character.name} (personnage)`);
    if (this.sector.army?.length) parts.push(`${this.sector.army.length} unité(s)`);
    if (this.sector.vehicles?.length) parts.push(`${this.sector.vehicles.length} véhicule(s)`);
    if (this.sector.buildings?.length) parts.push(`${this.sector.buildings.length} bâtiment(s)`);
    return parts.length ? parts.join(' · ') : 'Aucune force';
  });

  readonly mapPreview = computed(() => {
    const board = this.activeBoard.board();
    if (!board?.mapImageUrl || this.sector.x == null || this.sector.y == null) return null;
    const positioned = Object.values(board.sectors).filter(
      (candidate) => candidate.x != null && candidate.y != null,
    );
    if (!positioned.length) return null;

    const xs = positioned.map((candidate) => candidate.x!);
    const ys = positioned.map((candidate) => candidate.y!);
    const minX = Math.min(...xs);
    const maxX = Math.max(...xs);
    const minY = Math.min(...ys);
    const maxY = Math.max(...ys);
    const width = maxX - minX || 1;
    const height = maxY - minY || 1;

    return {
      url: board.mapImageUrl,
      left: Math.min(100, Math.max(0, ((this.sector.x - minX) / width) * 100)),
      top: Math.min(100, Math.max(0, ((this.sector.y - minY) / height) * 100)),
    };
  });

  readonly canSubmit = computed(
    () =>
      this.route().length >= 2 &&
      !this.busy() &&
      this.pendingOrdersForUnit().length === 0 &&
      this.pendingSniperOrdersForUnit().length === 0,
  );

  constructor() {
    effect(() => {
      this.mapPreview();
      untracked(() => this.mapError.set(false));
    });
    void this.movementState.loadOrders();
    void this.sniperState.loadOrders();
  }

  async submitMove(): Promise<void> {
    if (!this.canSubmit()) return;
    const route = this.route();
    this.busy.set(true);
    try {
      const order = await this.movementState.placeFootOrder([this.unit.id], route);
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
