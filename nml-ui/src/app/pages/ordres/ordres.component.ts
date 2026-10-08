import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Unit } from '../../models';
import { characterStats } from '../../core/stats';
import { ExpPipe } from '../../shared/exp.pipe';
import { MovementStateService } from '../../services/movement-state.service';
import { PlayerService } from '../../services/player.service';
import {
  totalsStats,
  unitClassCodes,
  unitEquipmentLabel,
  unitStats,
  vehicleStats,
} from '../joueur/joueur.helpers';
import { groupOrders } from './ordres.helpers';

/**
 * Page "Mes ordres" : liste les ordres de déplacement PENDING du tour courant
 * (filtré côté backend via UnitService.getPlayerPendingOrders -> getCurrentTurn).
 * Permet l'annulation d'un ordre. Aucun état global : lit MovementStateService.
 */
@Component({
  selector: 'app-ordres',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe,
    ExpPipe,
    MatCardModule,
    MatExpansionModule,
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
  private readonly playerService = inject(PlayerService);
  private readonly snackBar = inject(MatSnackBar);

  readonly orders = this.movementState.orders;
  readonly loading = this.movementState.loading;
  readonly error = this.movementState.error;
  readonly busy = signal(false);

  readonly player = this.playerService.player;
  readonly groups = computed(() => groupOrders(this.orders(), this.player()));
  readonly entityCount = computed(() =>
    this.orders().reduce(
      (n, o) => n + (o.entityIds?.length ?? 0) + (o.vehicleId != null ? 1 : 0),
      0,
    ),
  );
  readonly globalPower = computed(() => this.groups().reduce((sum, group) => sum + group.power, 0));

  constructor() {
    void this.movementState.loadOrders();
    void this.playerService.loadCurrent();
  }

  readonly unitStats = unitStats;
  readonly characterStats = characterStats;
  readonly vehicleStats = vehicleStats;
  readonly totalsStats = totalsStats;
  readonly unitClassCodes = unitClassCodes;
  readonly unitEquipLabel = (unit: Unit): string => unitEquipmentLabel(unit, this.player()?.race);

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
}
