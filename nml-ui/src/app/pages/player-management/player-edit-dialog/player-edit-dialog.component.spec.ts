import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import { PlayerEditDialogComponent } from './player-edit-dialog.component';
import { AdminService } from '../../../services/admin.service';
import { ApiService } from '../../../services/api.service';

describe('PlayerEditDialogComponent — mutations admin', () => {
  const baseEdit = () => ({
    player: {
      id: 1,
      name: 'lurio',
      stats: { money: 100 },
      sectors: [
        {
          number: 5,
          name: 'Secteur 5',
          vehicles: [{ id: 7, displayName: 'VTT', sectorNumber: 5 }],
        },
      ],
      character: null as { name: string; baseAttack: number } | null,
      equipments: [],
      buildings: [],
    },
    units: [],
    vehicles: [],
  });
  const edit = signal(baseEdit());
  const admin = {
    edit,
    catalog: signal(null),
    players: signal([]),
    editLoading: signal(false),
    openEdit: jest.fn(),
    closeEdit: jest.fn(),
    mutate: jest.fn().mockResolvedValue(undefined),
  };
  const api = {
    adminGetAudit: jest.fn(() => of({ content: [], totalElements: 0 })),
    adminSetMoney: jest.fn(() => of(undefined)),
    adminAddResource: jest.fn(() => of(undefined)),
    adminAddEquipment: jest.fn(() => of(undefined)),
    adminAddVehicles: jest.fn(() => of(undefined)),
    adminDeleteVehicle: jest.fn(() => of(undefined)),
    adminDeleteUnit: jest.fn(() => of(undefined)),
    adminAddBuilding: jest.fn(() => of(undefined)),
    adminDeleteBuilding: jest.fn(() => of(undefined)),
    adminAddBankResource: jest.fn(() => of(undefined)),
    adminRemoveBankResource: jest.fn(() => of(undefined)),
    adminCreateCharacter: jest.fn(() => of(undefined)),
    adminUpdateCharacter: jest.fn(() => of(undefined)),
    adminDeleteCharacter: jest.fn(() => of(undefined)),
    adminSetVehicleCrew: jest.fn(() => of(undefined)),
    adminAddCacheEquipment: jest.fn(() => of(undefined)),
    adminRemoveCacheEquipment: jest.fn(() => of(undefined)),
    adminReassignSector: jest.fn(() => of(undefined)),
  };
  const dialog = { open: jest.fn() };
  const dialogRef = { close: jest.fn() };

  beforeEach(() => {
    jest.clearAllMocks();
    edit.set(baseEdit());
    dialog.open.mockReturnValue({ afterClosed: () => of(true) });
    admin.catalog.set(null);
    admin.players.set([]);
    TestBed.configureTestingModule({
      providers: [
        { provide: AdminService, useValue: admin },
        { provide: ApiService, useValue: api },
        { provide: MatDialog, useValue: dialog },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { playerId: 1, playerName: 'lurio' } },
      ],
    });
  });

  function component(): PlayerEditDialogComponent {
    return TestBed.runInInjectionContext(() => new PlayerEditDialogComponent());
  }

  it('refuse un montant négatif sans appel API', async () => {
    const page = component();
    page.money.set(-1);

    await page.applyMoney();

    expect(api.adminSetMoney).not.toHaveBeenCalled();
  });

  it('met à jour l’argent via la mutation admin', async () => {
    const page = component();
    page.money.set(2500);

    await page.applyMoney();

    expect(api.adminSetMoney).toHaveBeenCalledWith(1, 2500);
    expect(admin.mutate).toHaveBeenCalled();
    expect(page.money()).toBeNull();
  });

  it('ajoute une ressource valide et ignore une quantité nulle', async () => {
    const page = component();
    page.resourceName.set('Auramite');
    page.resourceQuantity.set(0);
    await page.addResource();
    expect(api.adminAddResource).not.toHaveBeenCalled();

    page.resourceQuantity.set(3);
    await page.addResource();
    expect(api.adminAddResource).toHaveBeenCalledWith(1, 'Auramite', 3);
    expect(page.resourceQuantity()).toBe(1);
  });

  it('ajoute un véhicule au secteur possédé, ignore une quantité invalide', async () => {
    const page = component();
    page.vehicleType.set('VTT_LEGER');
    page.vehicleQuantity.set(0);
    await page.addVehicle();
    expect(api.adminAddVehicles).not.toHaveBeenCalled();

    page.vehicleQuantity.set(2);
    await page.addVehicle();
    expect(api.adminAddVehicles).toHaveBeenCalledWith(1, {
      vehicleType: 'VTT_LEGER',
      quantity: 2,
      sectorNumber: 5,
    });
  });

  it('supprime unité et véhicule après confirmation', async () => {
    const page = component();

    await page.deleteUnit({ id: 10, number: 1, sectorNumber: 5 } as never);
    await page.deleteVehicle({ id: 20, displayName: 'VTT léger', sectorNumber: 5 } as never);

    expect(api.adminDeleteUnit).toHaveBeenCalledWith(1, 10);
    expect(api.adminDeleteVehicle).toHaveBeenCalledWith(1, 20);
  });

  it('ajoute puis supprime un bâtiment', async () => {
    const page = component();
    page.buildingType.set('BANK');

    await page.addBuilding();
    await page.deleteBuilding({ id: 30, buildingType: 'BANK', sectorNumber: 5 } as never);

    expect(api.adminAddBuilding).toHaveBeenCalledWith(1, {
      buildingType: 'BANK',
      sectorNumber: 5,
    });
    expect(api.adminDeleteBuilding).toHaveBeenCalledWith(1, 30);
  });

  it('ajoute une ressource en banque et ignore une quantité nulle', async () => {
    const page = component();
    page.bankResourceName.set('Auramite');
    page.bankResourceQuantity.set(0);
    await page.addBankResource();
    expect(api.adminAddBankResource).not.toHaveBeenCalled();

    page.bankResourceQuantity.set(4);
    await page.addBankResource();
    expect(api.adminAddBankResource).toHaveBeenCalledWith(1, 'Auramite', 4);
    expect(page.bankResourceQuantity()).toBe(1);
  });

  it('crée puis met à jour le personnage', async () => {
    const page = component();
    page.characterName.set('');
    await page.saveCharacter();
    expect(api.adminCreateCharacter).not.toHaveBeenCalled();

    page.characterName.set('  Héros  ');
    page.characterAttack.set(10);
    await page.saveCharacter();
    expect(api.adminCreateCharacter).toHaveBeenCalledWith(1, {
      name: 'Héros',
      baseAttack: 10,
      baseDefense: 0,
      basePdf: 0,
      basePdc: 0,
      baseArmor: 0,
      baseEvasion: 0,
    });
    expect(admin.mutate).toHaveBeenCalled();
  });

  it('met à jour le personnage existant et le supprime après confirmation', async () => {
    edit.update((value) => ({
      ...value,
      player: { ...value.player, character: { name: 'Héros', baseAttack: 10 } },
    }));
    const page = component();
    page.characterName.set('Héros');
    await page.saveCharacter();
    expect(api.adminUpdateCharacter).toHaveBeenCalledWith(1, expect.objectContaining({ name: 'Héros' }));

    await page.deleteCharacter();
    expect(api.adminDeleteCharacter).toHaveBeenCalledWith(1);
  });

  it('met à jour l’équipage d’un véhicule déployé', async () => {
    dialog.open.mockReturnValueOnce({
      afterClosed: () => of({ pilotId: 2, passengerIds: [3] }),
    });
    const page = component();

    page.openVehicleCrew({ id: 7, displayName: 'VTT', sectorNumber: 5, isDestroyed: false } as never);
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(api.adminSetVehicleCrew).toHaveBeenCalledWith(1, 7, 2, [3]);
  });

  it('ajoute et retire du stock d’une cache d’armes', async () => {
    admin.catalog.set({ equipments: [{ name: 'Fusil' }] } as never);
    const cache = { id: 30, availableCapacity: 2, storedEquipments: [] };
    const page = component();
    page.addCacheEquipment(cache as never);

    expect(api.adminAddCacheEquipment).toHaveBeenCalledWith(1, 30, 'Fusil', 1);

    await page.removeCacheEquipment(cache as never, {
      equipment: { name: 'Fusil' },
      available: 2,
    } as never);
    expect(api.adminRemoveCacheEquipment).toHaveBeenCalledWith(1, 30, 'Fusil', 1);
  });

  it('retire une ressource de la banque avec la quantité bornée', async () => {
    const page = component();

    await page.removeBankResource({ name: 'Auramite', quantity: 5 } as never);

    expect(api.adminRemoveBankResource).toHaveBeenCalledWith(1, 'Auramite', 1);
  });

  it('réassigne un secteur à un autre joueur après confirmation', async () => {
    admin.players.set([{ id: 2, name: 'nursek', eliminated: false }] as never);
    const page = component();
    (page as unknown as { sectorChoice: { set(value: number): void } }).sectorChoice.set(5);
    page.sectorTarget.set(2);

    await page.reassignSector();

    expect(api.adminReassignSector).toHaveBeenCalledWith(5, 2);
  });

  it('ferme la boîte de dialogue', () => {
    component().close();
    expect(dialogRef.close).toHaveBeenCalled();
  });
});
