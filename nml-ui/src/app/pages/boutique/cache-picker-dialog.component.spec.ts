import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { CachePickerDialogComponent } from './cache-picker-dialog.component';
import type { CacheChoice } from './boutique.helpers';

describe('CachePickerDialogComponent', () => {
  const caches = [
    { id: 1, sectorNumber: 5, availableCapacity: 1 },
    { id: 2, sectorNumber: null, availableCapacity: 5 },
  ] as CacheChoice[];
  const dialogRef = { close: jest.fn() };

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { caches, totalQuantity: 3 } },
      ],
    });
  });

  function component(): CachePickerDialogComponent {
    return TestBed.runInInjectionContext(() => new CachePickerDialogComponent());
  }

  it('présélectionne la seule cache capable et décrit les secteurs', () => {
    const page = component();

    expect(page.hasRoom()).toBe(true);
    expect(page.canHost(caches[0])).toBe(false);
    expect(page.canHost(caches[1])).toBe(true);
    expect(page.selected()).toBe(2);
    expect(page.sectorLabel(caches[0])).toBe('secteur 5');
    expect(page.sectorLabel(caches[1])).toBe('hors secteur');
  });

  it('signale l’absence de cache assez grande', () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { caches: [caches[0]], totalQuantity: 3 } },
      ],
    });
    const page = TestBed.runInInjectionContext(() => new CachePickerDialogComponent());

    expect(page.hasRoom()).toBe(false);
    expect(page.selected()).toBeNull();
  });

  it('confirme la cache choisie ou annule', () => {
    const page = component();

    page.confirm();
    expect(dialogRef.close).toHaveBeenCalledWith({ cacheId: 2 });

    page.cancel();
    expect(dialogRef.close).toHaveBeenLastCalledWith(null);
  });

  it('ne confirme rien sans sélection', () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { caches: [caches[0]], totalQuantity: 3 } },
      ],
    });
    const page = TestBed.runInInjectionContext(() => new CachePickerDialogComponent());

    page.confirm();

    expect(dialogRef.close).not.toHaveBeenCalled();
  });
});
