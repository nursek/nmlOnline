import { ApplicationRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DevScenarioService } from './dev-scenario.service';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

describe('DevScenarioService (probe dev)', () => {
  const initialized = signal(true);
  const isAdmin = signal(false);
  const user = signal(null);

  beforeEach(() => {
    initialized.set(true);
    isAdmin.set(false);
    user.set(null);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: { initialized, isAdmin, user: user.asReadonly() },
        },
      ],
    });
  });

  it('ne sonde pas les endpoints dev hors session admin', () => {
    isAdmin.set(true);
    initialized.set(false);

    TestBed.inject(DevScenarioService);
    TestBed.tick();

    initialized.set(true);
    isAdmin.set(false);
    TestBed.tick();

    TestBed.inject(HttpTestingController).verify();
  });

  it('sonde les endpoints dev au passage admin et expose le statut', async () => {
    const service = TestBed.inject(DevScenarioService);

    isAdmin.set(true);
    TestBed.tick();

    const http = TestBed.inject(HttpTestingController);
    http
      .expectOne(`${environment.apiBaseUrl}/admin/dev/seed-resolution-scenario`)
      .flush({ available: true });
    http.expectOne(`${environment.apiBaseUrl}/admin/dev/combat-scenarios`).flush([]);
    http.expectOne(`${environment.apiBaseUrl}/admin/turn/current`).flush({ currentTurn: 1 });
    await TestBed.inject(ApplicationRef).whenStable();

    expect(service.available()).toBe(true);
    expect(service.combatScenarios()).toEqual([]);
    expect(service.demoAvailable()).toBe(true);
  });

  it('joue la démo puis relit le tour, la carte et les joueurs', async () => {
    const service = TestBed.inject(DevScenarioService);
    isAdmin.set(true);
    TestBed.tick();

    const http = TestBed.inject(HttpTestingController);
    http
      .expectOne(`${environment.apiBaseUrl}/admin/dev/seed-resolution-scenario`)
      .flush({ available: true });
    http.expectOne(`${environment.apiBaseUrl}/admin/dev/combat-scenarios`).flush([]);
    http.expectOne(`${environment.apiBaseUrl}/admin/turn/current`).flush({ currentTurn: 1 });
    await TestBed.inject(ApplicationRef).whenStable();

    const played = service.playDemoTurns();
    http
      .expectOne(`${environment.apiBaseUrl}/admin/dev/play-demo-turns`)
      .flush({ turn: 3, message: 'Tours 1 et 2 joués', observations: ['Ordre lurio 43→38 sauté'] });
    await played;

    TestBed.tick();
    http.expectOne(`${environment.apiBaseUrl}/admin/turn/current`).flush({ currentTurn: 3 });
    await TestBed.inject(ApplicationRef).whenStable();

    expect(service.demoReport()?.turn).toBe(3);
    expect(service.demoReport()?.observations).toEqual(['Ordre lurio 43→38 sauté']);
    expect(service.demoAvailable()).toBe(false);
    expect(service.error()).toBeNull();
  });
});
