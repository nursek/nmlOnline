import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { optimizedImage } from '../../core/optimized-image';
import type { OptimizedImage } from '../../core/optimized-image';
import { SigilComponent } from '../sigil/sigil.component';

@Component({
  selector: 'app-player-photo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [SigilComponent],
  templateUrl: './player-photo.component.html',
  styleUrls: ['./player-photo.component.scss'],
})
export class PlayerPhotoComponent {
  readonly playerName = input.required<string>();
  readonly characterName = input<string | null>(null);
  readonly caption = input<string | null>(null);
  readonly variant = input<'podium' | 'newsprint'>('podium');
  readonly color = input<string | null>(null);

  readonly imgError = signal(false);

  readonly portrait = computed<OptimizedImage | null>(() => {
    const key = this.playerName().trim().toLowerCase();
    if (!/^[a-z0-9_-]+$/.test(key)) {
      return null;
    }
    return optimizedImage(`assets/${key}/characters/portrait.png`);
  });

  readonly showFallback = computed(() => !this.portrait() || this.imgError());

  readonly alt = computed(() => {
    const caption = this.caption();
    if (caption) {
      return caption;
    }
    const character = this.characterName();
    return character
      ? `Portrait de ${this.playerName()} (${character})`
      : `Portrait de ${this.playerName()}`;
  });
}
