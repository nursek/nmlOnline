import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import {
  MatDialog,
  MatDialogModule,
  MatDialogRef,
  MAT_DIALOG_DATA,
} from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import type { EquipmentStack } from '../../models';
import { PlayerService } from '../../services/player.service';
import {
  DiscardEquipmentDialogComponent,
  DiscardEquipmentDialogData,
} from './discard-equipment-dialog.component';

export interface CacheEquipmentDialogData {
  buildingId: number;
}

@Component({
  selector: 'app-cache-equipment-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatDialogModule, MatIconModule, MatTooltipModule],
  templateUrl: './cache-equipment-dialog.component.html',
  styleUrls: ['./cache-equipment-dialog.component.scss'],
})
export class CacheEquipmentDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<CacheEquipmentDialogComponent>);
  private readonly dialog = inject(MatDialog);
  private readonly playerService = inject(PlayerService);
  readonly data = inject<CacheEquipmentDialogData>(MAT_DIALOG_DATA);

  readonly stacks = computed(() =>
    [...(this.playerService.player()?.equipments ?? [])].sort((a, b) =>
      a.equipment.name.localeCompare(b.equipment.name),
    ),
  );
  readonly total = computed(() =>
    this.stacks().reduce((sum, stack) => sum + (stack.quantity ?? 0), 0),
  );

  openDiscard(stack: EquipmentStack): void {
    const data: DiscardEquipmentDialogData = { buildingId: this.data.buildingId, stack };
    this.dialog.open(DiscardEquipmentDialogComponent, {
      width: '420px',
      maxWidth: '95vw',
      data,
    });
  }

  close(): void {
    this.dialogRef.close();
  }
}
