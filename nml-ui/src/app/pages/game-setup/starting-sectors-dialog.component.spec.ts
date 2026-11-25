import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { StartingSectorsDialogComponent } from './starting-sectors-dialog.component';
import type { Board, Sector } from '../../models';

describe('StartingSectorsDialogComponent', () => {
  const dialogRef = { close: jest.fn() };
  const board = {
    sectors: {
      2: { number: 2, name: 'Deux', ownerId: null },
      1: { number: 1, name: 'Un', ownerId: null },
      3: { number: 3, name: 'Trois', ownerId: 9 },
    },
  } as unknown as Board;
  const data = {
    playerId: 7,
    playerName: 'lurio',
    current: [1],
    board,
    spots: [
      [1, 2],
      [2, 3],
    ],
    playerNames: { 9: 'nursek' },
  };

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data },
      ],
    });
  });

  function component(): StartingSectorsDialogComponent {
    return TestBed.runInInjectionContext(() => new StartingSectorsDialogComponent());
  }

  it('trie les secteurs et distingue propriétaire courant et étranger', () => {
    const page = component();

    expect(page.sectors().map((sector) => sector.number)).toEqual([1, 2, 3]);
    expect(page.ownerLabel(page.sectors()[0])).toBeNull();
    expect(page.ownerLabel({ number: 4, ownerId: 7 } as Sector)).toBe('ce joueur');
    expect(page.ownerLabel(page.sectors()[2])).toBe('nursek');
    expect(page.blocked(page.sectors()[2])).toBe(true);
    expect(page.blocked(page.sectors()[0])).toBe(false);
  });

  it('sélectionne un spot libre et refuse un spot occupé', () => {
    const page = component();

    page.selectSpot([2, 3]);
    expect([...page.selected()]).toEqual([1]);

    page.selectSpot([1, 2]);
    expect([...page.selected()]).toEqual([1, 2]);
    expect(page.spotBlocked([2, 3])).toBe(true);
    expect(page.spotBlocked([1, 2])).toBe(false);
  });

  it('limite la sélection à trois secteurs', () => {
    const page = component();
    page.toggle(2, true);
    page.toggle(3, true);
    page.toggle(4, true);
    expect(page.selected().size).toBe(3);

    page.toggle(2, false);
    expect(page.selected().has(2)).toBe(false);
  });

  it('enregistre la sélection ou annule', () => {
    const page = component();
    page.selectSpot([1, 2]);

    page.save();
    expect(dialogRef.close).toHaveBeenCalledWith([1, 2]);

    page.cancel();
    expect(dialogRef.close).toHaveBeenLastCalledWith();
  });
});
