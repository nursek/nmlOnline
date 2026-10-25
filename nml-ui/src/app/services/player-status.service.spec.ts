import { ApplicationRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { PlayerStatusService } from './player-status.service';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

describe('PlayerStatusService (lecture seule)', () => {
  const initialized = signal(true);
  const isAdmin = signal(false);
  const user = signal<{ username: string } | null>({ username: 'lurio' });

  const flushStatus = (http: HttpTestingController, eliminated: boolean) =>
    http.expectOne(`${environment.apiBaseUrl}/players/me/status`).flush({
      eliminated,
      eliminationCause: eliminated ? 'MAIN_CHARACTER_KILLED' : null,
      eliminatedTurn: eliminated ? 3 : null,
      eliminatedByPlayerId: eliminated ? 2 : null,
    });

  beforeEach(() => {
    initialized.set(true);
    isAdmin.set(false);
    user.set({ username: 'lurio' });
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { initialized, isAdmin, user: user.asReadonly() } },
      ],
    });
  });

  it("garde l'état éliminé quand la relecture du statut échoue", async () => {
    const service = TestBed.inject(PlayerStatusService);
    const http = TestBed.inject(HttpTestingController);
    TestBed.tick();

    flushStatus(http, true);
    await TestBed.inject(ApplicationRef).whenStable();
    expect(service.eliminated()).toBe(true);

    service.reload();
    TestBed.tick();
    http.expectOne(`${environment.apiBaseUrl}/players/me/status`).error(new ProgressEvent('error'));
    await TestBed.inject(ApplicationRef).whenStable();
    expect(service.eliminated()).toBe(true);
  });

  it('ignore la lecture seule pour un compte MJ', async () => {
    isAdmin.set(true);
    const service = TestBed.inject(PlayerStatusService);
    const http = TestBed.inject(HttpTestingController);
    TestBed.tick();

    flushStatus(http, true);
    await TestBed.inject(ApplicationRef).whenStable();
    expect(service.status().eliminated).toBe(true);
    expect(service.eliminated()).toBe(false);
  });
});
