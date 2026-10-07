import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import type { Building } from '../../models';
import { buildingMoveStatus } from './joueur.helpers';

@Component({
  selector: 'app-bank-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, MatButtonModule, MatCardModule, MatIconModule, MatTooltipModule],
  templateUrl: './bank-panel.component.html',
  styleUrls: ['./building-panel.shared.scss'],
})
export class BankPanelComponent {
  readonly building = input<Building | null>(null);
  readonly money = input(0);
  readonly startingMoneyRemaining = input(0);
  readonly transferableMoney = input(0);
  readonly currentTurn = input<number | null>(null);
  readonly pendingOffers = input(0);

  readonly move = output<Building>();
  readonly exchange = output<void>();

  readonly status = computed(() => {
    const building = this.building();
    return building ? buildingMoveStatus(building, this.currentTurn()) : null;
  });

  requestMove(): void {
    const building = this.building();
    if (building) this.move.emit(building);
  }
}
