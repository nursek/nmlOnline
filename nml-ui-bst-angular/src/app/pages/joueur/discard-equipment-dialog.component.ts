import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import type { EquipmentStack } from '../../models';
import { PlayerService } from '../../services/player.service';

export interface DiscardEquipmentDialogData {
  buildingId: number;
  stack: EquipmentStack;
}

@Component({
  selector: 'app-discard-equipment-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatDialogModule, MatFormFieldModule, MatIconModule, MatInputModule],
  templateUrl: './discard-equipment-dialog.component.html',
  styleUrls: ['./discard-equipment-dialog.component.scss'],
})
export class DiscardEquipmentDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<DiscardEquipmentDialogComponent>);
  private readonly snackBar = inject(MatSnackBar);
  private readonly playerService = inject(PlayerService);
  readonly data = inject<DiscardEquipmentDialogData>(MAT_DIALOG_DATA);

  readonly maxQuantity = computed(() => Math.max(1, this.data.stack.available));
  readonly quantity = signal(Math.max(1, this.data.stack.available));
  readonly saving = signal(false);

  decrease(): void {
    this.setQuantity(this.quantity() - 1);
  }

  increase(): void {
    this.setQuantity(this.quantity() + 1);
  }

  setMax(): void {
    this.setQuantity(this.maxQuantity());
  }

  onInput(event: Event): void {
    this.setQuantity(Number((event.target as HTMLInputElement).value));
  }

  private setQuantity(value: number): void {
    const n = Number.isFinite(value) ? Math.trunc(value) : this.quantity();
    this.quantity.set(Math.min(this.maxQuantity(), Math.max(1, n)));
  }

  async confirm(): Promise<void> {
    if (this.saving()) return;
    this.saving.set(true);
    try {
      const ok = await this.playerService.discardEquipment(
        this.data.buildingId,
        this.data.stack.equipment.name,
        this.quantity(),
      );
      if (!ok) {
        this.snackBar.open(this.playerService.error() ?? 'Erreur lors du jet', 'Fermer', {
          duration: 5000,
        });
        return;
      }
      this.dialogRef.close();
      this.snackBar.open('Équipements jetés définitivement', 'Fermer', { duration: 3000 });
    } finally {
      this.saving.set(false);
    }
  }

  close(): void {
    this.dialogRef.close();
  }
}
