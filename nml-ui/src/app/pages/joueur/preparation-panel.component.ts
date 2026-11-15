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
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../../services/api.service';
import { ShopService } from '../../services/shop.service';
import { Building, Player, Sector, Unit } from '../../models';
import { httpErrorMessage } from '../../core/http-error.interceptor';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';

interface PlacedUnitGroup {
  key: string;
  sectorNumber: number;
  classNames: string[];
  label: string;
  count: number;
}

interface SectorDetail {
  number: number;
  name: string;
  incomeLabel: string;
  resource: string;
  adjacentOwners: string[];
  buildings: number;
  units: number;
  character: boolean;
}

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
  private readonly dialog = inject(MatDialog);

  readonly player = input<Player | null>(null);
  readonly sectors = input<Sector[]>([]);
  readonly reserveUnits = input<Unit[]>([]);
  readonly currentTurn = input<number | null>(null);
  readonly ownerNames = input<Record<number, string>>({});
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
  readonly placedCharacter = computed(() => {
    const character = this.character();
    return character !== null && character.sectorNumber !== null ? character : null;
  });
  readonly placedBuildings = computed(() =>
    (this.player()?.buildings ?? []).filter((building) => building.sectorNumber !== null),
  );
  readonly placedUnitGroups = computed<PlacedUnitGroup[]>(() => {
    const playerId = this.player()?.id ?? null;
    const groups = new Map<string, PlacedUnitGroup>();
    for (const sector of this.sectors()) {
      const sectorNumber = sector.number;
      if (playerId === null || sectorNumber === null) continue;
      for (const unit of sector.army ?? []) {
        if (unit.playerId !== playerId || unit.classes.length === 0) continue;
        const classNames = unit.classes.map((unitClass) => unitClass.name).sort();
        const key = `${sectorNumber}|${classNames.join(',')}`;
        const group = groups.get(key);
        if (group) {
          group.count++;
        } else {
          groups.set(key, {
            key,
            sectorNumber,
            classNames,
            label: classNames.join(' + '),
            count: 1,
          });
        }
      }
    }
    return [...groups.values()].sort(
      (a, b) => a.sectorNumber - b.sectorNumber || a.label.localeCompare(b.label),
    );
  });
  readonly buildingsTotal = computed(() => (this.player()?.buildings ?? []).length);
  readonly unitStats = computed(() => {
    const playerId = this.player()?.id ?? null;
    const placed = this.sectors()
      .flatMap((sector) => sector.army ?? [])
      .filter((unit) => unit.playerId === playerId).length;
    const total = this.reserveUnits().length + placed;
    const unclassified = this.unclassifiedUnits().length;
    return { total, classified: total - unclassified, unclassified };
  });
  readonly playerReady = computed(() => this.player()?.preparationReady === true);
  readonly sectorDetails = computed<SectorDetail[]>(() => {
    const playerId = this.player()?.id ?? null;
    const playerName = this.player()?.name;
    const names = this.ownerNames();
    return this.sectors()
      .filter((sector): sector is Sector & { number: number } => sector.number !== null)
      .sort((a, b) => a.number - b.number)
      .map((sector) => {
        const adjacentOwners = [
          ...new Set(
            (sector.neighbors ?? [])
              .map((number) => names[number])
              .filter((name): name is string => !!name && name !== playerName),
          ),
        ];
        return {
          number: sector.number,
          name: sector.name,
          incomeLabel:
            sector.income === null ? '—' : `${sector.income.toLocaleString('fr-FR')} ₡/tour`,
          resource: sector.resource ?? 'Aucune ressource',
          adjacentOwners,
          buildings: (this.player()?.buildings ?? []).filter(
            (building) => building.sectorNumber === sector.number,
          ).length,
          units: (sector.army ?? []).filter((unit) => unit.playerId === playerId).length,
          character: this.character()?.sectorNumber === sector.number,
        };
      });
  });
  readonly remainingLabel = computed(() => {
    const parts: string[] = [];
    if (this.characterUnplaced()) parts.push('le personnage');
    if (this.unplacedBuildings().length > 0) {
      parts.push(`${this.unplacedBuildings().length} bâtiment(s)`);
    }
    if (this.unitStats().unclassified > 0) {
      parts.push(`${this.unitStats().unclassified} unité(s) non classée(s)`);
    }
    if (parts.length === 0) return '';
    return (
      `Il reste ${parts.join(', ')}. Les éléments non placés iront sur le premier secteur ` +
      'et les unités non classées deviendront LÉGER.'
    );
  });
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
  readonly unitQuantity = signal(10);
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

  async unplaceCharacter(): Promise<void> {
    await this.run(
      () => firstValueFrom(this.api.unplacePreparationCharacter()),
      'Personnage retiré du placement',
    );
  }

  async unplaceBuilding(building: Building): Promise<void> {
    if (building.id === null) return;
    await this.run(
      () => firstValueFrom(this.api.unplacePreparationBuilding(building.id!)),
      `${building.displayName} retiré du placement`,
    );
  }

  async unplaceUnitGroup(group: PlacedUnitGroup): Promise<void> {
    await this.run(
      () =>
        firstValueFrom(
          this.api.unplacePreparationUnits(group.sectorNumber, group.count, group.classNames),
        ),
      `${group.count} unité(s) retirée(s) du placement`,
    );
  }

  async resetPlacement(): Promise<void> {
    const confirmed = await firstValueFrom(
      this.dialog
        .open(ConfirmDialogComponent, {
          data: {
            title: 'Tout annuler',
            message:
              'Retirer tous vos bâtiments, votre personnage et toutes vos unités du placement ?',
            confirmLabel: 'Tout annuler',
            cancelLabel: 'Annuler',
          },
        })
        .afterClosed(),
    );
    if (confirmed !== true) return;
    await this.run(
      () => firstValueFrom(this.api.resetPreparation()),
      'Placement de préparation réinitialisé',
    );
  }

  async markReady(): Promise<void> {
    await this.run(() => firstValueFrom(this.api.markPreparationReady()), 'Vous êtes prêt');
  }

  async unmarkReady(): Promise<void> {
    await this.run(
      () => firstValueFrom(this.api.unmarkPreparationReady()),
      'Statut « prêt » annulé',
    );
  }

  sectorLabel(number: number): string {
    const sector = this.sectors().find((candidate) => candidate.number === number);
    if (!sector) return `#${number}`;
    return `#${number} — ${sector.name}${sector.resource ? ` · ${sector.resource}` : ''}`;
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
      // Conserver la taille du lot : la remettre au restant pousse à tout poser d'un coup.
      const left = remaining - quantity;
      this.unitQuantity.set(left > 0 ? Math.min(quantity, left) : 1);
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
