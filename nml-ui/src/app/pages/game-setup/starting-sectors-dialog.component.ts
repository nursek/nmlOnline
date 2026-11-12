import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { Board, Sector } from '../../models';

export interface StartingSectorsData {
  playerId: number;
  playerName: string;
  current: number[];
  board: Board;
  spots: number[][];
  playerNames: Record<number, string>;
}

@Component({
  selector: 'app-starting-sectors-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatDialogModule, MatButtonModule, MatCheckboxModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>Secteurs de départ — {{ data.playerName }}</h2>
    <mat-dialog-content>
      <p class="hint">
        Choisissez 1 à 3 secteurs libres. Les spots du preset sont des groupes conseillés.
      </p>

      @if (data.spots.length > 0) {
        <div class="spots">
          @for (spot of data.spots; track $index) {
            <button
              mat-stroked-button
              type="button"
              [disabled]="spotBlocked(spot)"
              (click)="selectSpot(spot)"
            >
              Spot {{ $index + 1 }} — {{ spot.join(', ') }}
            </button>
          }
        </div>
      }

      <div class="selected">
        <strong>{{ selected().size }} secteur(s)</strong>
        @if (selected().size > 3) {
          <span class="error">Maximum 3.</span>
        }
      </div>

      <div class="sectors">
        @for (sector of sectors(); track sector.number) {
          <mat-checkbox
            [checked]="selected().has(sector.number!)"
            [disabled]="blocked(sector)"
            (change)="toggle(sector.number!, $event.checked)"
          >
            #{{ sector.number }} {{ sector.name }}
            @if (ownerLabel(sector); as owner) {
              <span class="owner">({{ owner }})</span>
            }
          </mat-checkbox>
        }
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Annuler</button>
      <button mat-flat-button color="primary" (click)="save()" [disabled]="selected().size > 3">
        Enregistrer
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .hint {
        color: var(--muted);
        font-size: 0.85rem;
        margin-bottom: 12px;
      }
      .spots {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        margin-bottom: 14px;
      }
      .selected {
        display: flex;
        gap: 10px;
        align-items: baseline;
        margin-bottom: 10px;
      }
      .sectors {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
        gap: 2px 14px;
        max-height: 320px;
        overflow-y: auto;
      }
      .owner {
        color: var(--muted);
        font-size: 0.75rem;
        margin-left: 4px;
      }
      .error {
        color: var(--danger);
        font-size: 0.8rem;
      }
    `,
  ],
})
export class StartingSectorsDialogComponent {
  readonly data = inject<StartingSectorsData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<StartingSectorsDialogComponent>);

  readonly selected = signal(new Set(this.data.current));

  readonly sectors = computed(() =>
    Object.values(this.data.board.sectors)
      .filter((sector) => sector.number !== null)
      .sort((a, b) => (a.number ?? 0) - (b.number ?? 0)),
  );

  blocked(sector: Sector): boolean {
    return sector.ownerId !== null && sector.ownerId !== this.data.playerId;
  }

  ownerLabel(sector: Sector): string | null {
    if (sector.ownerId === null) return null;
    if (sector.ownerId === this.data.playerId) return 'ce joueur';
    return this.data.playerNames[sector.ownerId] ?? `#${sector.ownerId}`;
  }

  spotBlocked(spot: number[]): boolean {
    return spot.some((number) => {
      const sector = this.data.board.sectors[number];
      return !!sector && sector.ownerId !== null && sector.ownerId !== this.data.playerId;
    });
  }

  selectSpot(spot: number[]): void {
    if (this.spotBlocked(spot)) return;
    this.selected.set(new Set(spot));
  }

  toggle(number: number, checked: boolean): void {
    const next = new Set(this.selected());
    if (checked) {
      if (next.size >= 3) return;
      next.add(number);
    } else {
      next.delete(number);
    }
    this.selected.set(next);
  }

  save(): void {
    this.dialogRef.close(Array.from(this.selected()));
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
