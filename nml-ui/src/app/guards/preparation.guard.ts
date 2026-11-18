import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { InstanceService } from '../services/instance.service';

/** Tour 0 : le joueur prépare ses forces, les écrans de jeu (boutique, ordres, rapports) sont fermés. */
export const preparationGuard: CanActivateFn = async (): Promise<boolean | UrlTree> => {
  const instance = inject(InstanceService);
  const router = inject(Router);
  try {
    await instance.ensureLoaded();
  } catch {
    // Liste indisponible : la garde de partie affichera l'erreur de chargement.
    return true;
  }
  const inPreparation = () => {
    const current = instance.selected();
    return current?.currentTurn === 0 && current.role === 'PLAYER';
  };
  if (!inPreparation()) return true;
  // La liste est mise en cache pour la page : sans relecture, le joueur resterait bloqué après le lancement.
  await instance.load().catch(() => undefined);
  return inPreparation() ? router.createUrlTree(['/joueur']) : true;
};
