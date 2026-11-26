import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MovementStateService } from './movement-state.service';
import { AuthService } from './auth.service';
import type { MovementOrder } from '../models';

describe('MovementStateService', () => {
  const user = signal({ id: 1, username: 'lurio' });
  let service: MovementStateService;
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
    service = TestBed.inject(MovementStateService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('charge les ordres, expose ids engagés et filtre par unité', async () => {
    const loaded = service.loadOrders();
    http.expectOne('/api/units/movement').flush([
      { id: 1, entityIds: [10, 11] },
      { id: 2, vehicleId: 20 },
    ] as MovementOrder[]);
    await loaded;

    expect(service.orders()).toHaveLength(2);
    expect([...service.pendingEntityIds()].sort()).toEqual([10, 11]);
    expect([...service.pendingVehicleIds()]).toEqual([20]);
    expect(service.pendingForUnit(10)).toHaveLength(1);
    expect(service.pendingForUnit(99)).toEqual([]);
  });

  it('tolère une réponse nulle et expose l’erreur', async () => {
    const empty = service.loadOrders();
    http.expectOne('/api/units/movement').flush(null);
    await empty;
    expect(service.orders()).toEqual([]);

    const failed = service.loadOrders();
    http
      .expectOne('/api/units/movement')
      .flush({ detail: 'Indisponible' }, { status: 500, statusText: 'Erreur' });
    await failed;
    expect(service.error()).toBe('Indisponible');
  });

  it('place un ordre à pied puis recharge la liste', async () => {
    const placed = service.placeFootOrder([1, 2], [5, 6]);
    const post = http.expectOne('/api/units/movement/foot');
    expect(post.request.body).toEqual({ entityIds: [1, 2], route: [5, 6] });
    post.flush({ id: 9 });
    await Promise.resolve();
    http.expectOne('/api/units/movement').flush([{ id: 9 }] as MovementOrder[]);

    await expect(placed).resolves.toEqual({ id: 9 });
  });

  it('place un ordre véhicule, annule et recharge', async () => {
    const placed = service.placeVehicleOrder(4, [5, 6]);
    http.expectOne('/api/vehicles/4/movement').flush({ id: 10 });
    await Promise.resolve();
    http.expectOne('/api/units/movement').flush([{ id: 10 }] as MovementOrder[]);
    await expect(placed).resolves.toEqual({ id: 10 });

    const cancelled = service.cancelOrder(10);
    const del = http.expectOne('/api/units/movement/10');
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
    await Promise.resolve();
    http.expectOne('/api/units/movement').flush([]);
    await expect(cancelled).resolves.toBe(true);
  });

  it('renvoie null/false et pose l’erreur sur refus', async () => {
    const placed = service.placeFootOrder([1], [5, 6]);
    http
      .expectOne('/api/units/movement/foot')
      .flush({ detail: 'Route invalide' }, { status: 400, statusText: 'Erreur' });
    await expect(placed).resolves.toBeNull();
    expect(service.error()).toBe('Route invalide');

    const cancelled = service.cancelOrder(10);
    http
      .expectOne('/api/units/movement/10')
      .flush({ detail: 'Ordre résolu' }, { status: 409, statusText: 'Conflit' });
    await expect(cancelled).resolves.toBe(false);
    expect(service.error()).toBe('Ordre résolu');
  });
});
