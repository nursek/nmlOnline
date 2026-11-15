import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PreparationPanelComponent } from './preparation-panel.component';
import { ApiService } from '../../services/api.service';
import { ShopService } from '../../services/shop.service';
import type { Building, Player, Sector, Unit, UnitClass } from '../../models';

function unitClass(name: string): UnitClass {
  return {
    name,
    code: name.slice(0, 1),
    criticalChance: null,
    criticalMultiplier: null,
    damageReductionPdf: null,
    damageReductionPdc: null,
    maxMovementHops: 1,
  };
}

function unit(id: number, classNames: string[] = [], playerId = 1): Unit {
  return {
    id,
    playerId,
    number: id,
    sectorNumber: null,
    experience: 0,
    type: {
      name: 'LARBIN',
      level: 1,
      baseAttack: 1,
      baseDefense: 1,
      maxFirearms: 1,
      maxMeleeWeapons: 1,
      maxDefensiveEquipment: 1,
    },
    classes: classNames.map(unitClass),
    isInjured: false,
    equipments: [],
    attack: 1,
    defense: 1,
    pdf: 0,
    pdc: 0,
    armor: 0,
    evasion: 0,
  };
}

function building(id: number, sectorNumber: number | null): Building {
  return {
    id,
    playerId: 1,
    buildingType: 'HEADQUARTERS',
    displayName: 'Quartier Général',
    attack: 0,
    defense: 0,
    isDestroyed: false,
    isCaptured: false,
    capturedByPlayerId: null,
    capturedTurn: null,
    lastMovedTurn: null,
    canMove: false,
    moveCooldown: 0,
    sectorNumber,
  };
}

function player(overrides: Partial<Player> = {}): Player {
  return {
    id: 1,
    name: 'Joueur',
    stats: {
      money: 0,
      totalIncome: 0,
      totalVehiclesValue: 0,
      totalEquipmentValue: 0,
      totalOffensivePower: 0,
      totalDefensivePower: 0,
      globalPower: 0,
      totalEconomyPower: 0,
      totalAtk: 0,
      totalPdf: 0,
      totalPdc: 0,
      totalDef: 0,
      totalArmor: 0,
    },
    equipments: [],
    resources: [],
    sectors: [],
    character: null,
    buildings: [],
    ...overrides,
  };
}

function sector(number = 1, army: Unit[] = []): Sector {
  return {
    number,
    name: `Secteur ${number}`,
    income: 0,
    army,
    buildings: [],
    character: null,
    vehicles: [],
    ownerId: 1,
    boardId: 1,
    color: '#fff',
    resource: null,
    neighbors: [],
    x: 0,
    y: 0,
  };
}

