import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { InstanceService } from '../services/instance.service';
import { waitForInitialization } from './auth.guard';
import { AuthService } from '../services/auth.service';

export const instanceGuard: CanActivateFn = async (_route, state): Promise<boolean | UrlTree> => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const instance = inject(InstanceService);

  await waitForInitialization(auth);

  const selectedId = instance.selectedId();
  if (selectedId !== null) {
    const known = instance.instances();
    if (known.length === 0) {
      // Sélection stockée : on navigue sans attendre la liste, sa validité est vérifiée après coup.
      void instance
        .ensureLoaded()
        .then((available) => {
          if (!available.some((candidate) => candidate.id === selectedId)) {
            instance.clear();
            void router.navigate(['/parties']);
          }
        })
        .catch(() => undefined);
      return true;
    }
    if (known.some((candidate) => candidate.id === selectedId)) return true;
    instance.clear();
  }

  let available = instance.instances();
  try {
    available = await instance.ensureLoaded();
  } catch {
    // Aucune sélection exploitable : le choix de partie est préférable à un écran bloqué.
    return router.createUrlTree(['/parties']);
  }

  if (available.length === 0) {
    // Installation vide : seul un admin plateforme peut importer le premier plateau (depuis la carte).
    return auth.isAdmin() ? true : router.createUrlTree(['/parties']);
  }

  const mine = available.filter((candidate) => candidate.participant);
  if (mine.length === 1) {
    instance.enter(mine[0], state.url);
    return false;
  }
  return router.createUrlTree(['/parties']);
};
