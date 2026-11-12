import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Board, TemplatePlayerRequest } from '../../models';

export interface TemplatePlayerData {
  board: Board;
  spots: number[][];
}

@Component({
  selector: 'app-template-player-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>Créer un joueur prêt</h2>
    <mat-dialog-content>
      <p class="hint">
        100 unités sans classe, un personnage et les 3 bâtiments à placer, 20 000 ₡ de départ. Le
        joueur choisira lui-même ses secteurs et ses classes au tour 0.
      </p>

      <mat-form-field appearance="outline" class="full">
        <mat-label>Nom du joueur</mat-label>
        <input matInput [value]="name()" (input)="name.set($any($event.target).value)" />
      </mat-form-field>

      <mat-form-field appearance="outline" class="full">
        <mat-label>Race</mat-label>
        <mat-select [value]="race()" (selectionChange)="race.set($event.value)">
          <mat-option value="ORKS">Orks</mat-option>
          <mat-option value="NECRONS">Nécrons</mat-option>
        </mat-select>
      </mat-form-field>

      @if (data.spots.length > 0) {
        <mat-form-field appearance="outline" class="full">
          <mat-label>Spot de départ</mat-label>
          <mat-select [value]="spotIndex()" (selectionChange)="spotIndex.set($event.value)">
            @for (spot of data.spots; track $index) {
              <mat-option [value]="$index" [disabled]="spotBlocked(spot)">
                Spot {{ $index + 1 }} — {{ spot.join(', ') }}
              </mat-option>
            }
          </mat-select>
        </mat-form-field>
      } @else {
        <mat-form-field appearance="outline" class="full">
          <mat-label>Secteurs de départ (1 à 3, séparés par des virgules)</mat-label>
          <input
            matInput
            [value]="manualSectors()"
            (input)="manualSectors.set($any($event.target).value)"
          />
        </mat-form-field>
      }

      <mat-form-field appearance="outline" class="full">
        <mat-label>Nom du personnage (optionnel)</mat-label>
        <input
          matInput
          [value]="characterName()"
          (input)="characterName.set($any($event.target).value)"
        />
      </mat-form-field>

      @if (sectorNumbers().length === 0 || sectorNumbers().length > 3) {
        <p class="error">Choisissez 1 à 3 secteurs de départ.</p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Annuler</button>
      <button mat-flat-button color="primary" (click)="create()" [disabled]="!canCreate()">
        Créer
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .hint {
        color: var(--muted);
        font-size: 0.85rem;
        margin-bottom: 14px;
        line-height: 1.5;
      }
      .full {
        width: 100%;
        margin-bottom: 10px;
      }
      .error {
        color: var(--danger);
        font-size: 0.8rem;
      }
    `,
  ],
})
export class TemplatePlayerDialogComponent {
  readonly data = inject<TemplatePlayerData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<TemplatePlayerDialogComponent>);

  readonly name = signal('');
  readonly race = signal('ORKS');
  readonly spotIndex = signal<number | null>(this.firstAvailableSpot());
  readonly manualSectors = signal('');
  readonly characterName = signal('');

  readonly sectorNumbers = computed(() => {
    const index = this.spotIndex();
    if (this.data.spots.length > 0 && index !== null) {
      return this.data.spots[index] ?? [];
    }
    return this.manualSectors()
      .split(',')
      .map((value) => Number(value.trim()))
      .filter((value) => Number.isInteger(value) && value > 0);
  });

  readonly canCreate = computed(
    () =>
      this.name().trim().length > 0 &&
      this.sectorNumbers().length >= 1 &&
      this.sectorNumbers().length <= 3,
  );

  private firstAvailableSpot(): number | null {
    const index = this.data.spots.findIndex((spot) => !this.spotBlocked(spot));
    return index >= 0 ? index : null;
  }

  spotBlocked(spot: number[]): boolean {
    return spot.some((number) => {
      const sector = this.data.board.sectors[number];
      return !!sector && sector.ownerId !== null;
    });
  }

  create(): void {
    if (!this.canCreate()) return;
    const request: TemplatePlayerRequest = {
      name: this.name().trim(),
      race: this.race(),
      sectorNumbers: this.sectorNumbers(),
      characterName: this.characterName().trim() || undefined,
    };
    this.dialogRef.close(request);
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
