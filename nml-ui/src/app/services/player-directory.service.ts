import { Injectable, computed, inject } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { PageResult, Player } from '../models';
import { environment } from '../../environments/environment';
import { assignPlayerColors } from '../pages/carte/carte.config';
import { AuthService } from './auth.service';

// Annuaire des joueurs partagé par la carte (légende, noms des propriétaires) et l'accueil (stats, classements) : un seul fetch et une seule palette.
@Injectable({ providedIn: 'root' })
export class PlayerDirectoryService {
  private readonly auth = inject(AuthService);

  // /players dépend du joueur (visibilité) : requête liée à la session, vidée au logout.
  private readonly playersRef = httpResource<PageResult<Player>>(() =>
    this.auth.user()
      ? {
          url: `${environment.apiBaseUrl}/players`,
          params: { page: '0', size: '50' },
        }
      : undefined,
  );

  readonly players = computed(() => this.playersRef.value()?.content ?? []);
  readonly colorMap = computed(() => assignPlayerColors(this.players()));
  readonly loading = computed(() => this.playersRef.isLoading());
  readonly error = computed(() => this.playersRef.error());
  readonly truncated = computed(() => {
    const page = this.playersRef.value();
    return !!page && page.content.length < (page.totalElements ?? page.content.length);
  });

  reload(): void {
    this.playersRef.reload();
  }
}
