import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { map, exhaustMap, catchError } from 'rxjs/operators';
import { ApiService } from '../../services/api.service';
import { AdminActions } from './admin.actions';

@Injectable()
export class AdminEffects {
  private readonly actions$ = inject(Actions);
  private readonly apiService = inject(ApiService);

  fetchAdminPlayers$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AdminActions.fetchAdminPlayers),
      exhaustMap(() =>
        this.apiService.adminGetAllPlayers().pipe(
          map((players) => AdminActions.fetchAdminPlayersSuccess({ players })),
          catchError((error) =>
            of(AdminActions.fetchAdminPlayersFailure({
              error: error.error?.message || error.message || 'Erreur lors de la récupération des joueurs'
            }))
          )
        )
      )
    )
  );

  importPlayer$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AdminActions.importPlayer),
      exhaustMap(({ file }) =>
        this.apiService.adminImportPlayer(file).pipe(
          map((player) => AdminActions.importPlayerSuccess({ player })),
          catchError((error) =>
            of(AdminActions.importPlayerFailure({
              error: error.error?.error || error.error?.message || error.message || 'Erreur lors de l\'import'
            }))
          )
        )
      )
    )
  );

  // Après un import réussi, recharger la liste des joueurs
  reloadAfterImport$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AdminActions.importPlayerSuccess),
      map(() => AdminActions.fetchAdminPlayers())
    )
  );

  deletePlayer$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AdminActions.deletePlayer),
      exhaustMap(({ playerId }) =>
        this.apiService.adminDeletePlayer(playerId).pipe(
          map(() => AdminActions.deletePlayerSuccess({ playerId })),
          catchError((error) =>
            of(AdminActions.deletePlayerFailure({
              error: error.error?.message || error.message || 'Erreur lors de la suppression'
            }))
          )
        )
      )
    )
  );
}
