import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../../services/api.service';
import { ShopService } from '../../services/shop.service';
import { Building, Player, Sector, Unit } from '../../models';
import { httpErrorMessage } from '../../core/http-error.interceptor';

@Component({
  selector: 'app-preparation-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './preparation-panel.component.html',
  styleUrls: ['./preparation-panel.component.scss'],
})
export class PreparationPanelComponent {
  private readonly api = inject(ApiService);
  private readonly shop = inject(ShopService);
  private readonly snackBar = inject(MatSnackBar);

  readonly player = input<Player | null>(null);
  readonly sectors = input<Sector[]>([]);
  readonly reserveUnits = input<Unit[]>([]);
  readonly currentTurn = input<number | null>(null);
  readonly changed = output<void>();

  readonly unplacedBuildings = computed(() =>
    (this.player()?.buildings ?? []).filter((building) => building.sectorNumber === null),
  );
  readonly character = computed(() => this.player()?.character ?? null);
  readonly characterUnplaced = computed(() => {
    const character = this.character();
    return character !== null && character.sectorNumber === null;
  });
  readonly unclassifiedUnits = computed(() =>
    this.reserveUnits().filter((unit) => unit.classes.length === 0),
  );
  readonly sectorNumbers = computed(() =>
    this.sectors()
      .map((sector) => sector.number)
      .filter((number): number is number => number !== null)
      .sort((a, b) => a - b),
  );
  readonly unitClasses = computed(() => this.shop.unitCatalog().classes);
  readonly ready = computed(
    () =>
      this.sectorNumbers().length > 0 &&
      this.unplacedBuildings().length === 0 &&
      !this.characterUnplaced() &&
      this.unclassifiedUnits().length === 0,
  );

  readonly characterSector = signal<number | null>(null);
  readonly buildingSectors = signal<Record<number, number>>({});
  readonly unitSector = signal<number | null>(null);
  readonly unitQuantity = signal(100);
  readonly unitClass = signal('LEGER');
  readonly saving = signal(false);

  readonly playerSectorLabel = computed(() =>
    this.sectorNumbers().length === 0
      ? 'Aucun secteur de départ'
      : 'Secteurs : ' + this.sectorNumbers().join(', '),
  );

  buildingSector(building: Building): number | null {
    return building.id === null ? null : (this.buildingSectors()[building.id] ?? null);
  }

  setBuildingSector(building: Building, sectorNumber: number): void {
    if (building.id === null) return;
    this.buildingSectors.update((current) => ({ ...current, [building.id!]: sectorNumber }));
  }

  async placeCharacter(): Promise<void> {
    const sector = this.characterSector();
    if (sector === null) return;
    await this.run(
      () => firstValueFrom(this.api.placePreparationCharacter(sector)),
      'Personnage placé',
    );
  }

  async placeBuilding(building: Building): Promise<void> {
    if (building.id === null) return;
    const sector = this.buildingSector(building);
    if (sector === null) return;
    await this.run(
      () => firstValueFrom(this.api.placePreparationBuilding(building.id!, sector)),
      `${building.displayName} placé`,
    );
  }

  async distributeUnits(): Promise<void> {
    const sector = this.unitSector();
    const remaining = this.unclassifiedUnits().length;
    const quantity = Math.max(1, Math.min(this.unitQuantity(), remaining));
    if (sector === null || remaining === 0) return;
    const done = await this.run(
      () =>
        firstValueFrom(this.api.distributePreparationUnits(sector, quantity, [this.unitClass()])),
      `${quantity} unité(s) répartie(s)`,
    );
    if (done) {
      this.unitQuantity.set(Math.max(1, remaining - quantity));
    }
  }

  private async run(action: () => Promise<unknown>, success: string): Promise<boolean> {
    this.saving.set(true);
    try {
      await action();
      this.snackBar.open(success, 'OK', { duration: 3000 });
      this.changed.emit();
      return true;
    } catch (error) {
      this.snackBar.open(httpErrorMessage(error, 'Action impossible'), 'Fermer', {
        duration: 5000,
        panelClass: 'error-snackbar',
      });
      return false;
    } finally {
      this.saving.set(false);
    }
  }
}
