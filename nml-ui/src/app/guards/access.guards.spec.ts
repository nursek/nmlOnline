import { TestBed } from '@angular/core/testing';
import { CanActivateFn, Router } from '@angular/router';
import { adminGuard } from './admin.guard';
import { authGuard, noAuthGuard } from './auth.guard';
import { playerGuard } from './player.guard';
import { platformAdminGuard } from './platform-admin.guard';
import { AuthService } from '../services/auth.service';

interface Access {
  authenticated: boolean;
  admin?: boolean;
  boardAdmin?: boolean;
}

function run(guard: CanActivateFn, access: Access, url = '/joueur'): Promise<unknown> {
  const router = {
    createUrlTree: jest.fn((commands: unknown[], extras?: { queryParams?: unknown }) => ({
      redirectTo: commands,
      queryParams: extras?.queryParams,
    })),
  };
  const auth = {
    initialized: () => true,
    isAuthenticated: () => access.authenticated,
    isAdmin: () => access.admin === true,
    canAdmin: () => access.admin === true || access.boardAdmin === true,
  };
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    providers: [
      { provide: AuthService, useValue: auth },
      { provide: Router, useValue: router },
    ],
  });
  const state = { url } as never;
  return Promise.resolve(TestBed.runInInjectionContext(() => guard({} as never, state)));
}

describe('guards d’accès', () => {
  it('authGuard renvoie vers le login avec returnUrl, sinon laisse passer', async () => {
    await expect(run(authGuard, { authenticated: false }, '/ordres')).resolves.toEqual({
      redirectTo: ['/login'],
      queryParams: { returnUrl: '/ordres' },
    });
    await expect(run(authGuard, { authenticated: true })).resolves.toBe(true);
  });

  it('noAuthGuard renvoie un connecté vers l’accueil et laisse passer un anonyme', async () => {
    await expect(run(noAuthGuard, { authenticated: true })).resolves.toEqual({
      redirectTo: ['/accueil'],
      queryParams: undefined,
    });
    await expect(run(noAuthGuard, { authenticated: false })).resolves.toBe(true);
  });

  it('adminGuard exige un admin de partie ou plateforme', async () => {
    await expect(run(adminGuard, { authenticated: false }, '/admin')).resolves.toEqual({
      redirectTo: ['/login'],
      queryParams: { returnUrl: '/admin' },
    });
    await expect(run(adminGuard, { authenticated: true })).resolves.toEqual({
      redirectTo: ['/accueil'],
      queryParams: undefined,
    });
    await expect(run(adminGuard, { authenticated: true, boardAdmin: true })).resolves.toBe(true);
    await expect(run(adminGuard, { authenticated: true, admin: true })).resolves.toBe(true);
  });

  it('playerGuard renvoie les admins vers /admin et laisse passer un joueur', async () => {
    await expect(run(playerGuard, { authenticated: false })).resolves.toEqual({
      redirectTo: ['/login'],
      queryParams: { returnUrl: '/joueur' },
    });
    await expect(run(playerGuard, { authenticated: true, boardAdmin: true })).resolves.toEqual({
      redirectTo: ['/admin'],
      queryParams: undefined,
    });
    await expect(run(playerGuard, { authenticated: true })).resolves.toBe(true);
  });

  it('platformAdminGuard refuse un MJ de partie, seul ADMIN passe', async () => {
    await expect(run(platformAdminGuard, { authenticated: false })).resolves.toEqual({
      redirectTo: ['/login'],
      queryParams: { returnUrl: '/joueur' },
    });
    await expect(run(platformAdminGuard, { authenticated: true, admin: true })).resolves.toBe(true);
    await expect(run(platformAdminGuard, { authenticated: true, boardAdmin: true })).resolves.toEqual({
      redirectTo: ['/accueil'],
      queryParams: undefined,
    });
  });
});
