import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import {
  MatDialog,
  MatDialogModule,
  MatDialogRef,
  MAT_DIALOG_DATA,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import type { PlayerResource } from '../../models';
import { ShopService } from '../../services/shop.service';
import { saleMultiplier, saleValue } from '../../core/sale-multiplier';
import {
  PurchaseSuccessDialogComponent,
  PurchaseSuccessData,
} from '../../shared/purchase-success-dialog/purchase-success-dialog.component';

export interface SellResourceDialogData {
  resource: PlayerResource;
}

@Component({
  selector: 'app-sell-resource-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  templateUrl: './sell-resource-dialog.component.html',
  styleUrls: ['./sell-resource-dialog.component.scss'],
})
export class SellResourceDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<SellResourceDialogComponent>);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  readonly data = inject<SellResourceDialogData>(MAT_DIALOG_DATA);
  private readonly shop = inject(ShopService);

  readonly busy = this.shop.purchaseLoading;

  readonly maxQuantity = computed(() => Math.max(1, this.data.resource.quantity));
  readonly quantity = signal(Math.max(1, this.data.resource.quantity));
  readonly multiplier = computed(() => saleMultiplier(this.quantity()));
  readonly totalValue = computed(() =>
    saleValue(this.data.resource.baseValue ?? 0, this.quantity()),
  );

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
    const id = this.data.resource.id;
    if (id == null || this.busy()) return;

    const quantity = this.quantity();
    try {
      const totalValue = await this.shop.sellResource(id, quantity);
      this.dialogRef.close();
      const data: PurchaseSuccessData = {
        title: 'Ressources vendues !',
        lines: [`${quantity} × ${this.data.resource.name}`],
        totalCost: totalValue,
        isSale: true,
      };
      this.dialog.open(PurchaseSuccessDialogComponent, { width: '360px', data });
    } catch {
      // Erreur déjà exposée par ShopService.error.
      this.snackBar.open(this.shop.error() ?? 'Erreur lors de la vente', 'Fermer', {
        duration: 5000,
      });
    }
  }

  close(): void {
    this.dialogRef.close();
  }
}
