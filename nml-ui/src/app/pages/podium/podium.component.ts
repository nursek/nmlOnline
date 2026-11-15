import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { GamePodium, PodiumPlace } from '../../models';
import { environment } from '../../../environments/environment';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { InstanceService } from '../../services/instance.service';
import { PlayerDirectoryService } from '../../services/player-directory.service';
import { httpErrorMessage } from '../../core/http-error.interceptor';
import { PlayerPhotoComponent } from '../../shared/player-photo/player-photo.component';
import { causeLabel, championLabel, medalEmoji, medalLabel } from './podium.helpers';

@Component({
  selector: 'app-podium',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    PlayerPhotoComponent,
  ],
  templateUrl: './podium.component.html',
  styleUrls: ['./podium.component.scss'],
})
export class PodiumComponent {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly instance = inject(InstanceService);
  private readonly directory = inject(PlayerDirectoryService);
  private readonly snackBar = inject(MatSnackBar);

  readonly podiumRef = httpResource<GamePodium>(() => `${environment.apiBaseUrl}/game/podium`);
  readonly podium = computed(() => (this.podiumRef.hasValue() ? this.podiumRef.value() : null));
  readonly places = computed(() => this.podium()?.places ?? []);
  readonly champion = computed(() => this.podium()?.champion ?? null);
  readonly topThree = computed(() => this.places().slice(0, 3));
  readonly steps = computed(() => {
    const [first, second, third] = this.topThree();
    return [second, first, third].filter((place): place is PodiumPlace => !!place);
  });
  readonly eliminatedChoices = computed(() => this.places().filter((place) => !place.alive));
  readonly playerColors = this.directory.colorMap;

  readonly selectedWinnerId = signal<number | null>(null);
  readonly saving = signal(false);
  readonly adjustError = signal<string | null>(null);

  readonly canAdmin = computed(
    () =>
      this.auth.isAdmin() ||
      this.instance.selectedRole() === 'ADMIN' ||
      this.instance.selectedRole() === 'MJ',
  );

  readonly causeLabel = causeLabel;
  readonly championLabel = championLabel;
  readonly medalEmoji = medalEmoji;
  readonly medalLabel = medalLabel;

  getPlayerColor(playerId: number): string {
    return this.playerColors().get(playerId) ?? 'var(--muted)';
  }

  retry(): void {
    this.podiumRef.reload();
  }

  print(): void {
    window.print();
  }

  onWinnerChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.selectedWinnerId.set(value ? Number(value) : null);
  }

  async adjustWinner(): Promise<void> {
    const playerId = this.selectedWinnerId();
    if (playerId === null || this.saving()) {
      return;
    }
    this.saving.set(true);
    this.adjustError.set(null);
    try {
      await firstValueFrom(this.api.adminAdjustWinner(playerId));
      this.snackBar.open('Champion désigné', 'OK', { duration: 2500 });
      this.selectedWinnerId.set(null);
      this.podiumRef.reload();
    } catch (error) {
      this.adjustError.set(httpErrorMessage(error, 'Impossible de désigner le champion'));
    } finally {
      this.saving.set(false);
    }
  }
}
