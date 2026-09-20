import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import type { HarvestChoice, PlayerAction } from '../../models';
import type { SectorHarvestState } from './joueur.helpers';

@Component({
  selector: 'app-harvest-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, MatButtonModule, MatCardModule, MatIconModule, MatTooltipModule],
  templateUrl: './harvest-panel.component.html',
  styleUrls: ['./harvest-panel.component.scss'],
})
export class HarvestPanelComponent {
  readonly states = input<SectorHarvestState[]>([]);
  readonly currentTurn = input<number | null>(null);
  readonly loading = input(false);

  readonly harvest = output<{ choice: HarvestChoice; sectorNumbers: number[] }>();
  readonly undo = output<PlayerAction>();

  readonly pending = computed(() => this.states().filter((state) => state.choice === null));
  readonly pendingMoney = computed(() =>
    this.pending().reduce((sum, state) => sum + state.money, 0),
  );
  readonly pendingWithResource = computed(() =>
    this.pending().filter((state) => state.resource != null),
  );

  harvestOne(state: SectorHarvestState, choice: HarvestChoice): void {
    if (state.sector.number == null) return;
    this.harvest.emit({ choice, sectorNumbers: [state.sector.number] });
  }

  harvestAll(choice: HarvestChoice): void {
    const candidates = choice === 'HARVEST_RESOURCE' ? this.pendingWithResource() : this.pending();
    const sectorNumbers = candidates
      .map((state) => state.sector.number)
      .filter((number): number is number => number != null);
    if (sectorNumbers.length > 0) {
      this.harvest.emit({ choice, sectorNumbers });
    }
  }

  cancel(state: SectorHarvestState): void {
    if (state.action) this.undo.emit(state.action);
  }
}
