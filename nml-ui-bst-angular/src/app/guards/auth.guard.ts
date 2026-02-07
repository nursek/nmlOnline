import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { filter, map, take } from 'rxjs/operators';
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
    // Attendre que loading soit false (initialisation terminée)
    filter((loading) => !loading),
    take(1),
    // Puis vérifier l'authentification
    map(() => {
      // Sélectionner isAuthenticated de manière synchrone après que loading soit false
      let isAuthenticated = false;
      store.select(selectIsAuthenticated).pipe(take(1)).subscribe(auth => {
        isAuthenticated = auth;
      });

      if (isAuthenticated) {
        return true;
      }

      // Rediriger vers login avec l'URL de retour
      return router.createUrlTree(['/login'], {
        queryParams: { returnUrl: state.url }
      });
    })
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
    map(() => {
      let isAuthenticated = false;
      store.select(selectIsAuthenticated).pipe(take(1)).subscribe(auth => {
        isAuthenticated = auth;
      });

      if (!isAuthenticated) {
        return true;
      }

      // Déjà connecté, rediriger vers la carte
      return router.createUrlTree(['/carte']);
    })
  );
};
