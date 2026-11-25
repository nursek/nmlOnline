import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { AdminService } from './admin.service';
import { ApiService } from './api.service';
import type { AdminCatalog, AdminPlayerEdit, Player } from '../models';

describe('AdminService', () => {
  let service: AdminService;
  let http: HttpTestingController;
  const snackBar = { open: jest.fn() };

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MatSnackBar, useValue: snackBar },
      ],
    });
    service = TestBed.inject(AdminService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('charge le tour courant et expose l’erreur réseau', async () => {
    const ok = service.loadCurrentTurn();
    http.expectOne('/api/admin/turn/current').flush({ currentTurn: 4 });
    await ok;
    expect(service.currentTurn()).toBe(4);

    const failed = service.loadCurrentTurn();
    http
      .expectOne('/api/admin/turn/current')
      .flush({ detail: 'Tour indisponible' }, { status: 500, statusText: 'Erreur' });
    await failed;
    TestBed.tick();
    expect(snackBar.open).toHaveBeenCalledWith('Tour indisponible', 'Fermer', {
      duration: 6000,
      panelClass: 'error-snackbar',
    });
  });

  it('charge les joueurs et détecte une liste tronquée', async () => {
    service.reloadPlayers();
    TestBed.tick();
    http.expectOne('/api/admin/players?page=0&size=50').flush({
      content: [{ id: 1, name: 'lurio' } as unknown as Player],
      totalElements: 2,
    });
    await Promise.resolve();
    TestBed.tick();

    expect(service.players()).toHaveLength(1);
    expect(service.playersTruncated()).toBe(true);
  });

  it('recharge les ordres avec le filtre de statut', async () => {
    service.reloadOrders();
    TestBed.tick();
    http.expectOne('/api/admin/turn/orders').flush([]);
    await Promise.resolve();

    service.orderStatusFilter.set('PENDING');
    TestBed.tick();
    http.expectOne('/api/admin/turn/orders?status=PENDING').flush([]);
    await Promise.resolve();
    expect(service.orders()).toEqual([]);
  });

  it('ouvre puis ferme l’édition en rechargeant la liste', async () => {
    service.reloadPlayers();
    TestBed.tick();
    http.expectOne('/api/admin/players?page=0&size=50').flush({ content: [], totalElements: 0 });
    await Promise.resolve();

    service.openEdit(5);
    TestBed.tick();
    http.expectOne('/api/admin/players/5').flush({ player: { id: 5 } } as AdminPlayerEdit);
    http.expectOne('/api/admin/catalog').flush({ equipments: [] } as unknown as AdminCatalog);
    await Promise.resolve();
    expect(service.edit()).toEqual({ player: { id: 5 } });

    service.closeEdit();
    expect(service.edit()).toBeNull();
    TestBed.tick();
    http.expectOne('/api/admin/players?page=0&size=50').flush({ content: [], totalElements: 0 });
  });

  it('mutate notifie le succès, recharge l’édition et les joueurs', async () => {
    service.reloadPlayers();
    TestBed.tick();
    http.expectOne('/api/admin/players?page=0&size=50').flush({ content: [], totalElements: 0 });
    await Promise.resolve();
    service.openEdit(5);
    TestBed.tick();
    http.expectOne('/api/admin/players/5').flush({ player: { id: 5 } } as AdminPlayerEdit);
    http.expectOne('/api/admin/catalog').flush({ equipments: [] } as unknown as AdminCatalog);
    await Promise.resolve();

    const mutation = service.mutate(of(undefined), 'Argent mis à jour');
    await mutation;
    TestBed.tick();
    http.expectOne('/api/admin/players/5').flush({ player: { id: 5 } } as AdminPlayerEdit);
    http.expectOne('/api/admin/players?page=0&size=50').flush({ content: [], totalElements: 0 });

    expect(snackBar.open).toHaveBeenCalledWith('Argent mis à jour', 'OK', {
      duration: 4000,
      panelClass: 'success-snackbar',
    });
  });

  it('mutate remonte l’erreur et la notifie', async () => {
    const mutation = service.mutate(
      throwError(() => ({ error: { detail: 'Refusé' } })),
      'OK',
    );

    await expect(mutation).rejects.toThrow('Refusé');
    TestBed.tick();
    expect(snackBar.open).toHaveBeenCalledWith('Refusé', 'Fermer', {
      duration: 6000,
      panelClass: 'error-snackbar',
    });
  });

  it('importe un joueur et expose importing', async () => {
    const file = new File(['{}'], 'lurio.json', { type: 'application/json' });
    const pending = service.importPlayer(file, 'secret');

    expect(service.importing()).toBe(true);
    const post = http.expectOne('/api/admin/players/import');
    post.flush({ id: 1, name: 'lurio' } as Player);
    await expect(pending).resolves.toEqual({ id: 1, name: 'lurio' });
    TestBed.tick();
    http.expectOne('/api/admin/players?page=0&size=50').flush({ content: [], totalElements: 0 });

    expect(service.importing()).toBe(false);
    TestBed.tick();
    expect(snackBar.open).toHaveBeenCalledWith(
      'Joueur "lurio" importé avec succès',
      'OK',
      { duration: 4000, panelClass: 'success-snackbar' },
    );
  });
});
