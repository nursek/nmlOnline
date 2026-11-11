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

  const selectedId = instance.selectedId();
  if (selectedId !== null && available.some((candidate) => candidate.id === selectedId)) {
    return true;
  }

  const mine = available.filter((candidate) => candidate.participant);
  if (mine.length === 1) {
    instance.select(mine[0]);
    return true;
  }

  if (selectedId === null) return router.createUrlTree(['/parties']);
  instance.clear();
  return router.createUrlTree(['/parties']);
};
