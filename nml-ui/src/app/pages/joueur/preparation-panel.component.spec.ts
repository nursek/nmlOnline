import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PreparationPanelComponent } from './preparation-panel.component';
import { ApiService } from '../../services/api.service';
import { ShopService } from '../../services/shop.service';
import type { Player, Sector, Unit } from '../../models';

function unit(id: number): Unit {
  return {
    id,
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
    classes: [],
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

function player(): Player {
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
  };
}

function sector(): Sector {
  return {
    number: 1,
    name: 'Secteur 1',
    income: 0,
    army: [],
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

describe('PreparationPanelComponent — répartition par lots', () => {
  let distribute: jest.Mock;

  beforeEach(async () => {
    distribute = jest.fn(() => of(void 0));
    await TestBed.configureTestingModule({
      imports: [PreparationPanelComponent],
      providers: [
        provideRouter([]),
        { provide: ApiService, useValue: { distributePreparationUnits: distribute } },
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
        { provide: MatSnackBar, useValue: { open: jest.fn() } },
      ],
    }).compileComponents();
  });

  function create(units: Unit[]): ComponentFixture<PreparationPanelComponent> {
    const fixture = TestBed.createComponent(PreparationPanelComponent);
    fixture.componentRef.setInput('player', player());
    fixture.componentRef.setInput('sectors', [sector()]);
    fixture.componentRef.setInput('reserveUnits', units);
    fixture.componentRef.setInput('currentTurn', 0);
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
});
