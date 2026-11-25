import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DiscardEquipmentDialogComponent } from './discard-equipment-dialog.component';
import { PlayerService } from '../../services/player.service';
import type { EquipmentStack } from '../../models';

describe('DiscardEquipmentDialogComponent', () => {
  const playerService = {
    player: signal({ id: 1, race: 'ORKS' }),
    discardEquipment: jest.fn().mockResolvedValue(true),
    error: signal<string | null>(null),
  };
  const snackBar = { open: jest.fn() };
  const dialogRef = { close: jest.fn() };
  const stack = {
    equipment: { name: 'Fusil', displayNames: { ORKS: 'Shoota' } },
    available: 4,
    quantity: 6,
  } as EquipmentStack;

  beforeEach(() => {
    jest.clearAllMocks();
    playerService.error.set(null);
    playerService.discardEquipment.mockResolvedValue(true);
    TestBed.configureTestingModule({
      providers: [
        { provide: PlayerService, useValue: playerService },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { buildingId: 3, stack } },
      ],
    });
  });

  function component(): DiscardEquipmentDialogComponent {
    return TestBed.runInInjectionContext(() => new DiscardEquipmentDialogComponent());
  }

  it('borne la quantité entre 1 et le stock disponible', () => {
    const page = component();
    expect(page.maxQuantity()).toBe(4);
    expect(page.quantity()).toBe(4);

    page.increase();
    expect(page.quantity()).toBe(4);
    page.decrease();
    expect(page.quantity()).toBe(3);
    page.setMax();
    expect(page.quantity()).toBe(4);
    page.onInput({ target: { value: '999' } } as unknown as Event);
    expect(page.quantity()).toBe(4);
    page.onInput({ target: { value: '0' } } as unknown as Event);
    expect(page.quantity()).toBe(1);
  });

  it('affiche le libellé de la race', () => {
    expect(component().equipmentLabel()).toBe('Shoota');
  });

  it('jette le stock et ferme en cas de succès', async () => {
    const page = component();
    page.setMax();

    await page.confirm();

    expect(playerService.discardEquipment).toHaveBeenCalledWith(3, 'Fusil', 4);
    expect(dialogRef.close).toHaveBeenCalled();
    expect(snackBar.open).toHaveBeenCalledWith('Équipements jetés définitivement', 'Fermer', {
      duration: 3000,
    });
  });

  it('garde la fenêtre ouverte et affiche l’erreur en cas d’échec', async () => {
    playerService.discardEquipment.mockResolvedValueOnce(false);
    playerService.error.set('Cache introuvable');
    const page = component();

    await page.confirm();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(snackBar.open).toHaveBeenCalledWith('Cache introuvable', 'Fermer', { duration: 5000 });
  });
});
