import { TestBed } from '@angular/core/testing';
import { CanActivateFn, Router } from '@angular/router';
import { preparationGuard } from './preparation.guard';
import { InstanceService } from '../services/instance.service';
import { GameInstance } from '../models';

function run(current: GameInstance | null, loaded = true): Promise<unknown> {
  TestBed.resetTestingModule();
  let visible = loaded ? current : null;
  const router = {
    createUrlTree: jest.fn((commands: unknown[]) => ({ redirectTo: commands })),
  };
  const service = {
    selected: () => visible,
    ensureLoaded: jest.fn(() => {
      visible = current;
      return Promise.resolve([]);
    }),
  };
  TestBed.configureTestingModule({
    providers: [
      { provide: InstanceService, useValue: service },
      { provide: Router, useValue: router },
    ],
  });
  const guard = preparationGuard as CanActivateFn;
  return TestBed.runInInjectionContext(() => guard({} as never, {} as never)) as Promise<unknown>;
}

function instance(role: GameInstance['role'], currentTurn: number): GameInstance {
  return {
    id: 1,
    name: 'Partie',
    currentTurn,
    playerCount: 2,
    playerId: 10,
    playerName: 'Joueur',
    eliminated: false,
    role,
    participant: true,
  };
}

describe('preparationGuard', () => {
  it('renvoie un joueur en préparation vers son écran de placement', async () => {
    await expect(run(instance('PLAYER', 0))).resolves.toEqual({ redirectTo: ['/joueur'] });
  });

  it('charge les parties avant de décider sur un rechargement direct', async () => {
    await expect(run(instance('PLAYER', 0), false)).resolves.toEqual({
      redirectTo: ['/joueur'],
    });
  });

  it('laisse passer un joueur en cours de partie et les admins/MJ', async () => {
    await expect(run(instance('PLAYER', 1))).resolves.toBe(true);
    await expect(run(instance('MJ', 0))).resolves.toBe(true);
    await expect(run(instance('ADMIN', 0))).resolves.toBe(true);
    await expect(run(null)).resolves.toBe(true);
  });
});
