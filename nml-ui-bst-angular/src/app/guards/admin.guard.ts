import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { waitForInitialization } from './auth.guard';

/**
 * Guard: admin-only routes. Verifies the logged-in user has the ADMIN role.
 * Unauthenticated users are redirected to /login; authenticated non-admins
 * are redirected to /carte.
 */
export const adminGuard: CanActivateFn = async (_route, state): Promise<boolean | UrlTree> => {
  const auth = inject(AuthService);
  const router = inject(Router);

  await waitForInitialization(auth);

  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }
  if (auth.isAdmin()) return true;
  return router.createUrlTree(['/carte']);
};
