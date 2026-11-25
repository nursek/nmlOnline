import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { BoutiqueComponent } from './boutique.component';
import { ShopService } from '../../services/shop.service';
import { PlayerService } from '../../services/player.service';
import { EquipmentImageService } from '../../core/equipment-image.service';
import { VehicleImageService } from '../../core/vehicle-image.service';
import type { Equipment } from '../../models';

describe('BoutiqueComponent — achat du panier', () => {
  const equipment = { name: 'Fusil', cost: 100, category: 'FIREARM', pdfBonus: 0 } as Equipment;
  const cart = signal([{ equipment, quantity: 2 }]);
  const purchaseLoading = signal(false);
  const vehicleCart = signal<unknown[]>([]);
  const unitCart = signal<unknown[]>([]);
  const sellCart = signal<unknown[]>([]);
  const shop = {
    equipments: signal([]),
    cart,
    vehicleCart,
    unitCart,
    sellCart,
    error: signal<string | null>(null),
    equipmentsLoading: signal(false),
    purchaseLoading,
    cartTotalItems: signal(2),
    cartTotalPrice: signal(200),
    vehicleCartTotalItems: signal(0),
    vehicleCartTotalPrice: signal(0),
    unitCartTotalItems: signal(0),
    unitCartTotalPrice: signal(0),
    sellCartTotalValue: signal(0),
    vehicleTypes: signal([]),
    unitCatalog: signal({ classes: [], entries: [] }),
    refreshUnitCatalog: jest.fn(),
    checkoutEquipments: jest.fn().mockResolvedValue({}),
    checkoutVehicles: jest.fn().mockResolvedValue([]),
    checkoutUnits: jest.fn().mockResolvedValue([]),
    checkoutSellCart: jest.fn().mockResolvedValue(900),
    addToSellCart: jest.fn(),
    removeFromSellCart: jest.fn(),
    addUnitToCart: jest.fn(),
    updateUnitCartItemQuantity: jest.fn(),
    removeUnitFromCart: jest.fn(),
    clearUnitCart: jest.fn(),
    addVehicleToCart: jest.fn(),
    updateVehicleCartItemQuantity: jest.fn(),
    removeVehicleFromCart: jest.fn(),
    clearVehicleCart: jest.fn(),
    clearSellCart: jest.fn(),
    addToCart: jest.fn(),
    removeFromCart: jest.fn(),
    updateCartItemQuantity: jest.fn(),
    clearCart: jest.fn(),
  };
  const playerService = {
    player: signal({ id: 1, race: 'ORKS', stats: { money: 1000 }, buildings: [] }),
    currentTurn: signal(1),
    loadCurrent: jest.fn().mockResolvedValue(undefined),
    loadCurrentTurn: jest.fn().mockResolvedValue(undefined),
  };
  const dialog = { open: jest.fn().mockReturnValue({ afterClosed: () => of(null) }) };

  beforeEach(() => {
    jest.clearAllMocks();
    cart.set([{ equipment, quantity: 2 }]);
    vehicleCart.set([]);
    unitCart.set([]);
    sellCart.set([]);
    purchaseLoading.set(false);
    TestBed.configureTestingModule({
      providers: [
        { provide: ShopService, useValue: shop },
        { provide: PlayerService, useValue: playerService },
        { provide: EquipmentImageService, useValue: {} },
        { provide: VehicleImageService, useValue: {} },
        { provide: MatDialog, useValue: dialog },
      ],
    });
  });

  function component(): BoutiqueComponent {
    return TestBed.runInInjectionContext(() => new BoutiqueComponent());
  }

  it('un double clic ne passe qu’une commande et vide le panier', async () => {
    const boutique = component();

    const first = boutique.checkout();
    const second = boutique.checkout();
    await Promise.all([first, second]);

    expect(shop.checkoutEquipments).toHaveBeenCalledTimes(1);
    expect(shop.checkoutEquipments).toHaveBeenCalledWith(undefined);
    expect(boutique.checkoutBusy()).toBe(false);
    expect(dialog.open).toHaveBeenCalled();
  });

  it('ne commande rien avec un panier vide', async () => {
    cart.set([]);

    await component().checkout();

    expect(shop.checkoutEquipments).not.toHaveBeenCalled();
    expect(dialog.open).not.toHaveBeenCalled();
  });

  it('achète le panier véhicules et affiche le succès', async () => {
    vehicleCart.set([
      {
        vehicleType: { name: 'Tank', displayName: 'Char', cost: 50 },
        quantity: 2,
      },
    ]);
    const boutique = component();

    await boutique.checkoutVehicles();

    expect(shop.checkoutVehicles).toHaveBeenCalledTimes(1);
    expect(dialog.open).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ data: expect.objectContaining({ title: 'Véhicules achetés !' }) }),
    );
  });

  it('recrute le panier unités et affiche le succès', async () => {
    unitCart.set([
      {
        unitType: { name: 'LARBIN', cost: 400 },
        unitClass: { name: 'LEGER' },
        quantity: 3,
      },
    ]);
    const boutique = component();

    await boutique.checkoutUnits();

    expect(shop.checkoutUnits).toHaveBeenCalledTimes(1);
    expect(dialog.open).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ data: expect.objectContaining({ title: 'Unités recrutées !' }) }),
    );
  });

  it('vend le panier de ressources et affiche le total', async () => {
    sellCart.set([{ resource: { id: 7, name: 'Bois' }, quantity: 4 }]);
    const boutique = component();

    await boutique.checkoutSellCart();

    expect(shop.checkoutSellCart).toHaveBeenCalledTimes(1);
    expect(dialog.open).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        data: expect.objectContaining({ isSale: true, totalCost: 900 }),
      }),
    );
  });

  it('ignore les paniers véhicules/unités/vente vides', async () => {
    const boutique = component();

    await boutique.checkoutVehicles();
    await boutique.checkoutUnits();
    await boutique.checkoutSellCart();

    expect(shop.checkoutVehicles).not.toHaveBeenCalled();
    expect(shop.checkoutUnits).not.toHaveBeenCalled();
    expect(shop.checkoutSellCart).not.toHaveBeenCalled();
  });

  it('borne la quantité de vente et délègue au panier', () => {
    const boutique = component();
    const resource = { id: 7, name: 'Bois', quantity: 20, baseValue: 2 };

    boutique.setSellQty(resource, 99);
    expect(boutique.getSellQty(resource)).toBe(9);
    boutique.setSellQty(resource, 0);
    expect(boutique.getSellQty(resource)).toBe(1);
    boutique.setSellQty({ ...resource, id: null }, 5);
    expect(boutique.getSellQty({ ...resource, id: null })).toBe(1);

    boutique.addToSellCart(resource);
    expect(shop.addToSellCart).toHaveBeenCalledWith(resource, 1);
  });

  it('calcule le quota unités et borne la saisie', () => {
    const entry = {
      name: 'LARBIN',
      maxPerTurn: 5,
      purchasedThisTurn: 1,
      availableNow: true,
      cost: 400,
    } as never;
    const unitClass = { name: 'LEGER' } as never;
    shop.unitCatalog.set({ classes: [{ name: 'LEGER' }], entries: [entry] } as never);
    const boutique = component();
    boutique.unitQuantities.set({ [boutique.unitCartKey('LARBIN', 'LEGER')]: 2 });

    expect(boutique.unitRemaining(entry)).toBe(2);
    expect(boutique.unitQuotaUsed(entry)).toBe(3);
    expect(boutique.unitQuantityLimit(entry, unitClass)).toBe(4);
    expect(boutique.unitQuantityToAdd(entry, unitClass)).toBe(2);

    boutique.setUnitQuantity(entry, unitClass, 10);
    expect(boutique.unitQuantityLimit(entry, unitClass)).toBe(4);
    boutique.addUnitToCart(entry, unitClass);
    expect(shop.addUnitToCart).toHaveBeenCalledWith(entry, unitClass, 4);
    expect(boutique.unitQuantities()).toEqual({});
  });

  it('gère les lignes du panier unités et véhicules', () => {
    unitCart.set([
      { unitType: { name: 'LARBIN', cost: 400 }, unitClass: { name: 'LEGER' }, quantity: 2 },
    ]);
    vehicleCart.set([{ vehicleType: { name: 'Tank', displayName: 'Char', cost: 50 }, quantity: 3 }]);
    const boutique = component();

    expect(boutique.unitCartLineQuantity('LARBIN#LEGER')).toBe(2);
    boutique.decrementUnitCartQuantity('LARBIN#LEGER');
    expect(shop.updateUnitCartItemQuantity).toHaveBeenCalledWith('LARBIN#LEGER', 1);

    vehicleCart.set([{ vehicleType: { name: 'Tank', displayName: 'Char', cost: 50 }, quantity: 1 }]);
    expect(boutique.getVehicleCartQuantity('Tank')).toBe(1);
    boutique.decrementVehicleCartQuantity('Tank');
    expect(shop.removeVehicleFromCart).toHaveBeenCalledWith('Tank');

    boutique.setVehicleQuantity('Tank', 0);
    expect(boutique.getVehicleQuantity('Tank')).toBe(1);
    expect(boutique.canAffordVehicle(50, 10)).toBe(true);
    expect(boutique.vehicleAvailable({ name: 'Tank', availableFromTurn: 5 } as never)).toBe(false);
    expect(boutique.vehicleTurnStatus(5)).toContain('tour');
  });

  it('gère recherche, filtres de classe et types adjacents', () => {
    shop.equipments.set([
      { name: 'Fusil', compatibleClass: [{ name: 'LEGER' }, { name: 'TIREUR' }] },
    ] as never);
    shop.unitCatalog.set({
      classes: [{ name: 'LEGER' }],
      entries: [
        { name: 'LARBIN' },
        { name: 'BRUTE' },
      ],
    } as never);
    const boutique = component();

    expect(boutique.equipmentClasses().map((entry) => entry.key)).toEqual(['LEGER', 'TIREUR']);
    boutique.selectAdjacentClass(1);
    expect(boutique.selectedClassFilter()).toBe('TIREUR');
    boutique.selectAdjacentClass(1);
    expect(boutique.selectedClassFilter()).toBe('LEGER');

    boutique.selectAdjacentUnitType(1);
    expect(boutique.selectedUnitType()).toBe('BRUTE');
    boutique.selectAdjacentUnitType(1);
    expect(boutique.selectedUnitType()).toBe('LARBIN');

    boutique.toggleSearch();
    expect(boutique.searchOpen()).toBe(true);
    boutique.searchTerm.set('fus');
    boutique.toggleSearch();
    expect(boutique.searchOpen()).toBe(false);
    expect(boutique.searchTerm()).toBe('');
    boutique.clearSearch();
    expect(boutique.searchTerm()).toBe('');
  });

  it('délègue les images et gère les erreurs', () => {
    const equipment = { name: 'Fusil', displayNames: { ORKS: 'Shoota' } };
    const boutique = component();

    expect(boutique.equipmentLabel(equipment as never)).toBe('Shoota');
    expect(boutique.equipmentDescription(equipment as never)).toBe('');
    boutique.onImgError('unit:LARBIN');
    expect(boutique.hasImage('unit:LARBIN')).toBe(false);
    expect(boutique.hasImage('unit:BRUTE')).toBe(true);
  });

  it('libère le verrou après un échec de commande', async () => {
    shop.checkoutEquipments.mockRejectedValueOnce(new Error('refus'));
    const boutique = component();

    await boutique.checkout();

    expect(boutique.checkoutBusy()).toBe(false);
    expect(dialog.open).not.toHaveBeenCalled();
  });
});
