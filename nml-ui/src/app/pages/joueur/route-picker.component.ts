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
import { MatIconModule } from '@angular/material/icon';
import { Sector } from '../../models';
import { sectorKind, sectorKindLabel } from '../../core/movement.helpers';
import { routeForSteps } from './route-picker.helpers';

@Component({
  selector: 'app-route-picker',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIconModule],
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
  private lastFrom: number | null = null;

  readonly routeSteps = computed(() => this.route().slice(1));
  readonly arrivalSector = computed(() => {
    const route = this.route();
    return route.length > 1 ? route[route.length - 1] : null;
  });
  readonly transitSectors = computed(() => {
    const route = this.route();
    return route.length > 2 ? route.slice(1, -1) : [];
  });
  readonly canRemoveLast = computed(() => this.route().length > 1);

  constructor() {
    effect(() => {
      const from = this.from();
      const maxHops = this.maxHops();
      untracked(() => {
        if (this.lastFrom !== null && this.lastFrom !== from) {
          this.reset();
        } else {
          this.truncateTo(maxHops);
        }
        this.lastFrom = from;
      });
    });
  }

  removeLastStep(): void {
    const steps = [...this.steps()];
    while (steps.length && steps[steps.length - 1] == null) steps.pop();
    steps.pop();
    this.steps.set(steps.length ? steps : [null]);
    this.syncRoute();
  }

  setRoute(route: number[]): void {
    if (route[0] !== this.from()) {
      this.reset();
      return;
    }
    const steps = route.slice(1, this.maxHops() + 1);
    this.steps.set(steps.length ? steps : [null]);
    this.syncRoute();
  }

  reset(): void {
    this.steps.set([null]);
    this.route.set([]);
  }

  sectorKindText(number: number): string | null {
    if (this.playerId() == null) return null;
    const sector = this.sectors().find((candidate) => candidate.number === number) ?? null;
    return sectorKindLabel(
      sectorKind(sector, this.playerId(), this.allyPlayerIds() ?? this.emptyAllies),
    );
  }

  isOccupied(number: number): boolean {
    const sector = this.sectors().find((candidate) => candidate.number === number);
    return (
      !!sector &&
      ((sector.army?.length ?? 0) > 0 ||
        sector.character != null ||
        (sector.vehicles?.length ?? 0) > 0)
    );
  }

  arrivalSuffix(): string {
    const sector = this.arrivalSector();
    if (sector == null) return '';
    const parts = [
      this.sectorKindText(sector),
      this.isOccupied(sector) ? 'forces présentes' : null,
    ].filter((part): part is string => part != null);
    return parts.length ? ` (${parts.join(' · ')})` : '';
  }

  private truncateTo(maxHops: number): void {
    if (maxHops < 1) {
      this.reset();
      return;
    }
    const steps = this.steps().slice(0, maxHops);
    this.steps.set(steps.length ? steps : [null]);
    this.syncRoute();
  }

  private syncRoute(): void {
    const steps = this.steps();
    this.route.set(steps.some((step) => step != null) ? routeForSteps(this.from(), steps) : []);
  }
}
