import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { filter, switchMap, map, take } from 'rxjs/operators';
import { selectAuthLoading, selectIsAdmin } from '../store';

/**
 * Guard qui protège les routes admin.
 * Vérifie que l'utilisateur est authentifié ET a le rôle ADMIN.
 */
export const adminGuard: CanActivateFn = (route, state): Observable<boolean | UrlTree> => {
  const store = inject(Store);
  const router = inject(Router);

  return store.select(selectAuthLoading).pipe(
    filter((loading) => !loading),
    take(1),
    switchMap(() => store.select(selectIsAdmin).pipe(
      take(1),
      map(isAdmin => {
        if (isAdmin) {
          return true;
        }
        return router.createUrlTree(['/carte']);
      })
    ))
  );
};
