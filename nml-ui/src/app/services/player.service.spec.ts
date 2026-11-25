import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PlayerService } from './player.service';
import { AuthService } from './auth.service';
import type { Player, Unit, Vehicle } from '../models';

describe('PlayerService', () => {
  const user = signal({ id: 1, username: 'lurio', role: 'USER' });
  let service: PlayerService;
  let http: HttpTestingController;

  beforeEach(() => {
    user.set({ id: 1, username: 'lurio', role: 'USER' });
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
    service = TestBed.inject(PlayerService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('charge le joueur courant et expose l’erreur de récupération', async () => {
    const loaded = service.loadCurrent();
    http.expectOne('/api/players/me').flush({ id: 1, name: 'lurio' } as Player);
    await loaded;
    expect(service.player()?.name).toBe('lurio');

    const failed = service.loadCurrent();
    http.expectOne('/api/players/me').flush(null, { status: 404, statusText: 'Absent' });
    await failed;
    expect(service.error()).toBe("Aucun profil de joueur n'est lié à ce compte.");
  });

  it('ne charge rien sans utilisateur connecté', async () => {
    user.set(null);
    TestBed.tick();

    await service.loadCurrent();

    http.expectNone('/api/players/me');
  });

  it('charge les véhicules et ne garde que les non déployés', async () => {
    const loaded = service.loadVehicles();
    http.expectOne('/api/vehicles/my').flush([
      { id: 1, sectorNumber: null, isDestroyed: false },
      { id: 2, sectorNumber: 5, isDestroyed: false },
      { id: 3, sectorNumber: null, isDestroyed: true },
    ] as Vehicle[]);
    await loaded;

    expect(service.vehicles()).toHaveLength(3);
    expect(service.undeployedVehicles().map((vehicle) => vehicle.id)).toEqual([1]);
  });

  it('place une unité, la retire de la réserve et recharge le profil', async () => {
    const reserve = service.loadReserveUnits();
    http.expectOne('/api/units/reserve').flush([{ id: 10 }, { id: 11 }] as Unit[]);
    await reserve;

    const placed = service.placeUnit(10, 3, 7);
    const post = http.expectOne('/api/units/10/place');
    expect(post.request.body).toEqual({ boardId: 3, sectorNumber: 7 });
    post.flush({ id: 10 } as Unit);
    await expect(placed).resolves.toEqual({ id: 10 });
    http.expectOne('/api/players/me').flush({ id: 1, name: 'lurio' } as Player);

    expect(service.reserveUnits().map((unit) => unit.id)).toEqual([11]);
  });

  it('charge les recrues et le tour courant, avec leurs erreurs', async () => {
    const reserve = service.loadReserveUnits();
    http.expectOne('/api/units/reserve').flush([{ id: 10 }] as Unit[]);
    await reserve;
    expect(service.reserveUnits()).toHaveLength(1);

    const turn = service.loadCurrentTurn();
    http.expectOne('/api/turn/current').flush(4);
    await turn;
    expect(service.currentTurn()).toBe(4);

    const failed = service.loadReserveUnits();
    http.expectOne('/api/units/reserve').flush({ detail: 'Indisponible' }, { status: 500, statusText: 'Erreur' });
    await failed;
    expect(service.error()).toBe('Indisponible');
  });

  it('met à jour l’équipage et recharge le joueur', async () => {
    const loaded = service.loadVehicles();
    http.expectOne('/api/vehicles/my').flush([{ id: 5, sectorNumber: 1 }] as Vehicle[]);
    await loaded;

    const updated = service.setVehicleCrew(5, 3, [4, 6]);
    const post = http.expectOne('/api/vehicles/5/crew');
    expect(post.request.body).toEqual({ pilotId: 3, passengerIds: [4, 6] });
    post.flush({ id: 5, sectorNumber: 2 } as Vehicle);
    await updated;
    http.expectOne('/api/players/me').flush({ id: 1, name: 'lurio' } as Player);

    expect(service.vehicles().find((vehicle) => vehicle.id === 5)?.sectorNumber).toBe(2);
  });

  it('déplace un bâtiment et recharge le joueur', async () => {
    const moved = service.moveBuilding(7, 3, 9);
    const post = http.expectOne('/api/buildings/7/move');
    expect(post.request.body).toEqual({ boardId: 3, newSectorNumber: 9 });
    post.flush(null);
    await moved;
    http.expectOne('/api/players/me').flush({ id: 1, name: 'lurio' } as Player);

    await expect(moved).resolves.toBe(true);

    const refused = service.moveBuilding(7, 3, 9);
    http
      .expectOne('/api/buildings/7/move')
      .flush({ detail: 'Cooldown' }, { status: 409, statusText: 'Conflit' });
    await expect(refused).resolves.toBe(false);
    expect(service.error()).toBe('Cooldown');
  });

  it('équipe, retire et auto-équipe les unités', async () => {
    const assigned = service.assignUnitEquipment(3, 'Fusil');
    http.expectOne('/api/units/3/equipment').flush({ id: 3 } as Unit);
    await assigned;
    http.expectOne('/api/players/me').flush({ id: 1, name: 'lurio' } as Player);

    const removed = service.removeUnitEquipment(3, 'Fusil');
    const del = http.expectOne('/api/units/3/equipment');
    expect(del.request.method).toBe('DELETE');
    del.flush({ id: 3 } as Unit);
    await removed;
    http.expectOne('/api/players/me').flush({ id: 1, name: 'lurio' } as Player);

    const auto = service.autoEquipUnits(['FIREARM'], ['Fusil']);
    const post = http.expectOne('/api/units/auto-equip');
    expect(post.request.body).toEqual({ categories: ['FIREARM'], equipmentNames: ['Fusil'] });
    post.flush({ equippedCount: 2, unitCount: 1 });
    await auto;
    http.expectOne('/api/players/me').flush({ id: 1, name: 'lurio' } as Player);

    await expect(assigned).resolves.toEqual({ id: 3 });
    await expect(removed).resolves.toEqual({ id: 3 });
    await expect(auto).resolves.toEqual({ equippedCount: 2, unitCount: 1 });
  });

  it('réinitialise tout l’état', () => {
    service.reset();
    expect(service.player()).toBeNull();
    expect(service.vehicles()).toEqual([]);
    expect(service.reserveUnits()).toEqual([]);
    expect(service.currentTurn()).toBeNull();
  });

  it('renvoie null et pose l’erreur quand un déploiement échoue', async () => {
    const placed = service.placeVehicle(5, 3, 7);
    http
      .expectOne('/api/vehicles/5/place')
      .flush({ detail: 'Secteur invalide' }, { status: 400, statusText: 'Bad Request' });

    await expect(placed).resolves.toBeNull();
    expect(service.error()).toBe('Secteur invalide');
  });

  it('jette l’équipement d’une cache et recharge le profil', async () => {
    const discarded = service.discardEquipment(4, 'Fusil', 2);
    const post = http.expectOne('/api/buildings/4/weapon-cache/discard');
    expect(post.request.body).toEqual({ equipmentName: 'Fusil', quantity: 2 });
    post.flush(null);
    await expect(discarded).resolves.toBe(true);
    http.expectOne('/api/players/me').flush({ id: 1, name: 'lurio' } as Player);
  });
});
