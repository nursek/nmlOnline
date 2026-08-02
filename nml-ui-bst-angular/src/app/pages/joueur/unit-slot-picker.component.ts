import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Equipment } from '../../models';
import { PlayerService } from '../../services/player.service';

export interface UnitSlotPickerData {
  unitId: number;
  /** Catégorie filtrée : 'FIREARM' | 'MELEE' | 'DEFENSIVE'. */
  category: string;
  categoryLabel: string;
}

/**
 * Sélecteur d'équipement pour un slot de catégorie (nested MatDialog) :
 * liste scrollable des équipements éligibles de l'inventaire + inéligibles grisés.
 * L'assignation se fait via PlayerService.assignUnitEquipment (filtre par catégorie).
 */
@Component({
  selector: 'app-unit-slot-picker',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './unit-slot-picker.component.html',
  styleUrls: ['./unit-slot-picker.component.scss'],
})
export class UnitSlotPickerComponent {
  private readonly dialogRef = inject(MatDialogRef<UnitSlotPickerComponent>);
  readonly data: UnitSlotPickerData = inject(MAT_DIALOG_DATA);
  private readonly playerService = inject(PlayerService);
  private readonly snackBar = inject(MatSnackBar);

  readonly player = this.playerService.player;
  readonly busy = signal(false);

  /** Équipements éligibles de la catégorie (dispo > 0 + compat classe + cap non pleine). */
  readonly eligible = computed(() => {
    const p = this.player();
    if (!p?.equipments) return [];
    return p.equipments.filter(
      (st) =>
        st.available > 0 &&
        st.equipment.category === this.data.category &&
        this.isEligible(st.equipment),
    );
  });

  /** Inéligibles (dispo > 0, catégorie OK mais classe/cap bloqué) — grisés avec raison. */
  readonly ineligible = computed(() => {
    const p = this.player();
    if (!p?.equipments) return [];
    return p.equipments.filter(
      (st) =>
        st.available > 0 &&
        st.equipment.category === this.data.category &&
        !this.isEligible(st.equipment),
    );
  });

  private isEligible(eq: Equipment): boolean {
    const p = this.player();
    if (!p) return false;
    const unit = p.sectors.flatMap((s) => s.army ?? []).find((u) => u.id === this.data.unitId);
    if (!unit) return false;
    const compat = (eq.compatibleClass ?? []).some((c) =>
      (unit.classes ?? []).some((uc) => uc.name === c.name),
    );
    if (!compat) return false;
    const cap = this.capFor(
      unit.type?.maxFirearms ?? 0,
      unit.type?.maxMeleeWeapons ?? 0,
      unit.type?.maxDefensiveEquipment ?? 0,
    );
    const current = (unit.equipments ?? []).filter((e) => e.category === eq.category).length;
    return current < cap;
  }

  private capFor(maxFirearms: number, maxMelee: number, maxDef: number): number {
    switch (this.data.category) {
      case 'FIREARM':
        return maxFirearms;
      case 'MELEE':
        return maxMelee;
      case 'DEFENSIVE':
        return maxDef;
      default:
        return 0;
    }
  }

  ineligibleReason(eq: Equipment): string {
    const p = this.player();
    const unit = p?.sectors.flatMap((s) => s.army ?? []).find((u) => u.id === this.data.unitId);
    if (!unit) return '';
    const compat = (eq.compatibleClass ?? []).some((c) =>
      (unit.classes ?? []).some((uc) => uc.name === c.name),
    );
    if (!compat) return 'Classe incompatible';
    const cap = this.capFor(
      unit.type?.maxFirearms ?? 0,
      unit.type?.maxMeleeWeapons ?? 0,
      unit.type?.maxDefensiveEquipment ?? 0,
    );
    const current = (unit.equipments ?? []).filter((e) => e.category === eq.category).length;
    if (current >= cap) return 'Catégorie pleine';
    return 'Indisponible';
  }

  async equip(eq: Equipment): Promise<void> {
    if (this.busy()) return;
    this.busy.set(true);
    try {
      const updated = await this.playerService.assignUnitEquipment(this.data.unitId, eq.name);
      if (updated) {
        this.snackBar.open(`${eq.name} équipé`, 'OK', { duration: 2500 });
        this.dialogRef.close(true);
      }
    } finally {
      this.busy.set(false);
    }
  }

  close(): void {
    this.dialogRef.close();
  }
}
