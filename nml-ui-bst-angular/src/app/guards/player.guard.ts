import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { waitForInitialization } from './auth.guard';

/** Les admins sont renvoyés vers /admin : les pages joueur supposent un profil Player. */
export const playerGuard: CanActivateFn = async (_route, state): Promise<boolean | UrlTree> => {
  const auth = inject(AuthService);
  const router = inject(Router);

  await waitForInitialization(auth);

  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }
  if (auth.isAdmin()) return router.createUrlTree(['/admin']);
  return true;
};
