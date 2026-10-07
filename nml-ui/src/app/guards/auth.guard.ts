import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { filter, take } from 'rxjs/operators';
import { toObservable } from '@angular/core/rxjs-interop';
import { AuthService } from '../services/auth.service';

export async function waitForInitialization(auth: AuthService): Promise<void> {
  if (auth.initialized()) return;
  await firstValueFrom(
    toObservable(auth.initialized).pipe(
      filter((v) => v),
      take(1),
    ),
  );
}

export const authGuard: CanActivateFn = async (_route, state): Promise<boolean | UrlTree> => {
  const auth = inject(AuthService);
  const router = inject(Router);

  await waitForInitialization(auth);

  if (auth.isAuthenticated()) return true;
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

export const noAuthGuard: CanActivateFn = async (): Promise<boolean | UrlTree> => {
  const auth = inject(AuthService);
  const router = inject(Router);

  await waitForInitialization(auth);

  if (!auth.isAuthenticated()) return true;
  return router.createUrlTree(['/carte']);
};
