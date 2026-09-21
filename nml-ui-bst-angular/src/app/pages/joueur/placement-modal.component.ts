import { Component, inject, computed, signal, ChangeDetectionStrategy } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { Sector } from '../../models';

export interface PlacementDialogData {
  title: string;
  ownedSectors: Sector[];
}

@Component({
  selector: 'app-placement-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatDialogModule, MatButtonModule, MatFormFieldModule, MatSelectModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>place</mat-icon>
      {{ data.title }}
    </h2>

    <mat-dialog-content>
      <mat-form-field appearance="outline" class="sector-select">
        <mat-label>Choisir un secteur</mat-label>
        <mat-select [value]="selectedSector()" (valueChange)="selectedSector.set($event)">
          @for (sector of eligibleSectors(); track sector.number) {
            <mat-option [value]="sector">
              Secteur {{ sector.number }} — {{ sector.name }}
            </mat-option>
          }
        </mat-select>
      </mat-form-field>

      @if (eligibleSectors().length === 0) {
        <p class="no-sectors">Vous ne possédez aucun secteur pour ce déploiement.</p>
      }
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Annuler</button>
      <button mat-raised-button color="primary" [disabled]="!selectedSector()" (click)="confirm()">
        <mat-icon>check</mat-icon>
        Déployer
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      h2 mat-icon {
        vertical-align: middle;
        margin-right: 8px;
      }
      .no-sectors {
        color: var(--danger);
        font-size: 0.9rem;
      }
      .sector-select {
        width: 100%;
        margin-top: 12px;
      }
    `,
  ],
})
export class PlacementModalComponent {
  readonly dialogRef = inject(MatDialogRef<PlacementModalComponent>);
  readonly data: PlacementDialogData = inject(MAT_DIALOG_DATA);

  readonly selectedSector = signal<Sector | null>(null);

  readonly eligibleSectors = computed(() =>
    this.data.ownedSectors.filter((s) => s.number != null && s.boardId != null),
  );

  confirm(): void {
    const sector = this.selectedSector();
    if (!sector) return;
    this.dialogRef.close(sector);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }
}
