import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AutoEquipDialogComponent } from './auto-equip-dialog.component';
import { PlayerService } from '../../services/player.service';
import type { Player } from '../../models';

describe('AutoEquipDialogComponent', () => {
  const stack = (name: string, category: string, cost: number, available = 2) => ({
    equipment: { name, category, cost, displayNames: { ORKS: `Ork ${name}` } },
    quantity: available,
    available,
  });
  const player = signal({
    id: 1,
    race: 'ORKS',
    equipments: [
      stack('Petit fusil', 'FIREARM', 100),
      stack('Gros fusil', 'FIREARM', 500),
      stack('Épée', 'MELEE', 200),
      stack('Armure', 'DEFENSIVE', 300),
      stack('Fusil épuisé', 'FIREARM', 900, 0),
    ],
    buildings: [],
  } as unknown as Player);
  const playerService = {
    player,
    error: signal<string | null>(null),
    autoEquipUnits: jest.fn(),
  };
  const snackBar = { open: jest.fn() };
  const dialogRef = { close: jest.fn() };

  beforeEach(() => {
    jest.clearAllMocks();
    playerService.error.set(null);
    playerService.autoEquipUnits.mockResolvedValue({ equippedCount: 2, unitCount: 1 });
    TestBed.configureTestingModule({
      providers: [
        { provide: PlayerService, useValue: playerService },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    });
  });

  function component(): AutoEquipDialogComponent {
    return TestBed.runInInjectionContext(() => new AutoEquipDialogComponent());
  }

  it('trie les armes à feu par coût et exclut les stocks vides', () => {
    const page = component();

    expect(page.firearmStacks().map((item) => item.equipment.name)).toEqual([
      'Gros fusil',
      'Petit fusil',
    ]);
    expect(page.equipmentLabel(page.firearmStacks()[0].equipment)).toBe('Ork Gros fusil');
    expect(page.canSubmit()).toBe(true);
  });

  it('désélectionne une arme à feu et bloque quand aucune catégorie', () => {
    const page = component();
    page.toggleFirearm('Gros fusil');
    expect(page.selectedFirearms().map((item) => item.equipment.name)).toEqual(['Petit fusil']);
    expect(page.isFirearmSelected('Gros fusil')).toBe(false);

    page.firearm.set(false);
    page.melee.set(false);
    page.defensive.set(false);
    expect(page.canSubmit()).toBe(false);
  });

  it('envoie les catégories et équipements sélectionnés', async () => {
    const page = component();
    page.toggleFirearm('Gros fusil');

    await page.submit();

    expect(playerService.autoEquipUnits).toHaveBeenCalledWith(
      ['FIREARM', 'MELEE', 'DEFENSIVE'],
      ['Petit fusil', 'Épée', 'Armure'],
    );
    expect(snackBar.open).toHaveBeenCalledWith(
      '2 équipement(s) attribué(s) à 1 unité(s)',
      'Fermer',
      { duration: 3500 },
    );
    expect(dialogRef.close).toHaveBeenCalledWith({ equippedCount: 2, unitCount: 1 });
  });

  it('signale l’absence d’équipement disponible', async () => {
    const page = component();
    page.toggleFirearm('Petit fusil');
    page.toggleFirearm('Gros fusil');
    page.melee.set(false);
    page.defensive.set(false);

    await page.submit();

    expect(playerService.autoEquipUnits).not.toHaveBeenCalled();
    expect(snackBar.open).toHaveBeenCalledWith(
      'Aucun équipement disponible pour ces catégories',
      'Fermer',
      { duration: 3000 },
    );
  });

  it('signale l’erreur du service et ferme', () => {
    playerService.autoEquipUnits.mockResolvedValueOnce(null);
    playerService.error.set('Quota dépassé');
    const page = component();

    return page.submit().then(() => {
      expect(snackBar.open).toHaveBeenCalledWith('Quota dépassé', 'Fermer', { duration: 5000 });
      expect(dialogRef.close).not.toHaveBeenCalled();
    });
  });

  it('ferme sans soumettre', () => {
    component().close();
    expect(dialogRef.close).toHaveBeenCalled();
  });
});
