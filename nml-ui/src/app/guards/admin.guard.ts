import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { InstanceService } from '../services/instance.service';
import { waitForInitialization } from './auth.guard';

export const adminGuard: CanActivateFn = async (_route, state): Promise<boolean | UrlTree> => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const instance = inject(InstanceService);

  await waitForInitialization(auth);

  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }
  const role = instance.selectedRole();
  if (auth.isAdmin() || role === 'ADMIN' || role === 'MJ') return true;
  return router.createUrlTree(['/accueil']);
};
