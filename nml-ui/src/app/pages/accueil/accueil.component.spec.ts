import { ApplicationRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AccueilComponent } from './accueil.component';
import { ActiveBoardService } from '../../services/active-board.service';
import { PlayerDirectoryService } from '../../services/player-directory.service';
import { AllianceStateService } from '../../services/alliance-state.service';
import { PlayerStatusService } from '../../services/player-status.service';
import type { Announcement, Player } from '../../models';

describe('AccueilComponent', () => {
  const board = {
    loading: signal(false),
    error: signal<string | null>(null),
    board: signal({
      sectors: {
        1: { ownerId: 1 },
        2: { ownerId: 2 },
        3: { ownerId: null },
      },
    }),
  };
  const directory = {
    loading: signal(false),
    players: signal([
      { id: 1, eliminated: false },
      { id: 2, eliminated: true },
    ] as Player[]),
    truncated: signal(false),
    colorMap: signal(new Map()),
  };
  const alliance = {
    announcements: signal<Announcement[]>([]),
    loadAnnouncements: jest.fn().mockResolvedValue(undefined),
  };
  const status = { gameFinished: signal(false) };

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActiveBoardService, useValue: board },
        { provide: PlayerDirectoryService, useValue: directory },
        { provide: AllianceStateService, useValue: alliance },
        { provide: PlayerStatusService, useValue: status },
      ],
    });
  });

  function component(): AccueilComponent {
    return TestBed.runInInjectionContext(() => new AccueilComponent());
  }

  it('calcule joueurs vivants, secteurs et statistiques de pertes', async () => {
    const page = component();
    const http = TestBed.inject(HttpTestingController);
    const appRef = TestBed.inject(ApplicationRef);

    expect(page.playersAlive()).toBe(1);
    expect(page.totalSectors()).toBe(3);
    expect(page.conqueredSectors()).toBe(2);
    expect(page.neutralSectors()).toBe(1);
    expect(alliance.loadAnnouncements).toHaveBeenCalled();

    page.turn();
    page.statistics();
    TestBed.tick();
    http.expectOne('/api/turn/current').flush(3);
    http
      .expectOne('/api/statistics')
      .flush({
        units: [
          { name: 'LARBIN', count: 2 },
          { name: 'BRUTE', count: 3 },
        ],
        vehicles: [{ name: 'TANK', count: 1 }],
      });
    await appRef.whenStable();
    TestBed.tick();

    expect(page.turn()).toBe(3);
    expect(page.unitDeaths()).toBe(5);
    expect(page.vehicleDeaths()).toBe(1);
    expect(page.statisticsError()).toBe(false);
  });

  it('formate chaque type d’annonce', () => {
    const page = component();
    const base = {
      id: 1,
      actorPlayerId: 1,
      actorName: 'lurio',
      targetPlayerId: 2,
      targetName: 'nursek',
      turn: 3,
      createdAt: '2026-11-25T12:00:00Z',
    } as unknown as Announcement;

    expect(page.announcementLabel({ ...base, type: 'ALLIANCE_FORMED' })).toBe(
      'lurio et nursek sont désormais alliés',
    );
    expect(page.announcementLabel({ ...base, type: 'ALLIANCE_BROKEN' })).toBe(
      'lurio et nursek ont rompu leur alliance',
    );
    expect(page.announcementLabel({ ...base, type: 'BETRAYAL' })).toBe('lurio a trahi nursek');
    expect(page.announcementLabel({ ...base, type: 'FINAL_DUEL' })).toContain(
      'la dernière alliance se brise',
    );
    expect(page.announcementLabel({ ...base, type: 'UNKNOWN' } as never)).toBe('Annonce');
  });
});
