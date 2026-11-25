import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ExchangeService } from './exchange.service';
import { AuthService } from './auth.service';
import { PlayerService } from './player.service';
import type { ExchangeOffer } from '../models';

describe('ExchangeService', () => {
  const user = signal({ id: 1, username: 'lurio' });
  const player = signal({ id: 1, name: 'lurio' });
  const playerService = { player, loadCurrent: jest.fn().mockResolvedValue(undefined) };
  const offer = (id: number, sender: number, receiver: number, status = 'PENDING') =>
    ({ id, senderPlayerId: sender, receiverPlayerId: receiver, status }) as ExchangeOffer;
  let service: ExchangeService;
  let http: HttpTestingController;

  beforeEach(() => {
    jest.clearAllMocks();
    user.set({ id: 1, username: 'lurio' });
    player.set({ id: 1, name: 'lurio' });
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
    service = TestBed.inject(ExchangeService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('charge les offres et sépare reçues/envoyées', async () => {
    const loaded = service.loadOffers();
    http.expectOne('/api/bank/offers').flush([
      offer(1, 1, 2),
      offer(2, 2, 1),
      offer(3, 1, 2, 'ACCEPTED'),
    ]);
    await loaded;

    expect(service.received().map((item) => item.id)).toEqual([2]);
    expect(service.sent().map((item) => item.id)).toEqual([1, 3]);
    expect(service.pendingSentCount()).toBe(1);
  });

  it('crée une offre en tête de liste', async () => {
    const created = service.create({ receiverPlayerId: 2, money: 100, resources: [] });
    const post = http.expectOne('/api/bank/offers');
    expect(post.request.method).toBe('POST');
    post.flush(offer(4, 1, 2));

    await expect(created).resolves.toBe(true);
    expect(service.offers()[0].id).toBe(4);
    expect(service.saving()).toBe(false);
  });

  it('accepte, refuse et annule en remplaçant l’offre', async () => {
    service.loadOffers();
    http.expectOne('/api/bank/offers').flush([offer(1, 1, 2)]);
    await Promise.resolve();

    for (const [action, path, status] of [
      ['accept', 'accept', 'ACCEPTED'],
      ['decline', 'decline', 'DECLINED'],
      ['cancel', 'cancel', 'CANCELLED'],
    ] as const) {
      const mutation = service[action](1);
      http.expectOne(`/api/bank/offers/1/${path}`).flush(offer(1, 1, 2, status));
      await expect(mutation).resolves.toBe(true);
      expect(service.offers()[0].status).toBe(status);
    }
    expect(playerService.loadCurrent).toHaveBeenCalledTimes(3);
  });

  it('expose les erreurs de chargement et de mutation', async () => {
    const load = service.loadOffers();
    http
      .expectOne('/api/bank/offers')
      .flush({ detail: 'Banque fermée' }, { status: 409, statusText: 'Conflit' });
    await load;
    expect(service.error()).toBe('Banque fermée');

    const create = service.create({ receiverPlayerId: 2, money: 0, resources: [] });
    http
      .expectOne('/api/bank/offers')
      .flush({ detail: 'Offre invalide' }, { status: 400, statusText: 'Erreur' });
    await expect(create).resolves.toBe(false);
    expect(service.error()).toBe('Offre invalide');
  });

  it('se vide à la déconnexion', () => {
    service.reset();
    expect(service.offers()).toEqual([]);
    expect(service.error()).toBeNull();
  });
});
