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
import { httpResource } from '@angular/common/http';
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
import { sanitizeSvg } from '../../core/svg-sanitize';
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
  private readonly overlayPoint = signal<{ left: number; top: number } | null>(null);
  private readonly routePicker = viewChild(RoutePickerComponent);
  private readonly svgUrl = computed(() => this.activeBoard.board()?.svgOverlayUrl ?? null);
  private readonly svgText = httpResource.text(() => this.svgUrl() ?? undefined);

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
    if (!board?.mapImageUrl) return null;
    const point = this.sectorPoint();
    return { url: board.mapImageUrl, left: point?.left ?? null, top: point?.top ?? null };
  });

  private readonly sectorPoint = computed<{ left: number; top: number } | null>(() => {
    const board = this.activeBoard.board();
    if (board && this.sector.x != null && this.sector.y != null) {
      const positioned = Object.values(board.sectors).filter(
        (candidate) => candidate.x != null && candidate.y != null,
      );
      if (positioned.length) {
        const xs = positioned.map((candidate) => candidate.x!);
        const ys = positioned.map((candidate) => candidate.y!);
        const minX = Math.min(...xs);
        const maxX = Math.max(...xs);
        const minY = Math.min(...ys);
        const maxY = Math.max(...ys);
        const width = maxX - minX || 1;
        const height = maxY - minY || 1;
        return {
          left: this.clampPercent(((this.sector.x - minX) / width) * 100),
          top: this.clampPercent(((this.sector.y - minY) / height) * 100),
        };
      }
    }
    return this.overlayPoint();
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
    effect(() => {
      const text = this.svgText.hasValue() ? this.svgText.value() : null;
      if (!text || (this.sector.x != null && this.sector.y != null)) return;
      untracked(() => this.computeOverlayPoint(text));
    });
    void this.movementState.loadOrders();
    void this.sniperState.loadOrders();
  }

  private computeOverlayPoint(text: string): void {
    const sanitized = sanitizeSvg(text);
    if (!sanitized) return;

    const parsed = new DOMParser().parseFromString(sanitized, 'image/svg+xml');
    const svg = parsed.documentElement as unknown as SVGSVGElement;
    if (!svg || svg.localName.toLowerCase() !== 'svg') return;

    const target = svg.querySelector<SVGGraphicsElement>(`#path${this.sectorNumber}`);
    if (!target) return;

    const host = document.createElement('div');
    host.style.cssText = 'position:absolute;width:0;height:0;overflow:hidden;pointer-events:none';
    document.body.appendChild(host);
    host.appendChild(svg);
    try {
      const box = target.getBBox();
      const viewBox = svg.viewBox.baseVal;
      const width = viewBox?.width || svg.width.baseVal.value;
      const height = viewBox?.height || svg.height.baseVal.value;
      if (!width || !height || (!box.width && !box.height)) return;
      this.overlayPoint.set({
        left: this.clampPercent(((box.x + box.width / 2 - (viewBox?.x ?? 0)) / width) * 100),
        top: this.clampPercent(((box.y + box.height / 2 - (viewBox?.y ?? 0)) / height) * 100),
      });
    } catch {
      this.overlayPoint.set(null);
    } finally {
      host.remove();
    }
  }

  private clampPercent(value: number): number {
    return Math.min(100, Math.max(0, value));
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
