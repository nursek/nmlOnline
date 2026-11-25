import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UnitSlotPickerComponent } from './unit-slot-picker.component';
import { PlayerService } from '../../services/player.service';
import type { Equipment, Player, Unit } from '../../models';

describe('UnitSlotPickerComponent', () => {
  const melee = (name: string, classes = ['LEGER'], available = 1) =>
    ({
      equipment: {
        name,
        category: 'MELEE',
        displayNames: { ORKS: `Ork ${name}` },
        compatibleClass: classes.map((unitClass) => ({ name: unitClass })),
      },
      quantity: available,
      available,
    }) as never;
  const unit = {
    id: 3,
    type: { maxFirearms: 1, maxMeleeWeapons: 1, maxDefensiveEquipment: 1 },
    classes: [{ name: 'LEGER' }],
    equipments: [],
  } as unknown as Unit;
  const player = signal({
    id: 1,
    race: 'ORKS',
    sectors: [{ number: 5, army: [unit] }],
    equipments: [melee('Épée'), melee('Hache', ['TIREUR']), melee('Vide', ['LEGER'], 0)],
  } as unknown as Player);
  const playerService = {
    player,
    assignUnitEquipment: jest.fn().mockResolvedValue(unit),
  };
  const snackBar = { open: jest.fn() };
  const dialogRef = { close: jest.fn() };

  beforeEach(() => {
    jest.clearAllMocks();
    playerService.assignUnitEquipment.mockResolvedValue(unit);
    TestBed.configureTestingModule({
      providers: [
        { provide: PlayerService, useValue: playerService },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialogRef, useValue: dialogRef },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { unitId: 3, category: 'MELEE', categoryLabel: 'Mêlée' },
        },
      ],
    });
  });

  function component(): UnitSlotPickerComponent {
    return TestBed.runInInjectionContext(() => new UnitSlotPickerComponent());
  }

  it('sépare les équipements éligibles des inéligibles', () => {
    const page = component();

    expect(page.eligible().map((stack) => stack.equipment.name)).toEqual(['Épée']);
    expect(page.ineligible().map((stack) => stack.equipment.name)).toEqual(['Hache']);
    expect(page.ineligibleReason(melee('Hache', ['TIREUR']).equipment as Equipment)).toBe(
      'Classe incompatible',
    );
    expect(page.equipmentLabel(melee('Épée').equipment as Equipment)).toBe('Ork Épée');
  });

  it('explique une catégorie pleine et l’indisponibilité', () => {
    TestBed.resetTestingModule();
    const fullUnit = { ...unit, equipments: [{ category: 'MELEE' }] } as unknown as Unit;
    const fullPlayer = signal({
      id: 1,
      race: 'ORKS',
      sectors: [{ number: 5, army: [fullUnit] }],
      equipments: [melee('Épée')],
    } as unknown as Player);
    TestBed.configureTestingModule({
      providers: [
        { provide: PlayerService, useValue: { player: fullPlayer, assignUnitEquipment: jest.fn() } },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialogRef, useValue: dialogRef },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { unitId: 3, category: 'MELEE', categoryLabel: 'Mêlée' },
        },
      ],
    });
    const page = TestBed.runInInjectionContext(() => new UnitSlotPickerComponent());

    expect(page.eligible()).toEqual([]);
    expect(page.ineligible()).toHaveLength(1);
    expect(page.ineligibleReason(melee('Épée').equipment as Equipment)).toBe('Catégorie pleine');
  });

  it('équipe un objet éligible et ferme', async () => {
    const page = component();

    await page.equip(melee('Épée').equipment as Equipment);

    expect(playerService.assignUnitEquipment).toHaveBeenCalledWith(3, 'Épée');
    expect(snackBar.open).toHaveBeenCalledWith('Ork Épée équipé', 'OK', { duration: 2500 });
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('reste ouvert si le service refuse', async () => {
    playerService.assignUnitEquipment.mockResolvedValueOnce(null);
    const page = component();

    await page.equip(melee('Épée').equipment as Equipment);

    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('ferme sans équiper', () => {
    component().close();
    expect(dialogRef.close).toHaveBeenCalled();
  });
});
