import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { map, exhaustMap, catchError, tap, timeout } from 'rxjs/operators';
import { Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { TokenService } from '../../services/token.service';
import { AuthActions } from './auth.actions';
import { PlayerActions } from '../player/player.actions';

@Injectable()
export class AuthEffects {
  private actions$ = inject(Actions);
  private apiService = inject(ApiService);
  private tokenService = inject(TokenService);
  private router = inject(Router);

  // Timeout pour les opérations d'authentification (15 secondes)
  private readonly AUTH_TIMEOUT_MS = 15000;

  login$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.login),
      exhaustMap(({ credentials }) =>
        this.apiService.login(credentials).pipe(
          timeout(this.AUTH_TIMEOUT_MS),
          map((response) => AuthActions.loginSuccess({ response })),
          catchError((error) =>
            of(AuthActions.loginFailure({
              error: this.getErrorMessage(error)
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
          timeout(this.AUTH_TIMEOUT_MS),
          map(() => AuthActions.logoutSuccess()),
          catchError(() => {
            // Même en cas d'erreur serveur, on déconnecte localement
            this.tokenService.clearAuth();
            return of(AuthActions.logoutSuccess());
          })
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
  // Utilise le TokenService qui gère le spam F5 via sessionStorage
  initSession$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.initSession),
      exhaustMap(() => {
        // Ne pas appeler le serveur si pas de token stocké
        if (!this.tokenService.hasStoredToken()) {
          return of(AuthActions.initSessionFailure());
        }

        // Utiliser le TokenService qui gère :
        // - Le cooldown entre les refreshes
        // - Le lock pour éviter les refreshes simultanés (spam F5)
        // - La persistance via sessionStorage
        return this.tokenService.refreshToken().pipe(
          map((token) => {
            const user = this.tokenService.getUser();
            if (token && user) {
              return AuthActions.initSessionSuccess({
                token: token,
                id: user.id,
                username: user.username
              });
            } else {
              return AuthActions.initSessionFailure();
            }
          }),
          catchError((error) => {
            // Si c'est juste un rate limit et qu'on a un token, considérer comme succès
            if (error?.message === 'Refresh rate limited' || error?.message === 'Refresh timeout') {
              const token = this.tokenService.getAccessToken();
              const user = this.tokenService.getUser();
              if (token && user) {
                return of(AuthActions.initSessionSuccess({
                  token: token,
                  id: user.id,
                  username: user.username
                }));
              }
            }
            // Erreur réelle, nettoyer
            this.tokenService.clearAuth();
            return of(AuthActions.initSessionFailure());
          })
        );
      })
    )
  );

  /**
   * Extrait un message d'erreur lisible.
   */
  private getErrorMessage(error: unknown): string {
    if (error && typeof error === 'object') {
      const err = error as { error?: { message?: string }; message?: string; name?: string };

      // Timeout
      if (err.name === 'TimeoutError') {
        return 'La connexion a pris trop de temps. Veuillez réessayer.';
      }

      // Erreur du serveur
      if (err.error?.message) {
        return err.error.message;
      }

      // Message générique
      if (err.message) {
        return err.message;
      }
    }

    return 'Une erreur est survenue lors de la connexion';
  }
}
