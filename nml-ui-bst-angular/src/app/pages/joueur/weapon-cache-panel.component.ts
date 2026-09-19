import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import type { Building, EquipmentStack } from '../../models';
import { buildingMoveStatus } from './joueur.helpers';

@Component({
  selector: 'app-weapon-cache-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatCardModule, MatIconModule, MatTooltipModule],
  templateUrl: './weapon-cache-panel.component.html',
  styleUrls: ['./building-panel.shared.scss'],
})
export class WeaponCachePanelComponent {
  readonly building = input<Building | null>(null);
  readonly stacks = input<EquipmentStack[]>([]);
  readonly currentTurn = input<number | null>(null);

  readonly move = output<Building>();
  readonly equipments = output<void>();

  readonly total = computed(() =>
    this.stacks().reduce((sum, stack) => sum + (stack.quantity ?? 0), 0),
  );
  readonly available = computed(() =>
    this.stacks().reduce((sum, stack) => sum + (stack.available ?? 0), 0),
  );
  readonly maxCapacity = computed(() => this.building()?.maxCapacity ?? 0);
  readonly fillPercentage = computed(() => {
    const max = this.maxCapacity();
    return max > 0 ? Math.min(100, (this.total() / max) * 100) : 0;
  });
  readonly status = computed(() => {
    const building = this.building();
    return building ? buildingMoveStatus(building, this.currentTurn()) : null;
  });

  requestMove(): void {
    const building = this.building();
    if (building) this.move.emit(building);
  }
}
