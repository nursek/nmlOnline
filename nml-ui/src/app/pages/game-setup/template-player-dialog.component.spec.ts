import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TemplatePlayerDialogComponent } from './template-player-dialog.component';
import type { Board } from '../../models';

describe('TemplatePlayerDialogComponent', () => {
  const dialogRef = { close: jest.fn() };
  const board = {
    sectors: {
      1: { number: 1, ownerId: null },
      2: { number: 2, ownerId: 7 },
      3: { number: 3, ownerId: null },
      4: { number: 4, ownerId: null },
    },
  } as unknown as Board;
  const data = {
    board,
    spots: [
      [2, 3],
      [1, 4],
      [3, 4],
    ],
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

  function component(): TemplatePlayerDialogComponent {
    return TestBed.runInInjectionContext(() => new TemplatePlayerDialogComponent());
  }

  it('choisit le premier spot libre et bloque les spots occupés', () => {
    const page = component();

    expect(page.spotBlocked([2, 3])).toBe(true);
    expect(page.spotBlocked([1, 4])).toBe(false);
    expect(page.spotIndex()).toBe(1);
    expect(page.sectorNumbers()).toEqual([1, 4]);
    expect(page.canCreate()).toBe(false);

    page.name.set('  lurio  ');
    expect(page.canCreate()).toBe(true);
  });

  it('lit des secteurs manuels quand le preset n’a aucun spot', () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { board, spots: [] } },
      ],
    });
    const page = TestBed.runInInjectionContext(() => new TemplatePlayerDialogComponent());
    page.manualSectors.set('1, 4, x, 0');

    expect(page.sectorNumbers()).toEqual([1, 4]);
    expect(page.canCreate()).toBe(false);
    page.name.set('nursek');
    expect(page.canCreate()).toBe(true);
  });

  it('crée avec le nom nettoyé et un personnage optionnel', () => {
    const page = component();
    page.name.set('  lurio  ');
    page.characterName.set('   ');

    page.create();

    expect(dialogRef.close).toHaveBeenCalledWith({
      name: 'lurio',
      race: 'ORKS',
      sectorNumbers: [1, 4],
      characterName: undefined,
    });
  });

  it('annule sans créer', () => {
    component().cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
