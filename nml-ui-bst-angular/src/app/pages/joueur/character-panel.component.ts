import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { GameCharacter, Sector } from '../../models';
import { sectorForces, totalsStats, troopSummaries, vehicleLabels } from './joueur.helpers';
import { CharacterPortraitComponent } from '../../shared/character-portrait/character-portrait.component';
import {
  CharacterAbilitiesDialogComponent,
  CharacterAbilitiesDialogData,
} from './character-abilities-dialog.component';
import { ExpPipe } from '../../shared/exp.pipe';

/** Bloc « Personnage » de Mon Joueur : portrait + contexte du secteur où il se tient. */
@Component({
  selector: 'app-character-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, ExpPipe, MatIconModule, CharacterPortraitComponent],
  templateUrl: './character-panel.component.html',
  styleUrls: ['./character-panel.component.scss'],
})
export class CharacterPanelComponent {
  private readonly dialog = inject(MatDialog);

  readonly playerName = input.required<string>();
  readonly character = input.required<GameCharacter | null>();
  readonly sector = input<Sector | null>(null);
  readonly playerId = input<number | null>(null);

  readonly forces = computed(() => {
    const sector = this.sector();
    return sector ? sectorForces(sector, this.playerId()) : null;
  });

  readonly troops = computed(() => {
    const sector = this.sector();
    return sector ? troopSummaries([sector], this.playerId()) : [];
  });

  readonly totals = computed(() => {
    const forces = this.forces();
    return forces ? totalsStats(forces.totals) : [];
  });

  readonly vehicleChips = computed(() => {
    const forces = this.forces();
    if (!forces) return [];
    const { labels } = vehicleLabels(forces.vehicles);
    return forces.vehicles.map((v) => ({
      id: v.id,
      label: v.id != null ? (labels.get(v.id) ?? v.displayName) : v.displayName,
      pilotName: v.pilotName,
    }));
  });

  /** Vide hors personnage : celui-ci est déjà montré dans le cadre, inutile de le lister. */
  readonly isEmpty = computed(() => {
    const forces = this.forces();
    return (
      !forces ||
      (forces.units.length === 0 && forces.buildings.length === 0 && forces.vehicles.length === 0)
    );
  });

  openAbilities(): void {
    const character = this.character();
    if (!character) return;
    const data: CharacterAbilitiesDialogData = { playerName: this.playerName(), character };
    this.dialog.open(CharacterAbilitiesDialogComponent, {
      width: '1040px',
      maxWidth: '96vw',
      maxHeight: '95vh',
      autoFocus: false,
      data,
    });
  }
}
