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

function build(isAdmin: boolean, accessible: number): NavbarComponent {
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    providers: [
      {
        provide: AuthService,
        useValue: {
          isAuthenticated: signal(true),
          isAdmin: signal(isAdmin),
          user: signal({ id: 1, username: 'lurio' }),
        },
      },
      {
        provide: InstanceService,
        useValue: {
          instances: signal(instances(accessible)),
          selectedRole: signal(null),
        },
      },
      { provide: Router, useValue: { navigate: jest.fn() } },
      { provide: DevScenarioService, useValue: { available: signal(false) } },
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
});
