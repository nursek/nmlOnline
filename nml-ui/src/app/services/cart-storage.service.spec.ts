import { TestBed } from '@angular/core/testing';
import { CartStorageService } from './cart-storage.service';
import type { CartItem, UnitCartItem, VehicleCartItem } from '../models';

describe('CartStorageService', () => {
  let storage: CartStorageService;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [CartStorageService] });
    storage = TestBed.inject(CartStorageService);
  });

  afterEach(() => sessionStorage.clear());

  const equipment = { name: 'Fusil', cost: 100 } as CartItem['equipment'];
  const cartItem: CartItem = { equipment, quantity: 2 };
  const vehicleItem: VehicleCartItem = {
    vehicleType: { name: 'Tank', displayName: 'Char', cost: 50 } as VehicleCartItem['vehicleType'],
    quantity: 1,
  };
  const unitItem: UnitCartItem = {
    unitType: { name: 'LARBIN', cost: 400 } as UnitCartItem['unitType'],
    unitClass: { name: 'LEGER' } as UnitCartItem['unitClass'],
    quantity: 3,
  };

  it('fait l’aller-retour des trois paniers', () => {
    storage.saveCart([cartItem]);
    storage.saveVehicleCart([vehicleItem]);
    storage.saveUnitCart([unitItem]);

    expect(storage.loadCart()).toEqual([cartItem]);
    expect(storage.loadVehicleCart()).toEqual([vehicleItem]);
    expect(storage.loadUnitCart()).toEqual([unitItem]);
  });

  it('ignore un JSON corrompu ou non tableau', () => {
    sessionStorage.setItem('nml_cart', '{pas du json');
    sessionStorage.setItem('nml_vehicle_cart', '{"quantite":1}');
    sessionStorage.setItem('nml_unit_cart', 'null');

    expect(storage.loadCart()).toEqual([]);
    expect(storage.loadVehicleCart()).toEqual([]);
    expect(storage.loadUnitCart()).toEqual([]);
  });

  it('filtre les lignes invalides sans perdre les valides', () => {
    sessionStorage.setItem(
      'nml_cart',
      JSON.stringify([
        cartItem,
        { equipment, quantity: 0 },
        { equipment, quantity: 1.5 },
        { equipment: { name: '', cost: 10 }, quantity: 1 },
        { equipment: { name: 'Prix négatif', cost: -1 }, quantity: 1 },
        null,
      ]),
    );

    expect(storage.loadCart()).toEqual([cartItem]);
  });

  it('filtre un panier unités sans classe valide', () => {
    sessionStorage.setItem(
      'nml_unit_cart',
      JSON.stringify([unitItem, { unitType: unitItem.unitType, quantity: 1 }]),
    );

    expect(storage.loadUnitCart()).toEqual([unitItem]);
  });

  it('supprime la clé quand le panier devient vide', () => {
    storage.saveCart([cartItem]);
    expect(sessionStorage.getItem('nml_cart')).not.toBeNull();

    storage.saveCart([]);

    expect(sessionStorage.getItem('nml_cart')).toBeNull();
  });
});
