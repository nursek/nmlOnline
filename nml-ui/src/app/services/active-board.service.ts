import { Injectable, computed, inject } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { Board } from '../models';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

/**
 * Board active unique (convention `boards[0]`) partagée par la carte et les
 * dialogs de déplacement : évite de recharger `/boards` dans chaque composant.
 */
@Injectable({ providedIn: 'root' })
export class ActiveBoardService {
  private readonly auth = inject(AuthService);

  // /boards dépend du joueur (visibilité) : requête liée à la session, vidée au logout.
  private readonly boardsRef = httpResource<Board[]>(() =>
    this.auth.user() ? { url: `${environment.apiBaseUrl}/boards` } : undefined,
  );

  readonly board = computed(() => this.boardsRef.value()?.[0] ?? null);
  readonly sectors = computed(() => Object.values(this.board()?.sectors ?? {}));
  readonly loading = computed(() => this.boardsRef.isLoading());
  readonly error = computed(() => this.boardsRef.error());
}
