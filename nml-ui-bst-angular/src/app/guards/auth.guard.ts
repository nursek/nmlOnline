import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { filter, switchMap, map, take } from 'rxjs/operators';
import { selectIsAuthenticated, selectAuthLoading } from '../store';

/**
 * Guard qui protège les routes nécessitant une authentification.
 * Attend que l'initialisation de la session soit terminée avant de vérifier.
 */
export const authGuard: CanActivateFn = (route, state): Observable<boolean | UrlTree> => {
  const store = inject(Store);
  const router = inject(Router);

  // Attendre que le loading soit terminé (initSession terminé)
  return store.select(selectAuthLoading).pipe(
    filter((loading) => !loading),
    take(1),
    switchMap(() => store.select(selectIsAuthenticated).pipe(
      take(1),
      map(isAuthenticated => {
        if (isAuthenticated) {
          return true;
        }
        return router.createUrlTree(['/login'], {
          queryParams: { returnUrl: state.url }
        });
      })
    ))
  );
};

/**
 * Guard pour les routes qui ne doivent être accessibles que si NON authentifié.
 * Exemple: page de login, register.
 */
export const noAuthGuard: CanActivateFn = (): Observable<boolean | UrlTree> => {
  const store = inject(Store);
  const router = inject(Router);

  return store.select(selectAuthLoading).pipe(
    filter((loading) => !loading),
    take(1),
    switchMap(() => store.select(selectIsAuthenticated).pipe(
      take(1),
      map(isAuthenticated => {
        if (!isAuthenticated) {
          return true;
        }
        return router.createUrlTree(['/carte']);
      })
    ))
  );
};
