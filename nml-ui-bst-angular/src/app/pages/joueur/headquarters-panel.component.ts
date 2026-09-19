import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import type { Building } from '../../models';
import { buildingMoveStatus } from './joueur.helpers';

@Component({
  selector: 'app-headquarters-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, MatButtonModule, MatCardModule, MatIconModule, MatTooltipModule],
  templateUrl: './headquarters-panel.component.html',
  styleUrls: ['./building-panel.shared.scss'],
})
export class HeadquartersPanelComponent {
  readonly building = input<Building | null>(null);
  readonly currentTurn = input<number | null>(null);

  readonly move = output<Building>();

  readonly status = computed(() => {
    const building = this.building();
    return building ? buildingMoveStatus(building, this.currentTurn()) : null;
  });

  requestMove(): void {
    const building = this.building();
    if (building) this.move.emit(building);
  }
}
