import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { OrdresComponent } from './ordres.component';
import { MovementStateService } from '../../services/movement-state.service';
import { SniperOrderStateService } from '../../services/sniper-order-state.service';
import { PlayerService } from '../../services/player.service';
import type { Unit } from '../../models';

describe('OrdresComponent', () => {
  const movement = {
    orders: signal([]),
    loading: signal(false),
    error: signal<string | null>(null),
    loadOrders: jest.fn().mockResolvedValue(undefined),
    cancelOrder: jest.fn().mockResolvedValue(true),
  };
  const sniper = {
    orders: signal([]),
    loading: signal(false),
    error: signal<string | null>(null),
    loadOrders: jest.fn().mockResolvedValue(undefined),
    cancelOrder: jest.fn().mockResolvedValue(true),
  };
  const playerService = {
    player: signal({
      race: 'ORKS',
      sectors: [
        {
          number: 5,
          army: [
            {
              id: 1,
              number: 2,
              type: { name: 'LARBIN' },
              attack: 10,
              defense: 10,
              pdf: 0,
              pdc: 0,
              armor: 0,
              evasion: 0,
              experience: 0,
              classes: [],
              equipments: [],
              isInjured: false,
            } as Unit,
          ],
        },
      ],
    }),
    loadCurrent: jest.fn().mockResolvedValue(undefined),
  };
  const snack = { open: jest.fn() };

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: MovementStateService, useValue: movement },
        { provide: SniperOrderStateService, useValue: sniper },
        { provide: PlayerService, useValue: playerService },
        { provide: MatSnackBar, useValue: snack },
      ],
    });
  });

  function component(): OrdresComponent {
    return TestBed.runInInjectionContext(() => new OrdresComponent());
  }

  it('charge les ordres de déplacement et sniper à la création', () => {
    component();

    expect(movement.loadOrders).toHaveBeenCalled();
    expect(sniper.loadOrders).toHaveBeenCalled();
    expect(playerService.loadCurrent).toHaveBeenCalled();
  });

  it('annule un ordre et notifie', async () => {
    const page = component();

    await page.cancelOrder(12);

    expect(movement.cancelOrder).toHaveBeenCalledWith(12);
    expect(snack.open).toHaveBeenCalledWith('Ordre annulé', 'OK', { duration: 2500 });
  });

  it('annule un ordre sniper et notifie', async () => {
    const page = component();

    await page.cancelSniperOrder(3);

    expect(sniper.cancelOrder).toHaveBeenCalledWith(3);
    expect(snack.open).toHaveBeenCalledWith('Ordre sniper annulé', 'OK', { duration: 2500 });
  });

  it('ne notifie pas une annulation refusée', async () => {
    movement.cancelOrder.mockResolvedValueOnce(false);
    const page = component();

    await page.cancelOrder(12);

    expect(snack.open).not.toHaveBeenCalled();
  });

  it('résout le libellé d’une unité et replie sur son id', () => {
    const page = component();

    expect(page.unitLabel(1)).toBe('LARBIN n°2');
    expect(page.unitLabel(99)).toBe('Unité #99');
  });

  it('rafraîchit les deux listes', async () => {
    const page = component();
    movement.loadOrders.mockClear();
    sniper.loadOrders.mockClear();

    await page.refresh();

    expect(movement.loadOrders).toHaveBeenCalled();
    expect(sniper.loadOrders).toHaveBeenCalled();
  });
});
