import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { CacheEquipmentDialogComponent } from './cache-equipment-dialog.component';
import { DiscardEquipmentDialogComponent } from './discard-equipment-dialog.component';
import { PlayerService } from '../../services/player.service';
import type { Equipment, EquipmentStack } from '../../models';

describe('CacheEquipmentDialogComponent', () => {
  const playerService = {
    player: signal({
      id: 1,
      race: 'ORKS',
      buildings: [
        {
          id: 3,
          storedEquipments: [
            { equipment: { name: 'Zarb', displayNames: { ORKS: 'Zarb' } }, quantity: 2, available: 2 },
            { equipment: { name: 'Fusil', displayNames: { ORKS: 'Shoota' } }, quantity: 3, available: 3 },
          ],
        },
      ],
    }),
  };
  const dialog = { open: jest.fn() };
  const dialogRef = { close: jest.fn() };

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: PlayerService, useValue: playerService },
        { provide: MatDialog, useValue: dialog },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { buildingId: 3 } },
      ],
    });
  });

  function component(): CacheEquipmentDialogComponent {
    return TestBed.runInInjectionContext(() => new CacheEquipmentDialogComponent());
  }

  it('trie les stacks par libellé et calcule le total', () => {
    const page = component();

    expect(page.stacks().map((stack) => stack.equipment.name)).toEqual(['Fusil', 'Zarb']);
    expect(page.total()).toBe(5);
    expect(page.equipmentLabel({ name: 'Fusil', displayNames: { ORKS: 'Shoota' } } as unknown as Equipment)).toBe('Shoota');
  });

  it('ouvre le jet d’équipement sur le bon bâtiment', () => {
    const page = component();
    const stack = page.stacks()[0];

    page.openDiscard(stack);

    expect(dialog.open).toHaveBeenCalledWith(DiscardEquipmentDialogComponent, {
      width: '420px',
      maxWidth: '95vw',
      data: { buildingId: 3, stack },
    });
  });

  it('ferme la fenêtre', () => {
    component().close();
    expect(dialogRef.close).toHaveBeenCalled();
  });
});
