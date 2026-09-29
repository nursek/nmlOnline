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

  beforeEach(() => {
    initialized.set(true);
    isAdmin.set(false);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: { initialized, isAdmin },
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
    await TestBed.inject(ApplicationRef).whenStable();

    expect(service.available()).toBe(true);
    expect(service.combatScenarios()).toEqual([]);
  });
});
