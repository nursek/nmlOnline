import { Injectable, computed } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { Board } from '../models';
import { environment } from '../../environments/environment';

/**
 * Board active unique (convention `boards[0]`) partagée par la carte et les
 * dialogs de déplacement : évite de recharger `/boards` dans chaque composant.
 */
@Injectable({ providedIn: 'root' })
export class ActiveBoardService {
  private readonly boardsRef = httpResource<Board[]>(() => ({
    url: `${environment.apiBaseUrl}/boards`,
  }));

  readonly board = computed(() => this.boardsRef.value()?.[0] ?? null);
  readonly sectors = computed(() => Object.values(this.board()?.sectors ?? {}));
  readonly loading = computed(() => this.boardsRef.isLoading());
  readonly error = computed(() => this.boardsRef.error());
}
