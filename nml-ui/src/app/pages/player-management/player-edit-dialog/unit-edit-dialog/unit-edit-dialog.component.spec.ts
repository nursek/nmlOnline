import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import { UnitEditDialogComponent } from './unit-edit-dialog.component';
import { AdminService } from '../../../../services/admin.service';
import { ApiService } from '../../../../services/api.service';

describe('UnitEditDialogComponent', () => {
  const unit = {
    id: 5,
    number: 1,
    type: { name: 'MALFRAT' },
    experience: 5,
    classes: [{ name: 'TIREUR' }],
    isInjured: true,
    sectorNumber: 5,
    equipments: [],
  };
  const admin = {
    edit: signal({
      player: { id: 1, sectors: [{ number: 5, name: 'Secteur 5' }], equipments: [] },
      units: [unit],
      vehicles: [],
    }),
    catalog: signal({ equipments: [], unitClasses: [{ name: 'TIREUR' }] }),
    mutate: jest.fn().mockResolvedValue(undefined),
  };
  const api = {
    adminUpdateUnit: jest.fn(() => of(undefined)),
    adminMoveUnit: jest.fn(() => of(undefined)),
    adminEquipUnit: jest.fn(() => of(undefined)),
    adminUnequipUnit: jest.fn(() => of(undefined)),
  };
  const dialogRef = { close: jest.fn() };

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: AdminService, useValue: admin },
        { provide: ApiService, useValue: api },
        { provide: MatDialog, useValue: { open: jest.fn() } },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { playerId: 1, unitId: 5 } },
      ],
    });
  });

  function component(): UnitEditDialogComponent {
    return TestBed.runInInjectionContext(() => new UnitEditDialogComponent());
  }

  it('pré-remplit le formulaire depuis l’unité éditée', () => {
    const page = component();
    TestBed.tick();

    expect(page.experience()).toBe(5);
    expect(page.classes()).toEqual(['TIREUR']);
    expect(page.injured()).toBe(true);
    expect(page.unitLabel(unit as never)).toBe('MALFRAT n°1');
  });

  it('enregistre l’expérience, les classes et la blessure', async () => {
    const page = component();
    TestBed.tick();
    page.experience.set(8);

    await page.save();

    expect(api.adminUpdateUnit).toHaveBeenCalledWith(1, 5, {
      experience: 8,
      classes: ['TIREUR'],
      injured: true,
    });
    expect(admin.mutate).toHaveBeenCalled();
  });

  it('refuse une expérience négative ou sans classe', async () => {
    const page = component();
    TestBed.tick();
    page.experience.set(-1);
    await page.save();
    expect(api.adminUpdateUnit).not.toHaveBeenCalled();

    page.experience.set(5);
    page.classes.set([]);
    await page.save();
    expect(api.adminUpdateUnit).not.toHaveBeenCalled();
  });

  it('déplace l’unité et gère équipement/retrait', async () => {
    const page = component();
    TestBed.tick();
    page.sectorChoice.set(7);

    await page.move();
    expect(api.adminMoveUnit).toHaveBeenCalledWith(1, 5, 7);

    page.equipmentToAdd.set('Fusil');
    await page.equip();
    expect(api.adminEquipUnit).toHaveBeenCalledWith(1, 5, 'Fusil');

    await page.unequip('Fusil');
    expect(api.adminUnequipUnit).toHaveBeenCalledWith(1, 5, 'Fusil');
  });

  it('ferme la boîte de dialogue', () => {
    component().close();
    expect(dialogRef.close).toHaveBeenCalled();
  });
});
