import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { GameCharacter, Sector } from '../../models';
import { ActiveBoardService } from '../../services/active-board.service';
import { MovementStateService } from '../../services/movement-state.service';
import { ExpPipe } from '../../shared/exp.pipe';
import { groupMaxHops, movableEntities, unitMaxHops } from '../../core/movement.helpers';
import { RoutePickerComponent } from './route-picker.component';
import { SectorMapPreviewComponent } from './sector-map-preview.component';

export interface GroupMoveDialogData {
  sector: Sector;
  sectorNumber: number;
  playerId: number;
  allyPlayerIds?: ReadonlySet<number>;
}

@Component({
  selector: 'app-group-move-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ExpPipe,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatIconModule,
    RoutePickerComponent,
    SectorMapPreviewComponent,
  ],
  templateUrl: './group-move-dialog.component.html',
  styleUrls: ['./group-move-dialog.component.scss'],
})
export class GroupMoveDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<GroupMoveDialogComponent>);
  readonly data = inject<GroupMoveDialogData>(MAT_DIALOG_DATA);
  private readonly movementState = inject(MovementStateService);
  private readonly snackBar = inject(MatSnackBar);

  readonly busy = signal(false);
  readonly route = signal<number[]>([]);
  readonly frontOverlay = signal(false);
  readonly resourcesOverlay = signal(false);
  readonly allyPlayerIds = this.data.allyPlayerIds ?? new Set<number>();
  /** Entités décochées ; la sélection par défaut couvre tout ce qui est déplaçable. */
  private readonly deselectedIds = signal<ReadonlySet<number>>(new Set());

  readonly unitHops = unitMaxHops;

  // Board complète (secteurs neutres/ennemis inclus) pour l'adjacence et la portée.
  private readonly activeBoard = inject(ActiveBoardService);
  readonly allSectors = this.activeBoard.sectors;

  /** Déplaçables = propriété stricte + hors ordres PENDING (mis à jour après loadOrders). */
  readonly movable = computed(() =>
    movableEntities(
      this.data.sector,
      this.data.playerId,
      this.movementState.pendingEntityIds(),
      this.movementState.pendingVehicleIds(),
    ),
  );

  readonly selectedUnits = computed(() =>
    this.movable().units.filter((u) => !this.deselectedIds().has(u.id)),
  );

  readonly selectedCharacter = computed<GameCharacter | null>(() => {
    const character = this.movable().character;
    return character?.id != null && !this.deselectedIds().has(character.id) ? character : null;
  });

  readonly selectedVehicles = computed(() =>
    this.movable().vehicles.filter((v) => v.id != null && !this.deselectedIds().has(v.id)),
  );

  readonly selectedCount = computed(
    () =>
      this.selectedUnits().length +
      (this.selectedCharacter() ? 1 : 0) +
      this.selectedVehicles().length,
  );

  readonly totalMovableCount = computed(
    () =>
      this.movable().units.length +
      (this.movable().character ? 1 : 0) +
      this.movable().vehicles.length,
  );

  readonly maxHops = computed(() =>
    groupMaxHops(this.selectedUnits(), this.selectedCharacter(), this.selectedVehicles()),
  );

  readonly limitingEntity = computed<string | null>(() => {
    const candidates: { label: string; hops: number }[] = [];
    for (const unit of this.selectedUnits()) {
      candidates.push({ label: `${unit.type.name} n°${unit.number}`, hops: unitMaxHops(unit) });
    }
    const character = this.selectedCharacter();
    if (character) candidates.push({ label: character.name, hops: 1 });
    for (const vehicle of this.selectedVehicles()) {
      candidates.push({ label: vehicle.displayName, hops: vehicle.speed });
    }
    if (!candidates.length) return null;
    const min = Math.min(...candidates.map((candidate) => candidate.hops));
    return candidates.find((candidate) => candidate.hops === min)?.label ?? null;
  });

  constructor() {
    void this.movementState.loadOrders();
  }

  isSelected(id: number): boolean {
    return !this.deselectedIds().has(id);
  }

  toggle(id: number, checked: boolean): void {
    this.deselectedIds.update((set) => {
      const next = new Set(set);
      next[checked ? 'delete' : 'add'](id);
      return next;
    });
  }

  selectAll(): void {
    this.deselectedIds.set(new Set());
  }

  clearSelection(): void {
    const ids = new Set<number>(this.movable().units.map((u) => u.id));
    const character = this.movable().character;
    if (character?.id != null) ids.add(character.id);
    this.movable().vehicles.forEach((v) => {
      if (v.id != null) ids.add(v.id);
    });
    this.deselectedIds.set(ids);
  }

  selectLightUnitsOnly(): void {
    const deselected = new Set<number>();
    for (const unit of this.movable().units) {
      if (unitMaxHops(unit) <= 1) deselected.add(unit.id);
    }
    const character = this.movable().character;
    if (character?.id != null) deselected.add(character.id);
    for (const vehicle of this.movable().vehicles) {
      if (vehicle.id != null) deselected.add(vehicle.id);
    }
    this.deselectedIds.set(deselected);
  }

  selectVehiclesOnly(): void {
    const deselected = new Set<number>();
    for (const unit of this.movable().units) deselected.add(unit.id);
    const character = this.movable().character;
    if (character?.id != null) deselected.add(character.id);
    this.deselectedIds.set(deselected);
  }

  async submit(): Promise<void> {
    const route = this.route();
    const entityIds = this.selectedUnits().map((u) => u.id);
    const character = this.selectedCharacter();
    if (character?.id != null) entityIds.push(character.id);
    const vehicleIds = this.selectedVehicles()
      .map((v) => v.id)
      .filter((id): id is number => id != null);

    if (route.length < 2 || (entityIds.length === 0 && vehicleIds.length === 0)) return;

    this.busy.set(true);
    try {
      let movedCount = 0;
      let ok = true;
      if (entityIds.length > 0) {
        const order = await this.movementState.placeFootOrder(entityIds, route);
        if (order) {
          movedCount += entityIds.length;
        } else {
          ok = false;
        }
      }
      for (const vehicleId of vehicleIds) {
        const order = await this.movementState.placeVehicleOrder(vehicleId, route);
        if (order) {
          movedCount += 1;
        } else {
          ok = false;
        }
      }

      if (!ok) {
        const error = this.movementState.error();
        if (error) this.snackBar.open(error, 'Fermer', { duration: 5000 });
        return;
      }

      this.snackBar.open(
        `Ordre groupé : secteur ${route.join(' → ')} (${movedCount} entité(s))`,
        'OK',
        { duration: 3000 },
      );
      this.dialogRef.close(true);
    } finally {
      this.busy.set(false);
    }
  }

  close(): void {
    this.dialogRef.close(false);
  }
}
