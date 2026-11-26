import { DOCUMENT } from '@angular/common';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { BreakpointObserver } from '@angular/cdk/layout';
import { of } from 'rxjs';
import { NavbarComponent } from './navbar.component';
import { AuthService } from '../../services/auth.service';
import { InstanceService } from '../../services/instance.service';
import { DevScenarioService } from '../../services/dev-scenario.service';
import { PlayerStatusService } from '../../services/player-status.service';
import { GameInstance } from '../../models';

function instances(count: number): GameInstance[] {
  return Array.from({ length: count }, (_, index) => ({
    id: index + 1,
    name: `Partie ${index + 1}`,
    currentTurn: 1,
    playerCount: 2,
    playerId: index + 1,
    playerName: `Joueur ${index + 1}`,
    eliminated: false,
    role: 'PLAYER' as const,
    participant: true,
  }));
}

const router = { navigate: jest.fn() };
const logout = jest.fn();

function build(
  isAdmin: boolean,
  accessible: number,
  finished = false,
  scenarios = false,
): NavbarComponent {
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    providers: [
      {
        provide: AuthService,
        useValue: {
          isAuthenticated: signal(true),
          isAdmin: signal(isAdmin),
          canAdmin: signal(isAdmin),
          user: signal({ id: 1, username: 'lurio' }),
          logout,
        },
      },
      {
        provide: InstanceService,
        useValue: {
          instances: signal(instances(accessible)),
          selectedRole: signal(null),
        },
      },
      {
        provide: PlayerStatusService,
        useValue: { gameFinished: signal(finished) },
      },
      { provide: Router, useValue: router },
      { provide: DevScenarioService, useValue: { available: signal(scenarios) } },
      { provide: BreakpointObserver, useValue: { observe: () => of({ matches: false }) } },
      { provide: DOCUMENT, useValue: document },
    ],
  });
  return TestBed.runInInjectionContext(() => new NavbarComponent());
}

describe('NavbarComponent — « Changer de partie »', () => {
  it('reste masqué quand une seule partie est accessible', () => {
    expect(build(false, 1).canSwitchInstance()).toBe(false);
  });

  it('apparaît dès que plusieurs parties sont accessibles', () => {
    expect(build(false, 2).canSwitchInstance()).toBe(true);
    expect(build(true, 3).canSwitchInstance()).toBe(true);
  });

  it('masque les scénarios dev aux admins quand le seeder est indisponible', () => {
    const hidden = build(true, 1, false, false).menuItems();
    const shown = build(true, 1, false, true).menuItems();

    expect(hidden.some((item) => item.path === '/admin/scenarios')).toBe(false);
    expect(shown.some((item) => item.path === '/admin/scenarios')).toBe(true);
  });

  it('change de partie et se déconnecte', () => {
    router.navigate.mockClear();
    logout.mockClear();
    const navbar = build(false, 2);

    navbar.switchInstance();
    expect(router.navigate).toHaveBeenCalledWith(['/parties']);
    expect(navbar.drawerOpen()).toBe(false);

    navbar.logout();
    expect(logout).toHaveBeenCalled();
  });

  it('ouvre et ferme le tiroir mobile', () => {
    const navbar = build(false, 1);

    navbar.toggleDrawer();
    expect(navbar.drawerOpen()).toBe(true);
    navbar.toggleDrawer();
    expect(navbar.drawerOpen()).toBe(false);
  });

  it('ajoute le lien Podium seulement quand la partie est terminée', () => {
    const running = build(false, 1, false).menuItems();
    const finished = build(false, 1, true).menuItems();

    expect(running.some((item) => item.path === '/podium')).toBe(false);
    expect(finished.some((item) => item.path === '/podium')).toBe(true);
  });
});
