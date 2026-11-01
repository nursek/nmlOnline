import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatRadioModule } from '@angular/material/radio';
import type { CacheChoice } from './boutique.helpers';

export interface CachePickerDialogData {
  caches: CacheChoice[];
  totalQuantity: number;
}

export interface CachePickerDialogResult {
  cacheId: number;
}

@Component({
  selector: 'app-cache-picker-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatDialogModule, MatButtonModule, MatIconModule, MatRadioModule],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>inventory_2</mat-icon>
      Cache de destination
    </h2>

    <mat-dialog-content>
      <p class="hint">
        {{ data.totalQuantity }} équipement(s) à ranger : choisissez la cache d'armes qui les reçoit.
      </p>

      <mat-radio-group class="caches" [value]="selected()" (change)="selected.set($event.value)">
        @for (cache of data.caches; track cache.id; let index = $index) {
          <mat-radio-button [value]="cache.id" [disabled]="!canHost(cache)">
            Cache {{ index + 1 }} — {{ sectorLabel(cache) }} · {{ cache.availableCapacity }} place(s)
            libre(s)
          </mat-radio-button>
        }
      </mat-radio-group>

      @if (!hasRoom()) {
        <p class="empty">
          Pas assez de place pour ce panier dans une seule cache : réduisez le panier ou libérez de
          la place.
        </p>
      }
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Annuler</button>
      <button mat-raised-button color="primary" [disabled]="selected() === null" (click)="confirm()">
        Choisir
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      h2 mat-icon {
        vertical-align: middle;
        margin-right: 8px;
      }
      .hint {
        color: var(--muted);
        font-size: 0.85rem;
      }
      .caches {
        display: flex;
        flex-direction: column;
        gap: 4px;
      }
      .empty {
        color: var(--danger);
        font-size: 0.85rem;
      }
    `,
  ],
})
export class CachePickerDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<CachePickerDialogComponent>);
  readonly data = inject<CachePickerDialogData>(MAT_DIALOG_DATA);

  readonly selected = signal<number | null>(
    this.data.caches.find((cache) => this.canHost(cache))?.id ?? null,
  );

  canHost(cache: CacheChoice): boolean {
    return cache.availableCapacity >= this.data.totalQuantity;
  }

  hasRoom(): boolean {
    return this.data.caches.some((cache) => this.canHost(cache));
  }

  sectorLabel(cache: CacheChoice): string {
    return cache.sectorNumber == null ? 'hors secteur' : `secteur ${cache.sectorNumber}`;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  confirm(): void {
    const cacheId = this.selected();
    if (cacheId != null) {
      this.dialogRef.close({ cacheId } satisfies CachePickerDialogResult);
    }
  }
}
