import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { InstanceService } from '../services/instance.service';
import { waitForInitialization } from './auth.guard';
import { AuthService } from '../services/auth.service';

export const instanceGuard: CanActivateFn = async (): Promise<boolean | UrlTree> => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const instance = inject(InstanceService);

  await waitForInitialization(auth);
  let available = instance.instances();
  try {
    available = await instance.ensureLoaded();
  } catch {
    // Liste indisponible : on garde la partie déjà choisie plutôt que de bloquer la navigation.
  }

  if (available.length === 1) {
    instance.select(available[0]);
    return true;
  }

  const selectedId = instance.selectedId();
  if (selectedId === null) return router.createUrlTree(['/parties']);
  if (available.length > 0 && !available.some((candidate) => candidate.id === selectedId)) {
    instance.clear();
    return router.createUrlTree(['/parties']);
  }
  return true;
};
