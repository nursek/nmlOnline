import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UnitMovementDialogComponent } from './unit-movement-dialog.component';
import { ActiveBoardService } from '../../services/active-board.service';
import { MovementStateService } from '../../services/movement-state.service';
import { PlayerDirectoryService } from '../../services/player-directory.service';
import { SniperOrderStateService } from '../../services/sniper-order-state.service';
import type { Sector, Unit } from '../../models';

describe('UnitMovementDialogComponent', () => {
  const unit = {
    id: 3,
    number: 1,
    type: { name: 'TIREUR' },
    classes: [{ name: 'LEGER', maxMovementHops: 1 }],
  } as unknown as Unit;
  const sector = {
    number: 5,
    name: 'Cinq',
    ownerId: 1,
    neighbors: [6],
    army: [],
    buildings: [],
    vehicles: [],
    characters: [],
  } as unknown as Sector;
  const sectors = [
    sector,
    {
      number: 6,
      name: 'Six',
      ownerId: null,
      neighbors: [5, 7],
      army: [],
      buildings: [],
      vehicles: [],
      characters: [],
    },
    {
      number: 7,
      name: 'Sept',
      ownerId: 9,
      neighbors: [6],
      army: [],
      buildings: [],
      vehicles: [],
      characters: [],
    },
  ] as unknown as Sector[];
  const movement = {
    orders: signal([]),
    error: signal<string | null>(null),
    loadOrders: jest.fn().mockResolvedValue(undefined),
    placeFootOrder: jest.fn().mockResolvedValue({ id: 9 }),
    cancelOrder: jest.fn().mockResolvedValue(true),
  };
  const sniper = {
    orders: signal([]),
    error: signal<string | null>(null),
    loadOrders: jest.fn().mockResolvedValue(undefined),
  };
  const snackBar = { open: jest.fn() };
  const dialogRef = { close: jest.fn() };

  beforeEach(() => {
    jest.clearAllMocks();
    movement.orders.set([]);
    movement.error.set(null);
    TestBed.configureTestingModule({
      providers: [
        { provide: MovementStateService, useValue: movement },
        { provide: SniperOrderStateService, useValue: sniper },
        { provide: ActiveBoardService, useValue: { sectors: signal(sectors) } },
        { provide: PlayerDirectoryService, useValue: { players: signal([]), colorMap: signal(new Map()) } },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialogRef, useValue: dialogRef },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { unit, sector, playerId: 1, allyPlayerIds: new Set<number>() },
        },
      ],
    });
  });

  function component(): UnitMovementDialogComponent {
    return TestBed.runInInjectionContext(() => new UnitMovementDialogComponent());
  }

  it('bloque un ordre si l’entité a déjà un ordre en attente', async () => {
    movement.orders.set([{ entityIds: [3] }] as never);
    const page = component();

    await page.submitMove();

    expect(page.canSubmit()).toBe(false);
    expect(movement.placeFootOrder).not.toHaveBeenCalled();
  });

  it('envoie l’ordre sur le chemin cliqué', async () => {
    const page = component();

    page.onMapSectorClick(6);
    expect(page.route()).toEqual([5, 6]);
    await page.submitMove();

    expect(movement.placeFootOrder).toHaveBeenCalledWith([3], [5, 6]);
    expect(snackBar.open).toHaveBeenCalledWith('Ordre : secteur 5 → 6', 'OK', { duration: 2500 });
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('annule un ordre en attente et vide le chemin', async () => {
    const page = component();
    page.route.set([5, 6]);

    await page.cancelOrder(9);

    expect(movement.cancelOrder).toHaveBeenCalledWith(9);
    expect(page.route()).toEqual([]);
    expect(snackBar.open).toHaveBeenCalledWith('Ordre annulé', 'OK', { duration: 2500 });
  });

  it('refuse un secteur non adjacent sans créer d’ordre', async () => {
    const page = component();

    page.onMapSectorClick(7);

    expect(page.route()).toEqual([]);
    expect(snackBar.open).toHaveBeenCalledWith(
      'Ce secteur n’est pas adjacent à la dernière étape.',
      'Fermer',
      { duration: 2500 },
    );
    await page.submitMove();
    expect(movement.placeFootOrder).not.toHaveBeenCalled();
  });
});
