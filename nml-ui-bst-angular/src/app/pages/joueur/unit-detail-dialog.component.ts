import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { httpResource } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import {
  MatDialog,
  MatDialogModule,
  MatDialogRef,
  MAT_DIALOG_DATA,
} from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Board, Unit, Equipment } from '../../models';
import { environment } from '../../../environments/environment';
import { PlayerService } from '../../services/player.service';
import { MovementStateService } from '../../services/movement-state.service';
import { UnitSlotPickerComponent, UnitSlotPickerData } from './unit-slot-picker.component';
import { ExpPipe } from '../../shared/exp.pipe';

export interface UnitDetailDialogData {
  /** Snapshot initial — l'unité live est re-résolue depuis PlayerService.player(). */
  unit: Unit;
  sectorNumber: number;
  sectorName: string;
}

/** Groupe de slots pour une catégorie d'équipement (cap + équipements remplis). */
export interface SlotGroup {
  category: string;
  label: string;
  cap: number;
  filled: Equipment[];
}

/** slots offensifs | stats + portrait | slots défensifs, ordre de déplacement en bas. */
@Component({
  selector: 'app-unit-detail-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ExpPipe,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule,
    MatTooltipModule,
  ],
  templateUrl: './unit-detail-dialog.component.html',
  styleUrls: ['./unit-detail-dialog.component.scss'],
})
export class UnitDetailDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<UnitDetailDialogComponent>);
  readonly data: UnitDetailDialogData = inject(MAT_DIALOG_DATA);
  private readonly playerService = inject(PlayerService);
  private readonly movementState = inject(MovementStateService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  readonly player = this.playerService.player;

  // Board complète (secteurs neutres inclus) — nécessaire pour résoudre l'adjacence
  // des secteurs non possédés par le joueur lors du calcul des routes de déplacement.
  // Une seule board active (convention boards[0], cf. carte.component).
  private readonly boardsRef = httpResource<Board[]>(() => ({
    url: `${environment.apiBaseUrl}/boards`,
  }));
  readonly allSectors = computed(() => Object.values(this.boardsRef.value()?.[0]?.sectors ?? {}));

  constructor() {
    // imgError doit se reset dès que portraitUrl se recalcule (ex: unité évolue
    // et change de type → nouvelle URL potentiellement valide). Sans cela, un
    // 1er échec d'image verrouillerait le fallback même après recompute valide.
    effect(() => {
      this.portraitUrl();
      this.imgError.set(false);
    });
  }

  readonly imgError = signal(false);
  readonly busy = signal(false);
  readonly targetSector = signal<number | null>(null);

  /** Unité live re-lue depuis le player signal (rafraîchi après équipement). */
  readonly unit = computed<Unit>(() => {
    const p = this.player();
    const snapshot = this.data.unit;
    if (!p?.sectors) return snapshot;
    const sec = p.sectors.find((s) => s.number === this.data.sectorNumber);
    const live = sec?.army?.find((u) => u.id === snapshot.id);
    return live ?? snapshot;
  });

  readonly portraitUrl = computed<string>(() => {
    const p = this.player();
    const u = this.unit();
    if (!p?.name || !u?.type.name) return '';
    return `assets/${p.name.toLowerCase()}/units/${u.type.name.toLowerCase()}/portrait.png`;
  });

  readonly maxHops = computed<number>(() => {
    const classes = this.unit()?.classes ?? [];
    if (classes.length === 0) return 1;
    return classes.reduce((m, c) => Math.max(m, c.maxMovementHops ?? 1), 1);
  });

  /** Voisins directs du secteur de départ (adjacence côté serveur via SectorDto.neighbors). */
  readonly neighbors = computed<number[]>(() => {
    const sec = this.allSectors().find((s) => s.number === this.data.sectorNumber);
    return sec?.neighbors ?? [];
  });

  /**
   * Secteurs atteignables en <= maxHops (route adjacente). BFS traversant aussi
   * les secteurs neutres (non possédés) — règle : le 1er hop peut cibler un
   * quartier neutre, et un 2e hop peut traverser un secteur neutre comme mid.
   */
  readonly reachableTargets = computed<number[]>(() => {
    const all = this.allSectors();
    const from = this.data.sectorNumber;
    const max = this.maxHops();
    const sectorOf = (n: number) => all.find((s) => s.number === n) ?? null;

    const result = new Set<number>();
    let frontier = new Set<number>(this.neighbors());
    frontier.forEach((n) => result.add(n));
    for (let h = 2; h <= max; h++) {
      const next = new Set<number>();
      for (const n of frontier) {
        const s = sectorOf(n);
        for (const nn of s?.neighbors ?? []) {
          if (nn !== from && !result.has(nn)) next.add(nn);
        }
      }
      next.forEach((n) => result.add(n));
      frontier = next;
    }
    return [...result].sort((a, b) => a - b);
  });

  /** Groupe de slots pour une catégorie (cap + équipements actuels positionnels). */
  readonly slotGroups = computed<SlotGroup[]>(() => {
    const u = this.unit();
    if (!u?.type) return [];
    return [
      this.buildGroup('MELEE', 'Mêlée', u.type.maxMeleeWeapons ?? 0, u),
      this.buildGroup('FIREARM', 'Distance', u.type.maxFirearms ?? 0, u),
      this.buildGroup('DEFENSIVE', 'Défensif', u.type.maxDefensiveEquipment ?? 0, u),
    ].filter((g) => g.cap > 0);
  });

  /** Slots offensifs (Mêlée + Distance) — colonne gauche. */
  readonly offensiveSlots = computed<SlotGroup[]>(() =>
    this.slotGroups().filter((g) => g.category === 'MELEE' || g.category === 'FIREARM'),
  );

  /** Slots défensifs — colonne droite. */
  readonly defensiveSlots = computed<SlotGroup[]>(() =>
    this.slotGroups().filter((g) => g.category === 'DEFENSIVE'),
  );

  private buildGroup(category: string, label: string, cap: number, u: Unit): SlotGroup {
    const filled = (u.equipments ?? []).filter((e) => e.category === category);
    return { category, label, cap, filled };
  }

  /** Index des slots vides d'un groupe (cap - remplis) pour rendu `@for`. */
  emptySlots(group: SlotGroup): number[] {
    const count = Math.max(0, group.cap - group.filled.length);
    return Array.from({ length: count }, (_, i) => i);
  }

  /** Ouvre le sélecteur d'équipement pour une catégorie de slot. */
  openSlotPicker(group: SlotGroup): void {
    const pickerData: UnitSlotPickerData = {
      unitId: this.unit().id,
      category: group.category,
      categoryLabel: group.label,
    };
    this.dialog.open(UnitSlotPickerComponent, {
      width: '420px',
      maxWidth: '90vw',
      data: pickerData,
    });
  }

  readonly pendingOrdersForUnit = computed(() =>
    this.movementState.orders().filter((o) => (o.entityIds ?? []).includes(this.unit()?.id ?? -1)),
  );

  // === Actions ===

  async unequip(eq: Equipment): Promise<void> {
    if (this.busy()) return;
    this.busy.set(true);
    try {
      const updated = await this.playerService.removeUnitEquipment(this.unit().id, eq.name);
      if (updated) {
        this.snackBar.open(`${eq.name} retiré`, 'OK', { duration: 2500 });
      }
    } finally {
      this.busy.set(false);
    }
  }

  async submitMove(): Promise<void> {
    const from = this.data.sectorNumber;
    const to = this.targetSector();
    if (to === null || to === from) return;
    const route = this.computeRoute(from, to);
    if (route.length < 2) {
      this.snackBar.open('Aucune route adjacente trouvée vers la destination', 'Fermer', {
        duration: 3000,
      });
      return;
    }
    this.busy.set(true);
    try {
      const order = await this.movementState.placeFootOrder(this.unit().id, route);
      if (order) {
        this.snackBar.open(`Ordre: secteur ${from} -> ${to}`, 'OK', { duration: 2500 });
        this.targetSector.set(null);
      } else {
        const err = this.movementState.error();
        if (err) this.snackBar.open(err, 'Fermer', { duration: 5000 });
      }
    } finally {
      this.busy.set(false);
    }
  }

  async cancelOrder(orderId: number): Promise<void> {
    if (this.busy()) return;
    this.busy.set(true);
    try {
      await this.movementState.cancelOrder(orderId);
    } finally {
      this.busy.set(false);
    }
  }

  close(): void {
    this.dialogRef.close();
  }

  onImgError(): void {
    this.imgError.set(true);
  }

  // === Helpers ===

  /** Construit une route adjacente from->to d'au plus maxHops (BFS borné). */
  private computeRoute(from: number, to: number): number[] {
    if (from === to) return [];
    const all = this.allSectors();
    const neighborsOf = (n: number) => all.find((s) => s.number === n)?.neighbors ?? [];
    const max = this.maxHops();

    if (this.neighbors().includes(to)) return [from, to];
    if (max < 2) return [];

    // 2 hops : chercher un mid commun (voisin de from ET de to).
    const fromNs = new Set(this.neighbors());
    for (const mid of fromNs) {
      const midNs = neighborsOf(mid);
      if (midNs.includes(to)) {
        return [from, mid, to];
      }
    }
    return [];
  }
}
