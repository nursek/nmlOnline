import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { GameCharacter } from '../../models';
import { characterStats } from '../../core/stats';

/** Cadre doré du personnage : illustration + nom + stats. `size="lg"` pour le pop-up Capacités. */
@Component({
  selector: 'app-character-portrait',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, MatIconModule, MatTooltipModule],
  templateUrl: './character-portrait.component.html',
  styleUrls: ['./character-portrait.component.scss'],
})
export class CharacterPortraitComponent {
  readonly playerName = input.required<string>();
  readonly character = input.required<GameCharacter>();
  readonly sectorNumber = input<number | null>(null);
  readonly size = input<'sm' | 'lg'>('sm');
  readonly interactive = input(false);
  readonly activate = output<void>();

  readonly imgError = signal(false);

  // playerName et non character.name : le dossier d'assets porte le nom du compte
  // (nursek) alors que le personnage peut s'appeler autrement (Ratcatcher).
  readonly portraitUrl = computed(() =>
    this.playerName() ? `assets/${this.playerName().toLowerCase()}/characters/portrait.png` : '',
  );

  readonly stats = computed(() => characterStats(this.character()));

  onActivate(): void {
    if (this.interactive()) {
      this.activate.emit();
    }
  }

  onImgError(): void {
    this.imgError.set(true);
  }
}
