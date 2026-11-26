import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SniperOrderStateService } from './sniper-order-state.service';
import { AuthService } from './auth.service';
import type { SniperOrder } from '../models';

describe('SniperOrderStateService', () => {
  const user = signal({ id: 1, username: 'lurio' });
  let service: SniperOrderStateService;
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
    service = TestBed.inject(SniperOrderStateService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('charge les ordres sniper et tolère une réponse nulle', async () => {
    const loaded = service.loadOrders();
    http.expectOne('/api/units/sniper-orders').flush([{ id: 1 }] as SniperOrder[]);
    await loaded;
    expect(service.orders()).toHaveLength(1);

    const empty = service.loadOrders();
    http.expectOne('/api/units/sniper-orders').flush(null);
    await empty;
    expect(service.orders()).toEqual([]);
  });

  it('place un ordre puis recharge la liste', async () => {
    const placed = service.placeOrder(5, 12);
    const post = http.expectOne('/api/units/5/sniper-orders');
    expect(post.request.body).toEqual({ targetSectorNumber: 12 });
    post.flush({ id: 3, targetSectorNumber: 12 } as SniperOrder);
    await Promise.resolve();
    http.expectOne('/api/units/sniper-orders').flush([{ id: 3 }] as SniperOrder[]);

    await expect(placed).resolves.toEqual({ id: 3, targetSectorNumber: 12 });
    expect(service.orders()).toEqual([{ id: 3 }]);
  });

  it('annule un ordre puis recharge la liste', async () => {
    const cancelled = service.cancelOrder(3);
    const del = http.expectOne('/api/units/sniper-orders/3');
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
    await Promise.resolve();
    http.expectOne('/api/units/sniper-orders').flush([]);

    await expect(cancelled).resolves.toBe(true);
    expect(service.orders()).toEqual([]);
  });

  it('pose l’erreur sur un refus de placement', async () => {
    const placed = service.placeOrder(5, 12);
    http
      .expectOne('/api/units/5/sniper-orders')
      .flush({ detail: 'Unité invalide' }, { status: 400, statusText: 'Erreur' });

    await expect(placed).resolves.toBeNull();
    expect(service.error()).toBe('Unité invalide');
  });

  it('se vide à la déconnexion', () => {
    service.clear();
    expect(service.orders()).toEqual([]);
    expect(service.error()).toBeNull();
  });
});
