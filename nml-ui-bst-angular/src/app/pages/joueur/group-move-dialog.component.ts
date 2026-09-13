import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { GameCharacter, Sector, Vehicle } from '../../models';
import { ActiveBoardService } from '../../services/active-board.service';
import { MovementStateService } from '../../services/movement-state.service';
import { ExpPipe } from '../../shared/exp.pipe';
import {
  findRoute,
  groupMaxHops,
  movableEntities,
  reachableTargets,
  sectorKind,
  SectorKind,
  unitMaxHops,
} from './movement.helpers';

export interface GroupMoveDialogData {
  sector: Sector;
  sectorNumber: number;
  playerId: number;
}

interface MoveTarget {
  number: number;
  name: string;
  kind: SectorKind;
}

const KIND_LABELS: Record<SectorKind, string> = {
  own: 'Interne',
  neutral: 'Neutre',
  enemy: 'Ennemi',
  unknown: 'Inconnu',
};

@Component({
  selector: 'app-group-move-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ExpPipe,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatSelectModule,
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
  readonly targetSector = signal<number | null>(null);
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

  readonly reachableTargets = computed(() =>
    reachableTargets(this.allSectors(), this.data.sectorNumber, this.maxHops()),
  );

  readonly targets = computed<MoveTarget[]>(() =>
    this.reachableTargets().map((number) => {
      const sector = this.allSectors().find((s) => s.number === number) ?? null;
      return {
        number,
        name: sector?.name ?? `Secteur ${number}`,
        kind: sectorKind(sector, this.data.playerId),
      };
    }),
  );

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

  kindLabel(kind: SectorKind): string {
    return KIND_LABELS[kind];
  }

  async submit(): Promise<void> {
    const to = this.targetSector();
    const entityIds = this.selectedUnits().map((u) => u.id);
    const character = this.selectedCharacter();
    if (character?.id != null) entityIds.push(character.id);
    const vehicleIds = this.selectedVehicles()
      .map((v) => v.id)
      .filter((id): id is number => id != null);

    if (to === null || (entityIds.length === 0 && vehicleIds.length === 0)) return;

    const route = findRoute(this.allSectors(), this.data.sectorNumber, to, this.maxHops());
    if (route.length < 2) {
      this.snackBar.open('Aucune route adjacente trouvée vers la destination', 'Fermer', {
        duration: 3000,
      });
      return;
    }

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
        `Ordre groupé : secteur ${this.data.sectorNumber} → ${to} (${movedCount} entité(s))`,
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
