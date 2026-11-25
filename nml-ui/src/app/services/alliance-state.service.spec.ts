import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AllianceStateService } from './alliance-state.service';
import { AuthService } from './auth.service';
import type { AllianceMe } from '../models';

describe('AllianceStateService', () => {
  const user = signal({ id: 1, username: 'lurio' });
  let service: AllianceStateService;
  let http: HttpTestingController;

  beforeEach(() => {
    user.set({ id: 1, username: 'lurio' });
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: { user: user.asReadonly(), initialized: signal(true) },
        },
      ],
    });
    service = TestBed.inject(AllianceStateService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('charge l’alliance et expose les alliés', async () => {
    const loaded = service.loadMe();
    http.expectOne('/api/alliances/me').flush({
      alliedPlayers: [{ playerId: 2 }, { playerId: 3 }],
      incomingProposals: [],
      outgoingProposals: [],
      activeBetrayalBonuses: [],
      headquartersOperational: true,
    } as AllianceMe);
    await loaded;

    expect([...service.alliedPlayerIds()].sort()).toEqual([2, 3]);
    expect(service.loading()).toBe(false);

    const failed = service.loadMe();
    http
      .expectOne('/api/alliances/me')
      .flush({ detail: 'Partie introuvable' }, { status: 404, statusText: 'Absent' });
    await failed;
    expect(service.error()).toBe('Partie introuvable');
  });

  it('envoie chaque mutation puis recharge alliance et annonces', async () => {
    const actions = [
      ['propose', 'POST', '/api/alliances/proposals', () => service.propose('ALLIANCE', 2)],
      ['accept', 'POST', '/api/alliances/proposals/5/accept', () => service.accept(5)],
      ['decline', 'POST', '/api/alliances/proposals/5/decline', () => service.decline(5)],
      ['withdraw', 'DELETE', '/api/alliances/proposals/5', () => service.withdraw(5)],
      ['betray', 'POST', '/api/alliances/3/betray', () => service.betray(3)],
    ] as const;

    for (const [, method, url, call] of actions) {
      const mutation = call();
      const req = http.expectOne(url);
      expect(req.request.method).toBe(method);
      req.flush({});

      await Promise.resolve();
      http
        .expectOne('/api/alliances/me')
        .flush({ alliedPlayers: [{ playerId: 2 }] } as AllianceMe);
      await new Promise((resolve) => setTimeout(resolve, 0));
      http.expectOne('/api/announcements').flush([]);

      await expect(mutation).resolves.toBe(true);
    }
  });

  it('renvoie false et pose le message de repli sur refus', async () => {
    const mutation = service.propose('ALLIANCE', 2);
    http
      .expectOne('/api/alliances/proposals')
      .flush({ detail: 'Pas de QG' }, { status: 400, statusText: 'Erreur' });

    await expect(mutation).resolves.toBe(false);
    expect(service.error()).toBe('Pas de QG');
  });

  it('charge les annonces sans propager d’erreur', async () => {
    const loaded = service.loadAnnouncements();
    http.expectOne('/api/announcements').flush([{ id: 1 }] as never[]);
    await loaded;
    expect(service.announcements()).toHaveLength(1);

    const failed = service.loadAnnouncements();
    http
      .expectOne('/api/announcements')
      .flush({ detail: 'Indisponible' }, { status: 500, statusText: 'Erreur' });
    await expect(failed).resolves.toBeUndefined();
    expect(service.error()).toBeNull();
  });

  it('se vide à la déconnexion', () => {
    service.reset();
    expect(service.me()).toBeNull();
    expect(service.announcements()).toEqual([]);
  });
});