describe('PreparationPanelComponent — répartition et annulation', () => {
  let distribute: jest.Mock;
  let unplaceUnits: jest.Mock;
  let resetPreparation: jest.Mock;
  let markReady: jest.Mock;
  let unmarkReady: jest.Mock;
  let dialogOpen: jest.Mock;

  beforeEach(async () => {
    distribute = jest.fn(() => of(void 0));
    unplaceUnits = jest.fn(() => of(void 0));
    resetPreparation = jest.fn(() => of(void 0));
    markReady = jest.fn(() => of(void 0));
    unmarkReady = jest.fn(() => of(void 0));
    dialogOpen = jest.fn(() => ({ afterClosed: () => of(true) }));
    await TestBed.configureTestingModule({
      imports: [PreparationPanelComponent],
      providers: [
        provideRouter([]),
        {
          provide: ApiService,
          useValue: {
            distributePreparationUnits: distribute,
            unplacePreparationCharacter: jest.fn(() => of(void 0)),
            unplacePreparationBuilding: jest.fn(() => of(void 0)),
            unplacePreparationUnits: unplaceUnits,
            resetPreparation,
            markPreparationReady: markReady,
            unmarkPreparationReady: unmarkReady,
          },
        },
        {
          provide: ShopService,
          useValue: {
            unitCatalog: () => ({
              classes: [
                { name: 'LEGER', code: 'L' },
                { name: 'SNIPER', code: 'S' },
              ],
              entries: [],
            }),
          },
        },
        { provide: MatDialog, useValue: { open: dialogOpen } },
        { provide: MatSnackBar, useValue: { open: jest.fn() } },
      ],
    }).compileComponents();
  });

  function create(
    units: Unit[],
    options: {
      player?: Partial<Player>;
      sectors?: Sector[];
      ownerNames?: Record<number, string>;
    } = {},
  ): ComponentFixture<PreparationPanelComponent> {
    const fixture = TestBed.createComponent(PreparationPanelComponent);
    fixture.componentRef.setInput('player', player(options.player));
    fixture.componentRef.setInput('sectors', options.sectors ?? [sector()]);
    fixture.componentRef.setInput('reserveUnits', units);
    fixture.componentRef.setInput('currentTurn', 0);
    fixture.componentRef.setInput('ownerNames', options.ownerNames ?? {});
    fixture.detectChanges();
    fixture.componentInstance.unitSector.set(1);
    return fixture;
  }

  it('pose 10 unités par défaut et conserve la taille du lot suivant', async () => {
    const fixture = create(Array.from({ length: 100 }, (_, index) => unit(index + 1)));

    await fixture.componentInstance.distributeUnits();
    expect(distribute).toHaveBeenLastCalledWith(1, 10, ['LEGER']);
    expect(fixture.componentInstance.unitQuantity()).toBe(10);

    await fixture.componentInstance.distributeUnits();
    expect(distribute).toHaveBeenLastCalledWith(1, 10, ['LEGER']);
  });

  it('borne le lot suivant au restant', async () => {
    const fixture = create(Array.from({ length: 30 }, (_, index) => unit(index + 1)));
    fixture.componentInstance.unitQuantity.set(25);

    await fixture.componentInstance.distributeUnits();

    expect(distribute).toHaveBeenLastCalledWith(1, 25, ['LEGER']);
    expect(fixture.componentInstance.unitQuantity()).toBe(5);
  });

  it('regroupe les unités placées par secteur et classes', () => {
    const fixture = create([], {
      sectors: [
        sector(1, [unit(1, ['SNIPER']), unit(2, ['SNIPER']), unit(3, ['MASTODONTE'])]),
        sector(2, [unit(4, ['TIREUR'])]),
      ],
    });

    expect(fixture.componentInstance.placedUnitGroups()).toEqual([
      {
        key: '1|MASTODONTE',
        sectorNumber: 1,
        classNames: ['MASTODONTE'],
        label: 'MASTODONTE',
        count: 1,
      },
      {
        key: '1|SNIPER',
        sectorNumber: 1,
        classNames: ['SNIPER'],
        label: 'SNIPER',
        count: 2,
      },
      {
        key: '2|TIREUR',
        sectorNumber: 2,
        classNames: ['TIREUR'],
        label: 'TIREUR',
        count: 1,
      },
    ]);
  });

  it('retire un groupe d’unités placées', async () => {
    const fixture = create([], {
      sectors: [sector(1, [unit(1, ['SNIPER']), unit(2, ['SNIPER'])])],
    });

    await fixture.componentInstance.unplaceUnitGroup(
      fixture.componentInstance.placedUnitGroups()[0],
    );

    expect(unplaceUnits).toHaveBeenCalledWith(1, 2, ['SNIPER']);
  });

  it('réinitialise tout le placement après confirmation', async () => {
    const fixture = create([], { player: { buildings: [building(7, 1)] } });

    await fixture.componentInstance.resetPlacement();

    expect(dialogOpen).toHaveBeenCalled();
    expect(resetPreparation).toHaveBeenCalled();
  });

  it('affiche le contexte des secteurs et les propriétaires adjacents', () => {
    const fixture = create([], {
      sectors: [
        { ...sector(5), income: 2000, resource: 'Prométhéum', neighbors: [1, 6] },
        { ...sector(6), income: 1500, resource: null, neighbors: [5, 7] },
      ],
      ownerNames: { 6: 'Voisin', 7: 'Joueur' },
    });

    const details = fixture.componentInstance.sectorDetails();
    expect(details).toHaveLength(2);
    expect(details[0]).toMatchObject({
      number: 5,
      name: 'Secteur 5',
      resource: 'Prométhéum',
      adjacentOwners: ['Voisin'],
    });
    expect(details[1].adjacentOwners).toEqual([]);
    expect(fixture.componentInstance.sectorLabel(5)).toContain('Prométhéum');
  });

  it('marque le joueur prêt', async () => {
    const fixture = create([]);

    await fixture.componentInstance.markReady();

    expect(markReady).toHaveBeenCalled();
  });

  it('annule le statut prêt', async () => {
    const fixture = create([], { player: { preparationReady: true } });
    expect(fixture.componentInstance.playerReady()).toBe(true);

    await fixture.componentInstance.unmarkReady();

    expect(unmarkReady).toHaveBeenCalled();
  });
});
