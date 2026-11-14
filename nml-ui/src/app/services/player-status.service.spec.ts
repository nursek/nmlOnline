import { ApplicationRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { PlayerStatusService } from './player-status.service';
import { AuthService } from './auth.service';
import { PlayerStatus } from '../models';
import { environment } from '../../environments/environment';

const STATUS: PlayerStatus = {
  eliminated: false,
  eliminationCause: null,
  eliminatedTurn: null,
  eliminatedByPlayerId: null,
  participant: true,
  gameFinished: false,
  finalTurn: null,
  winnerPlayerId: null,
  winnerName: null,
  winnerPosthumous: false,
  winnerAdjusted: false,
  winner: false,
};

describe('PlayerStatusService (lecture seule)', () => {
  const initialized = signal(true);
  const isAdmin = signal(false);
  const user = signal<{ username: string } | null>({ username: 'lurio' });

  const flushStatus = (http: HttpTestingController, overrides: Partial<PlayerStatus> = {}) =>
    http
      .expectOne(`${environment.apiBaseUrl}/players/me/status`)
      .flush({ ...STATUS, ...overrides });

  const eliminatedStatus = (): Partial<PlayerStatus> => ({
    eliminated: true,
    eliminationCause: 'MAIN_CHARACTER_KILLED',
    eliminatedTurn: 3,
    eliminatedByPlayerId: 2,
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

    flushStatus(http, eliminatedStatus());
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

    flushStatus(http, eliminatedStatus());
    await TestBed.inject(ApplicationRef).whenStable();
    expect(service.status().eliminated).toBe(true);
    expect(service.eliminated()).toBe(false);
  });

  it('expose la fin de partie, le champion et le verrouillage', async () => {
    const service = TestBed.inject(PlayerStatusService);
    const http = TestBed.inject(HttpTestingController);
    TestBed.tick();

    flushStatus(http, {
      gameFinished: true,
      finalTurn: 7,
      winnerPlayerId: 3,
      winnerName: 'nursek',
      winner: true,
      winnerPosthumous: true,
      winnerAdjusted: true,
    });
    await TestBed.inject(ApplicationRef).whenStable();

    expect(service.gameFinished()).toBe(true);
    expect(service.winner()).toBe(true);
    expect(service.winnerName()).toBe('nursek');
    expect(service.winnerPosthumous()).toBe(true);
    expect(service.winnerAdjusted()).toBe(true);
    expect(service.locked()).toBe(true);
  });

  it('ne verrouille pas un compte qui ne participe pas à la partie', async () => {
    const service = TestBed.inject(PlayerStatusService);
    const http = TestBed.inject(HttpTestingController);
    TestBed.tick();

    flushStatus(http, { participant: false, gameFinished: true, winnerName: 'nursek' });
    await TestBed.inject(ApplicationRef).whenStable();

    expect(service.gameFinished()).toBe(true);
    expect(service.locked()).toBe(false);
  });
});
