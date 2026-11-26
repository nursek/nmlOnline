import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UnitDetailDialogComponent } from './unit-detail-dialog.component';
import { UnitSlotPickerComponent } from './unit-slot-picker.component';
import { PlayerService } from '../../services/player.service';
import { ActiveBoardService } from '../../services/active-board.service';
import { MovementStateService } from '../../services/movement-state.service';
import { SniperOrderStateService } from '../../services/sniper-order-state.service';
import { AllianceStateService } from '../../services/alliance-state.service';
import type { Equipment, Sector, Unit } from '../../models';

describe('UnitDetailDialogComponent', () => {
  const equipment = {
    name: 'Fusil',
    category: 'FIREARM',
    displayNames: { ORKS: 'Shoota' },
  } as unknown as Equipment;
  const unit = {
    id: 3,
    number: 1,
    type: {
      name: 'TIREUR',
      maxMeleeWeapons: 1,
      maxFirearms: 1,
      maxDefensiveEquipment: 1,
    },
    classes: [{ name: 'LEGER', maxMovementHops: 1 }],
    equipments: [equipment],
    attack: 10,
    defense: 10,
    pdf: 30,
    pdc: 20,
    armor: 0,
    evasion: 0,
    experience: 2,
    isInjured: false,
    sectorNumber: 5,
  } as unknown as Unit;
  const sectors = [
    { number: 5, name: 'Cinq', ownerId: 1, neighbors: [6], army: [], buildings: [], vehicles: [], characters: [] },
    { number: 6, name: 'Six', ownerId: null, neighbors: [5, 7], army: [], buildings: [], vehicles: [], characters: [] },
    { number: 7, name: 'Sept', ownerId: 9, neighbors: [6], army: [], buildings: [], vehicles: [], characters: [] },
  ] as unknown as Sector[];
  const player = signal({
    id: 1,
    name: 'lurio',
    race: 'ORKS',
    sectors: [{ number: 5, army: [{ ...unit, experience: 9 }] }],
  });
  const playerService = {
    player,
    removeUnitEquipment: jest.fn().mockResolvedValue(unit),
    error: signal<string | null>(null),
  };
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
    placeOrder: jest.fn().mockResolvedValue({ id: 4, action: 'TIR_PRECISION' }),
    cancelOrder: jest.fn().mockResolvedValue(true),
  };
  const alliance = { alliedPlayerIds: signal(new Set<number>()) };
  const snackBar = { open: jest.fn() };
  const dialog = { open: jest.fn() };
  const dialogRef = { close: jest.fn() };

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: PlayerService, useValue: playerService },
        { provide: ActiveBoardService, useValue: { sectors: signal(sectors) } },
        { provide: MovementStateService, useValue: movement },
        { provide: SniperOrderStateService, useValue: sniper },
        { provide: AllianceStateService, useValue: alliance },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialog, useValue: dialog },
        { provide: MatDialogRef, useValue: dialogRef },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { unit, sectorNumber: 5, sectorName: 'Cinq' },
        },
      ],
    });
  });

  function component(): UnitDetailDialogComponent {
    return TestBed.runInInjectionContext(() => new UnitDetailDialogComponent());
  }

  it('relit l’unité live et expose les slots d’équipement', () => {
    const page = component();

    expect(page.unit().experience).toBe(9);
    expect(page.portraitUrl()).toContain('lurio/units/tireur/portrait.png');
    expect(page.slotGroups().map((group) => group.category)).toEqual([
      'MELEE',
      'FIREARM',
      'DEFENSIVE',
    ]);
    expect(page.offensiveSlots()).toHaveLength(2);
    expect(page.defensiveSlots()).toHaveLength(1);
    expect(page.emptySlots(page.slotGroups()[1])).toEqual([]);
    expect(page.equipmentLabel(equipment)).toBe('Shoota');
  });

  it('retire un équipement et notifie', async () => {
    const page = component();

    await page.unequip(equipment);

    expect(playerService.removeUnitEquipment).toHaveBeenCalledWith(3, 'Fusil');
    expect(snackBar.open).toHaveBeenCalledWith('Shoota retiré', 'OK', { duration: 2500 });
  });

  it('bloque un ordre si un ordre est déjà en attente', async () => {
    movement.orders.set([{ entityIds: [3] }] as never);
    const page = component();

    await page.submitMove();

    expect(movement.placeFootOrder).not.toHaveBeenCalled();
    expect(snackBar.open).toHaveBeenCalledWith('Cette unité a déjà un ordre en attente', 'Fermer', {
      duration: 3000,
    });
  });

  it('place un ordre de déplacement sur une route adjacente', async () => {
    movement.orders.set([]);
    const page = component();
    page.targetSector.set(6);

    await page.submitMove();

    expect(movement.placeFootOrder).toHaveBeenCalledWith([3], [5, 6]);
    expect(snackBar.open).toHaveBeenCalledWith('Ordre: secteur 5 -> 6', 'OK', { duration: 2500 });
    expect(page.targetSector()).toBeNull();
  });

  it('place un ordre sniper et dérive l’action du propriétaire', async () => {
    alliance.alliedPlayerIds.set(new Set([9]));
    const page = component();
    expect(page.sniperActionFor(7)).toBe('SURVEILLANCE');
    expect(page.sniperActionFor(6)).toBe('TIR_PRECISION');

    page.sniperTarget.set(7);
    await page.submitSniperOrder();

    expect(sniper.placeOrder).toHaveBeenCalledWith(3, 7);
    expect(snackBar.open).toHaveBeenCalledWith('Tir de précision : secteur 5 -> 7', 'OK', {
      duration: 2500,
    });
    expect(page.sniperTarget()).toBeNull();
  });

  it('ouvre le sélecteur de slot, annule les ordres, ferme et signale l’image cassée', async () => {
    const page = component();
    page.openSlotPicker(page.slotGroups()[1]);
    expect(dialog.open).toHaveBeenCalledWith(
      UnitSlotPickerComponent,
      expect.objectContaining({ data: { unitId: 3, category: 'FIREARM', categoryLabel: 'Distance' } }),
    );

    await page.cancelOrder(9);
    await page.cancelSniperOrder(4);
    expect(movement.cancelOrder).toHaveBeenCalledWith(9);
    expect(sniper.cancelOrder).toHaveBeenCalledWith(4);

    page.onImgError();
    expect(page.imgError()).toBe(true);
    page.close();
    expect(dialogRef.close).toHaveBeenCalled();
  });
});
