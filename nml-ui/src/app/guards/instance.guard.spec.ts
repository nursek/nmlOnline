import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { CanActivateFn } from '@angular/router';
import { instanceGuard } from './instance.guard';
import { InstanceService } from '../services/instance.service';
import { AuthService } from '../services/auth.service';
import { GameInstance } from '../models';
import { signal } from '@angular/core';

function instance(
  id: number,
  participant: boolean,
  role: GameInstance['role'] = 'PLAYER',
): GameInstance {
  return {
    id,
    name: `Partie ${id}`,
    currentTurn: 1,
    playerCount: 2,
    playerId: participant ? id * 10 : null,
    playerName: participant ? `Joueur ${id}` : null,
    eliminated: false,
    role,
    participant,
  };
}

function setup(
  instances: GameInstance[],
  selectedId: number | null,
  load: () => Promise<GameInstance[]>,
  isAdmin = false,
) {
  const selected = signal<number | null>(selectedId);
  const service = {
    instances: signal(instances),
    selectedId: selected.asReadonly(),
    select: jest.fn((value: GameInstance) => selected.set(value.id)),
    enter: jest.fn((value: GameInstance) => selected.set(value.id)),
    clear: jest.fn(() => selected.set(null)),
    ensureLoaded: jest.fn(load),
  };
  const router = {
    createUrlTree: jest.fn((commands: unknown[]) => ({ redirectTo: commands })),
    navigate: jest.fn((commands: unknown[]) => Promise.resolve(commands !== undefined)),
  };
  TestBed.configureTestingModule({
    providers: [
      { provide: InstanceService, useValue: service },
      { provide: AuthService, useValue: { initialized: signal(true), isAdmin: signal(isAdmin) } },
      { provide: Router, useValue: router },
    ],
  });
  return { service, router, selected };
}

async function run(url = '/accueil'): Promise<unknown> {
  const guard = instanceGuard as CanActivateFn;
  return TestBed.runInInjectionContext(() => guard({} as never, { url } as never));
}

describe('instanceGuard', () => {
  it('garde la sélection existante même sur une partie où le compte ne joue pas', async () => {
    const available = [instance(9, false, 'ADMIN'), instance(7, true)];
    const { service, selected } = setup(available, 9, () => Promise.resolve(available));

    await expect(run()).resolves.toBe(true);
    expect(service.select).not.toHaveBeenCalled();
    expect(selected()).toBe(9);
  });

  it('sélectionne la seule partie jouée quand aucune sélection valide n’existe', async () => {
    const available = [instance(9, false, 'ADMIN'), instance(7, true)];
    const { service, selected } = setup(available, null, () => Promise.resolve(available));

    await expect(run()).resolves.toBe(false);
    expect(service.enter).toHaveBeenCalledWith(expect.objectContaining({ id: 7 }), '/accueil');
    expect(selected()).toBe(7);
  });

  it('conserve la sélection quand la liste des parties ne peut pas être chargée', async () => {
    const { service, selected } = setup([], 7, () => Promise.reject(new Error('offline')));

    await expect(run()).resolves.toBe(true);
    expect(service.clear).not.toHaveBeenCalled();
    expect(selected()).toBe(7);
  });

  it("laisse un admin plateforme atteindre la carte quand aucun plateau n'existe", async () => {
    const { service } = setup([], null, () => Promise.resolve([]), true);

    await expect(run()).resolves.toBe(true);
    expect(service.enter).not.toHaveBeenCalled();
  });

  it('renvoie un joueur sans partie vers le sélecteur, même quand la base est vide', async () => {
    const { service } = setup([], null, () => Promise.resolve([]), false);

    await expect(run()).resolves.toEqual({ redirectTo: ['/parties'] });
    expect(service.enter).not.toHaveBeenCalled();
  });

  it('redirige vers le sélecteur sans sélection ni partie unique', async () => {
    const available = [instance(7, true), instance(8, true)];
    const { router } = setup(available, null, () => Promise.resolve(available));

    await expect(run()).resolves.toEqual({ redirectTo: ['/parties'] });
    expect(router.createUrlTree).toHaveBeenCalledWith(['/parties']);
  });

  it("ramène au sélecteur quand la partie stockée n'est plus accessible", async () => {
    const available = [instance(7, true)];
    const { service, router } = setup([], 9, () => Promise.resolve(available));

    await expect(run()).resolves.toBe(true);
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(service.clear).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/parties']);
  });

  it('renvoie au sélecteur quand la liste échoue et qu’aucune sélection n’existe', async () => {
    const { service } = setup([], null, () => Promise.reject(new Error('offline')));

    await expect(run()).resolves.toEqual({ redirectTo: ['/parties'] });
    expect(service.enter).not.toHaveBeenCalled();
  });

  it('conserve la route demandée quand il faut recharger pour sélectionner la partie', async () => {
    const available = [instance(7, true)];
    const { service } = setup(available, null, () => Promise.resolve(available));

    await expect(run('/carte')).resolves.toBe(false);
    expect(service.enter).toHaveBeenCalledWith(expect.objectContaining({ id: 7 }), '/carte');
  });

  it('ne renie pas une sélection stockée valide quand la liste arrive après coup', async () => {
    const available = [instance(7, true), instance(8, true)];
    const { service, router, selected } = setup([], 7, () => Promise.resolve(available));

    await expect(run()).resolves.toBe(true);
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(selected()).toBe(7);
    expect(service.clear).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
