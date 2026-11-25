import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { PlacementModalComponent } from './placement-modal.component';
import type { Sector } from '../../models';

describe('PlacementModalComponent', () => {
  const dialogRef = { close: jest.fn() };
  const sectors = [
    { number: 1, name: 'Un', boardId: 1 },
    { number: 2, name: 'Deux', boardId: 1 },
    { number: null, name: 'Sans numéro', boardId: 1 },
    { number: 3, name: 'Sans board', boardId: null },
  ] as Sector[];

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { title: 'Déployer', ownedSectors: sectors } },
      ],
    });
  });

  function component(): PlacementModalComponent {
    return TestBed.runInInjectionContext(() => new PlacementModalComponent());
  }

  it('ne propose que les secteurs complets', () => {
    expect(component().eligibleSectors().map((sector) => sector.number)).toEqual([1, 2]);
  });

  it('confirme le secteur choisi ou annule', () => {
    const page = component();
    page.confirm();
    expect(dialogRef.close).not.toHaveBeenCalled();

    page.selectedSector.set(sectors[0]);
    page.confirm();
    expect(dialogRef.close).toHaveBeenCalledWith(sectors[0]);

    page.cancel();
    expect(dialogRef.close).toHaveBeenLastCalledWith(null);
  });
});
