import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { GameCharacter } from '../../models';
import { ABILITY_KIND_LABELS, Ability, AbilityKind, abilitiesFor } from '../../core/abilities';
import { CharacterPortraitComponent } from '../../shared/character-portrait/character-portrait.component';

export interface CharacterAbilitiesDialogData {
  playerName: string;
  character: GameCharacter;
}

/** Pop-up Capacités : grande illustration, description statique puis les 3 emplacements empilés. */
@Component({
  selector: 'app-character-abilities-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatDialogModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    CharacterPortraitComponent,
  ],
  templateUrl: './character-abilities-dialog.component.html',
  styleUrls: ['./character-abilities-dialog.component.scss'],
})
export class CharacterAbilitiesDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<CharacterAbilitiesDialogComponent>);
  readonly data: CharacterAbilitiesDialogData = inject(MAT_DIALOG_DATA);

  readonly abilities = computed(() => abilitiesFor(this.data.playerName));

  readonly abilityList = computed<Ability[]>(() => {
    const abilities = this.abilities();
    return [abilities.character, abilities.army, abilities.territory];
  });

  kindLabel(kind: AbilityKind): string {
    return ABILITY_KIND_LABELS[kind];
  }

  close(): void {
    this.dialogRef.close();
  }
}
