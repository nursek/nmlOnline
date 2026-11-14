import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { GamePodium, PodiumPlace } from '../../models';
import { environment } from '../../../environments/environment';
import { PlayerPhotoComponent } from '../../shared/player-photo/player-photo.component';
import { championLabel, medalEmoji } from '../podium/podium.helpers';

@Component({
  selector: 'app-podium-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    PlayerPhotoComponent,
  ],
  templateUrl: './podium-panel.component.html',
  styleUrls: ['./podium-panel.component.scss'],
})
export class PodiumPanelComponent {
  readonly playerColors = input<ReadonlyMap<number, string>>(new Map());

  readonly podiumRef = httpResource<GamePodium>(
    () => `${environment.apiBaseUrl}/game/podium`,
  );
  readonly podium = computed(() => (this.podiumRef.hasValue() ? this.podiumRef.value() : null));
  readonly champion = computed(() => this.podium()?.champion ?? null);
  readonly topThree = computed(() => (this.podium()?.places ?? []).slice(0, 3));

  readonly championLabel = championLabel;
  readonly medalEmoji = medalEmoji;

  getPlayerColor(playerId: number): string {
    return this.playerColors().get(playerId) ?? 'var(--muted)';
  }

  retry(): void {
    this.podiumRef.reload();
  }

  trackPlace(_: number, place: PodiumPlace): number {
    return place.playerId;
  }
}
