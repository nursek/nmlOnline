import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import type { Equipment } from '../../models';
import { equipmentLabel as equipmentLabelFor } from '../../core/equipment-label';
import { PlayerService } from '../../services/player.service';
import { allEquipmentStacks } from './joueur.helpers';

type AutoEquipCategory = 'FIREARM' | 'MELEE' | 'DEFENSIVE';

@Component({
  selector: 'app-auto-equip-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatDialogModule, MatButtonModule, MatIconModule, MatCheckboxModule],
  templateUrl: './auto-equip-dialog.component.html',
  styleUrls: ['./auto-equip-dialog.component.scss'],
})
export class AutoEquipDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<AutoEquipDialogComponent>);
  private readonly playerService = inject(PlayerService);
  private readonly snackBar = inject(MatSnackBar);

  readonly player = this.playerService.player;
  readonly busy = signal(false);
  readonly firearm = signal(true);
  readonly melee = signal(true);
  readonly defensive = signal(true);
  private readonly excludedFirearms = signal<ReadonlySet<string>>(new Set());

  private readonly stocks = computed(() =>
    allEquipmentStacks(this.player()).filter((stack) => stack.available > 0),
  );

  readonly firearmStacks = computed(() =>
    this.stocks()
      .filter((stack) => stack.equipment.category === 'FIREARM')
      .sort((a, b) => b.equipment.cost - a.equipment.cost),
  );

  readonly selectedFirearms = computed(() =>
    this.firearmStacks().filter((stack) => !this.excludedFirearms().has(stack.equipment.name)),
  );

  readonly canSubmit = computed(() => {
    if (this.busy() || (!this.firearm() && !this.melee() && !this.defensive())) return false;
    return (
      !this.firearm() || this.firearmStacks().length === 0 || this.selectedFirearms().length > 0
    );
  });

  equipmentLabel(equipment: Equipment): string {
    return equipmentLabelFor(equipment, this.player()?.race);
  }

  isFirearmSelected(name: string): boolean {
    return !this.excludedFirearms().has(name);
  }

  toggleFirearm(name: string): void {
    this.excludedFirearms.update((current) => {
      const next = new Set(current);
      if (next.has(name)) next.delete(name);
      else next.add(name);
      return next;
    });
  }

  async submit(): Promise<void> {
    if (!this.canSubmit()) return;
    const categories: AutoEquipCategory[] = [];
    const equipmentNames: string[] = [];
    if (this.firearm()) {
      categories.push('FIREARM');
      equipmentNames.push(...this.selectedFirearms().map((stack) => stack.equipment.name));
    }
    if (this.melee()) {
      categories.push('MELEE');
      equipmentNames.push(...this.namesOf('MELEE'));
    }
    if (this.defensive()) {
      categories.push('DEFENSIVE');
      equipmentNames.push(...this.namesOf('DEFENSIVE'));
    }
    if (equipmentNames.length === 0) {
      this.snackBar.open('Aucun équipement disponible pour ces catégories', 'Fermer', {
        duration: 3000,
      });
      return;
    }
    this.busy.set(true);
    try {
      const result = await this.playerService.autoEquipUnits(categories, equipmentNames);
      if (!result) {
        const error = this.playerService.error();
        if (error) this.snackBar.open(error, 'Fermer', { duration: 5000 });
        return;
      }
      this.snackBar.open(
        result.equippedCount > 0
          ? `${result.equippedCount} équipement(s) attribué(s) à ${result.unitCount} unité(s)`
          : 'Aucun équipement à attribuer',
        'Fermer',
        { duration: 3500 },
      );
      this.dialogRef.close(result);
    } finally {
      this.busy.set(false);
    }
  }

  close(): void {
    this.dialogRef.close();
  }

  private namesOf(category: AutoEquipCategory): string[] {
    return this.stocks()
      .filter((stack) => stack.equipment.category === category)
      .map((stack) => stack.equipment.name);
  }
}
