import { Component, inject, computed, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { Vehicle, Sector } from '../../models';

export interface VehiclePlacementDialogData {
  vehicle: Vehicle;
  ownedSectors: Sector[];
}

@Component({
  selector: 'app-vehicle-placement-modal',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    MatIconModule,
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>place</mat-icon>
      Déployer {{ data.vehicle.displayName }}
    </h2>

    <mat-dialog-content>
      <p class="vehicle-info">
        <strong>{{ data.vehicle.displayName }}</strong> &mdash;
        PDF&nbsp;: {{ data.vehicle.pdf }} | Défense&nbsp;: {{ data.vehicle.defense }}
      </p>

      <mat-form-field appearance="outline" style="width: 100%; margin-top: 12px;">
        <mat-label>Choisir un secteur</mat-label>
        <mat-select [(ngModel)]="selectedSector">
          @for (sector of eligibleSectors(); track sector.number) {
            <mat-option [value]="sector">
              Secteur {{ sector.number }} — {{ sector.name }}
            </mat-option>
          }
        </mat-select>
      </mat-form-field>

      @if (eligibleSectors().length === 0) {
        <p class="no-sectors">Vous ne possédez aucun secteur pour déployer ce véhicule.</p>
      }
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Annuler</button>
      <button mat-raised-button color="primary"
              [disabled]="!selectedSector"
              (click)="confirm()">
        <mat-icon>check</mat-icon>
        Déployer
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    h2 mat-icon { vertical-align: middle; margin-right: 8px; }
    .vehicle-info { color: #aaa; font-size: 0.9rem; margin-bottom: 8px; }
    .no-sectors { color: #f44336; font-size: 0.9rem; }
  `],
})
export class VehiclePlacementModalComponent {
  readonly dialogRef = inject(MatDialogRef<VehiclePlacementModalComponent>);
  readonly data: VehiclePlacementDialogData = inject(MAT_DIALOG_DATA);

  selectedSector: Sector | null = null;

  readonly eligibleSectors = computed(() =>
    this.data.ownedSectors.filter(s => s.number != null && s.boardId != null)
  );

  confirm(): void {
    if (!this.selectedSector) return;
    this.dialogRef.close(this.selectedSector);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }
}
