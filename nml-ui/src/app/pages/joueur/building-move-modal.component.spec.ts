import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { BuildingMoveModalComponent } from './building-move-modal.component';
import type { Building, Sector } from '../../models';

describe('BuildingMoveModalComponent', () => {
  const dialogRef = { close: jest.fn() };
  const sectors = [
    { number: 1, name: 'Un', boardId: 1 },
    { number: 2, name: 'Deux', boardId: 1 },
    { number: 3, name: 'Trois', boardId: 1 },
  ] as Sector[];

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            building: { displayName: 'Banque', sectorNumber: 2, lastMovedTurn: 4 } as Building,
            ownedSectors: sectors,
          },
        },
      ],
    });
  });

  function component(): BuildingMoveModalComponent {
    return TestBed.runInInjectionContext(() => new BuildingMoveModalComponent());
  }

  it('exclut le secteur actuel du bâtiment', () => {
    expect(component().eligibleSectors().map((sector) => sector.number)).toEqual([1, 3]);
  });

  it('confirme ou annule le déplacement', () => {
    const page = component();
    page.confirm();
    expect(dialogRef.close).not.toHaveBeenCalled();

    page.selectedSector.set(sectors[2]);
    page.confirm();
    expect(dialogRef.close).toHaveBeenCalledWith(sectors[2]);

    page.cancel();
    expect(dialogRef.close).toHaveBeenLastCalledWith(null);
  });
});
