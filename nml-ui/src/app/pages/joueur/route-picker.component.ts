import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  input,
  model,
  signal,
  untracked,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { Sector } from '../../models';
import { sectorKind, sectorKindLabel } from '../../core/movement.helpers';
import { routeForSteps, routeStepOptions, visibleRouteSteps } from './route-picker.helpers';

@Component({
  selector: 'app-route-picker',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatFormFieldModule, MatIconModule, MatSelectModule],
  templateUrl: './route-picker.component.html',
  styleUrls: ['./route-picker.component.scss'],
})
export class RoutePickerComponent {
  readonly sectors = input.required<readonly Sector[]>();
  readonly from = input.required<number>();
  readonly maxHops = input.required<number>();
  readonly playerId = input<number | null>(null);
  readonly allyPlayerIds = input<ReadonlySet<number> | undefined>(undefined);
  readonly disabled = input(false);

  readonly route = model<number[]>([]);

  private readonly emptyAllies = new Set<number>();
  private readonly steps = signal<(number | null)[]>([null]);

  readonly visibleSteps = computed(() => visibleRouteSteps(this.maxHops(), this.steps()));
  readonly stepIndexes = computed(() => Array.from({ length: this.visibleSteps() }, (_, i) => i));
  readonly routeSteps = computed(() => this.route().slice(1));

  constructor() {
    effect(() => {
      this.from();
      this.maxHops();
      untracked(() => this.reset());
    });
  }

  stepValue(index: number): number | null {
    return this.steps()[index] ?? null;
  }

  optionsFor(index: number): number[] {
    return routeStepOptions(this.sectors(), this.from(), this.steps(), index);
  }

  setStep(index: number, value: number | null): void {
    const steps = [...this.steps()];
    steps[index] = value;
    steps.length = Math.max(1, index + 1);
    this.steps.set(steps);
    this.route.set(routeForSteps(this.from(), steps));
  }

  reset(): void {
    this.steps.set([null]);
    this.route.set([]);
  }

  sectorName(number: number): string {
    return this.sectors().find((sector) => sector.number === number)?.name ?? `Secteur ${number}`;
  }

  sectorKindText(number: number): string | null {
    if (this.playerId() == null) return null;
    const sector = this.sectors().find((candidate) => candidate.number === number) ?? null;
    return sectorKindLabel(
      sectorKind(sector, this.playerId(), this.allyPlayerIds() ?? this.emptyAllies),
    );
  }
}
