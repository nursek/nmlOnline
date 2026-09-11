import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import type { PlayerResource } from '../../models';
import { slugify } from '../../core/slug';
import { saleValue } from '../../core/sale-multiplier';

@Component({
  selector: 'app-resource-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './resource-card.component.html',
  styleUrls: ['./resource-card.component.scss'],
})
export class ResourceCardComponent {
  readonly resource = input.required<PlayerResource>();
  readonly sell = output<void>();

  readonly imgError = signal(false);

  readonly imageUrl = computed(() => `assets/shop/resources/${slugify(this.resource().name)}.png`);

  readonly totalValue = computed(() =>
    saleValue(this.resource().baseValue ?? 0, this.resource().quantity),
  );

  onImgError(): void {
    this.imgError.set(true);
  }
}
