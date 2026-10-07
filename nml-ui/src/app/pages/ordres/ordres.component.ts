import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MovementStateService } from '../../services/movement-state.service';

/**
 * Page "Mes ordres" : liste les ordres de déplacement PENDING du tour courant
 * (filtré côté backend via UnitService.getPlayerPendingOrders -> getCurrentTurn).
 * Permet l'annulation d'un ordre. Aucun état global : lit MovementStateService.
 */
@Component({
  selector: 'app-ordres',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatCardModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
  ],
  templateUrl: './ordres.component.html',
  styleUrls: ['./ordres.component.scss'],
})
export class OrdresComponent {
  private readonly movementState = inject(MovementStateService);
  private readonly snackBar = inject(MatSnackBar);

  readonly orders = this.movementState.orders;
  readonly loading = this.movementState.loading;
  readonly error = this.movementState.error;
  readonly busy = signal(false);

  constructor() {
    void this.movementState.loadOrders();
  }

  async cancelOrder(orderId: number): Promise<void> {
    if (this.busy()) return;
    this.busy.set(true);
    try {
      const ok = await this.movementState.cancelOrder(orderId);
      if (ok) this.snackBar.open('Ordre annulé', 'OK', { duration: 2500 });
    } finally {
      this.busy.set(false);
    }
  }

  async refresh(): Promise<void> {
    await this.movementState.loadOrders();
  }

  trackOrder(_: number, o: { id: number }): number {
    return o.id;
  }
}
