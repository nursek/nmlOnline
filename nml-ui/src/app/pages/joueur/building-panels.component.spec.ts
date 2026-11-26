import { TestBed } from '@angular/core/testing';
import { HeadquartersPanelComponent } from './headquarters-panel.component';
import { BankPanelComponent } from './bank-panel.component';
import { WeaponCachePanelComponent } from './weapon-cache-panel.component';
import type { Building, EquipmentStack } from '../../models';

const headquarters = {
  id: 1,
  buildingType: 'HEADQUARTERS',
  sectorNumber: 5,
  lastMovedTurn: null,
  isDestroyed: false,
  isOperational: true,
} as unknown as Building;

describe('HeadquartersPanelComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HeadquartersPanelComponent] });
  });

  it('expose le statut et émet les actions', () => {
    const fixture = TestBed.createComponent(HeadquartersPanelComponent);
    fixture.componentRef.setInput('building', headquarters);
    fixture.componentRef.setInput('currentTurn', 4);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const moves: Building[] = [];
    let alliance = 0;
    component.move.subscribe((building) => moves.push(building));
    component.openAlliance.subscribe(() => alliance++);

    expect(component.status()).toBeTruthy();
    component.requestMove();
    component.requestAlliance();

    expect(moves).toEqual([headquarters]);
    expect(alliance).toBe(1);
  });

  it('ignore le déplacement sans bâtiment', () => {
    const fixture = TestBed.createComponent(HeadquartersPanelComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const moves: Building[] = [];
    component.move.subscribe((building) => moves.push(building));

    expect(component.status()).toBeNull();
    component.requestMove();
    expect(moves).toEqual([]);
  });
});

describe('BankPanelComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [BankPanelComponent] });
  });

  it('affiche l’état et émet le déplacement', () => {
    const fixture = TestBed.createComponent(BankPanelComponent);
    fixture.componentRef.setInput('building', {
      ...headquarters,
      buildingType: 'BANK',
    } as Building);
    fixture.componentRef.setInput('currentTurn', 5);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const moves: Building[] = [];
    component.move.subscribe((building) => moves.push(building));

    expect(component.status()).toBeTruthy();
    component.requestMove();
    expect(moves).toHaveLength(1);
  });
});

describe('WeaponCachePanelComponent', () => {
  const stacks = [
    { quantity: 3, available: 2 },
    { quantity: 2, available: 2 },
  ] as EquipmentStack[];
  const cache = {
    ...headquarters,
    buildingType: 'WEAPON_CACHE',
    maxCapacity: 10,
    sectorNumber: null,
  } as Building;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [WeaponCachePanelComponent] });
  });

  it('calcule total, capacité, remplissage et secteur', () => {
    const fixture = TestBed.createComponent(WeaponCachePanelComponent);
    fixture.componentRef.setInput('building', cache);
    fixture.componentRef.setInput('stacks', stacks);
    fixture.componentRef.setInput('currentTurn', 5);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    expect(component.total()).toBe(5);
    expect(component.maxCapacity()).toBe(10);
    expect(component.sectorLabel()).toBe('hors secteur');
    expect(component.fillPercentage()).toBe(50);
    expect(component.status()).toBeTruthy();

    const moves: Building[] = [];
    component.move.subscribe((building) => moves.push(building));
    component.requestMove();
    expect(moves).toEqual([cache]);
  });

  it('gère une cache absente ou une capacité nulle', () => {
    const fixture = TestBed.createComponent(WeaponCachePanelComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    expect(component.total()).toBe(0);
    expect(component.maxCapacity()).toBe(0);
    expect(component.fillPercentage()).toBe(0);
    expect(component.sectorLabel()).toBe('hors secteur');
    expect(component.status()).toBeNull();
  });
});
