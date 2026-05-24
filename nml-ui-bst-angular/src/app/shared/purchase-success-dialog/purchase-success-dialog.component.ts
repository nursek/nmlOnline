import { Component, Inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';

export interface PurchaseSuccessData {
  title: string;
  lines: string[];
  totalCost: number;
  isSale?: boolean;
}

@Component({
  selector: 'app-purchase-success-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, MatDividerModule],
  template: `
    <div class="success-dialog">
      <div class="success-header">
        <mat-icon class="success-icon">check_circle</mat-icon>
        <h2 mat-dialog-title>{{ data.title }}</h2>
      </div>

      <mat-dialog-content>
        <ul class="items-list">
          @for (line of data.lines; track line) {
            <li>{{ line }}</li>
          }
        </ul>
        <mat-divider />
        <div class="total-row">
          <span class="total-label">{{ data.isSale ? 'Recettes totales' : 'Total dépensé' }}</span>
          <span class="total-value" [class.sale]="data.isSale">
            {{ data.totalCost.toFixed(0) }} ₡
          </span>
        </div>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-flat-button color="primary" (click)="close()">Parfait !</button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .success-dialog {
      min-width: 320px;
    }
    .success-header {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 20px 24px 8px;
    }
    .success-icon {
      font-size: 36px;
      width: 36px;
      height: 36px;
      color: #10b981;
    }
    h2[mat-dialog-title] {
      margin: 0;
      font-size: 1.2rem;
    }
    .items-list {
      margin: 8px 0 16px;
      padding-left: 18px;
    }
    .items-list li {
      margin-bottom: 4px;
      color: rgba(255,255,255,0.85);
    }
    .total-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 0 4px;
    }
    .total-label {
      color: rgba(255,255,255,0.6);
      font-size: 0.9rem;
    }
    .total-value {
      font-weight: 700;
      font-size: 1.1rem;
      color: #f59e0b;
    }
    .total-value.sale { color: #10b981; }
  `],
})
export class PurchaseSuccessDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<PurchaseSuccessDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: PurchaseSuccessData
  ) {}

  close(): void {
    this.dialogRef.close();
  }
}
