import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { map, exhaustMap, catchError } from 'rxjs/operators';
import { ApiService } from '../../services/api.service';
import { PlayerActions } from './player.actions';

@Injectable()
export class PlayerEffects {
  private actions$ = inject(Actions);
  private apiService = inject(ApiService);

  fetchCurrentPlayer$ = createEffect(() =>
    this.actions$.pipe(
      ofType(PlayerActions.fetchCurrentPlayer),
      exhaustMap(({ username }) =>
        this.apiService.getPlayer(username).pipe(
          map((player) => PlayerActions.fetchCurrentPlayerSuccess({ player })),
          catchError((error) => {
            let errorMessage = 'Erreur lors de la récupération du joueur';
            if (error.status === 404) {
              errorMessage = `Aucun profil de joueur trouvé pour "${username}". Créez un joueur avec ce nom dans le jeu.`;
            } else if (error.error?.message) {
              errorMessage = error.error.message;
            } else if (error.message) {
              errorMessage = error.message;
            }
            return of(PlayerActions.fetchCurrentPlayerFailure({ error: errorMessage }));
          })
        )
      )
    )
  );

  fetchAllPlayers$ = createEffect(() =>
    this.actions$.pipe(
      ofType(PlayerActions.fetchAllPlayers),
      exhaustMap(() =>
        this.apiService.getAllPlayers().pipe(
          map((players) => PlayerActions.fetchAllPlayersSuccess({ players })),
          catchError((error) =>
            of(PlayerActions.fetchAllPlayersFailure({
              error: error.error?.message || error.message || 'Erreur lors de la récupération des joueurs'
            }))
          )
        )
      )
    )
  );
}
