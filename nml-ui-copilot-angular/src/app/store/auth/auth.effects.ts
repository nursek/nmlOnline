import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { map, exhaustMap, catchError, tap, switchMap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AuthActions } from './auth.actions';
import { PlayerActions } from '../player/player.actions';

@Injectable()
export class AuthEffects {
  private actions$ = inject(Actions);
  private apiService = inject(ApiService);
  private router = inject(Router);

  login$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.login),
      exhaustMap(({ credentials }) =>
        this.apiService.login(credentials).pipe(
          map((response) => AuthActions.loginSuccess({ response })),
          catchError((error) =>
            of(AuthActions.loginFailure({
              error: error.error?.message || error.message || 'Erreur de connexion'
            }))
          )
        )
      )
    )
  );

  loginSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(AuthActions.loginSuccess),
        tap(() => {
          this.router.navigate(['/carte']);
        })
      ),
    { dispatch: false }
  );

  logout$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.logout),
      exhaustMap(() =>
        this.apiService.logout().pipe(
          map(() => AuthActions.logoutSuccess()),
          catchError(() => of(AuthActions.logoutSuccess()))
        )
      )
    )
  );

  // Quand logout success, reset le player state et rediriger
  logoutSuccess$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.logoutSuccess),
      tap(() => {
        this.router.navigate(['/login']);
      }),
      map(() => PlayerActions.reset())
    )
  );

  // Refresh token au démarrage de l'app
  initSession$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.initSession),
      exhaustMap(() =>
        this.apiService.refreshToken().pipe(
          map((response) => {
            if (response.valid && response.token && response.id && response.name) {
              return AuthActions.initSessionSuccess({
                token: response.token,
                id: response.id,
                username: response.name
              });
            } else {
              return AuthActions.initSessionFailure();
            }
          }),
          catchError(() => of(AuthActions.initSessionFailure()))
        )
      )
    )
  );
}
