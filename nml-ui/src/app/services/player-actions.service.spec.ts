import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PlayerActionsService } from './player-actions.service';
import { AuthService } from './auth.service';
import { PlayerService } from './player.service';
import type { PlayerAction } from '../models';

describe('PlayerActionsService', () => {
  const user = signal({ id: 1, username: 'lurio' });
  const playerService = {
    loadCurrent: jest.fn().mockResolvedValue(undefined),
    loadVehicles: jest.fn().mockResolvedValue(undefined),
    loadReserveUnits: jest.fn().mockResolvedValue(undefined),
  };
  let service: PlayerActionsService;
  let http: HttpTestingController;

  beforeEach(() => {
    jest.clearAllMocks();
    user.set({ id: 1, username: 'lurio' });
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: { user: user.asReadonly(), initialized: signal(true) },
        },
        { provide: PlayerService, useValue: playerService },
      ],
    });
    service = TestBed.inject(PlayerActionsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('charge les actions et expose une erreur lisible', async () => {
    const loaded = service.loadActions();
    http.expectOne('/api/players/actions').flush([{ id: 1 }] as PlayerAction[]);
    await loaded;
    expect(service.actions()).toHaveLength(1);
    expect(service.hasActions()).toBe(true);

    const failed = service.loadActions();
    http
      .expectOne('/api/players/actions')
      .flush({ detail: 'Session expirée' }, { status: 400, statusText: 'Erreur' });
    await failed;
    expect(service.error()).toBe('Session expirée');
  });

  it('annule une action, recharge le joueur et ses listes', async () => {
    const undo = service.undoFrom(5);
    const post = http.expectOne('/api/players/actions/5/undo');
    expect(post.request.method).toBe('POST');
    post.flush([{ id: 2 }] as PlayerAction[]);

    await expect(undo).resolves.toBe(true);
    expect(service.actions()).toEqual([{ id: 2 }]);
    expect(playerService.loadCurrent).toHaveBeenCalled();
    expect(playerService.loadVehicles).toHaveBeenCalled();
    expect(playerService.loadReserveUnits).toHaveBeenCalled();
  });

  it('annule tout et récolte avec le choix demandé', async () => {
    const undoAll = service.undoAll();
    http.expectOne('/api/players/actions/undo-all').flush([]);
    await expect(undoAll).resolves.toBe(true);

    const harvest = service.harvest('HARVEST_MONEY', [1, 2]);
    const post = http.expectOne('/api/players/actions/harvest');
    expect(post.request.body).toEqual({ choice: 'HARVEST_MONEY', sectorNumbers: [1, 2] });
    post.flush([]);
    await expect(harvest).resolves.toBe(true);
  });

  it('renvoie false et pose l’erreur sur refus', async () => {
    const undo = service.undoFrom(5);
    http
      .expectOne('/api/players/actions/5/undo')
      .flush({ detail: 'Tour terminé' }, { status: 400, statusText: 'Erreur' });

    await expect(undo).resolves.toBe(false);
    expect(service.error()).toBe('Tour terminé');
  });
});
