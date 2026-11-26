import { TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { CreateGameDialogComponent } from './create-game-dialog.component';
import { ApiService } from '../../services/api.service';

describe('CreateGameDialogComponent', () => {
  const api = {
    adminGetBoardPresets: jest.fn(() =>
      of([{ key: 'main', name: 'Carte principale', sectorCount: 43, spotCount: 3 }]),
    ),
    adminGetBoardPreset: jest.fn(() => of({ key: 'main', board: { name: 'Carte' } })),
    createBoard: jest.fn(() => of({ id: 7, name: 'Ma partie' })),
    adminImportBoard: jest.fn(() => of({ id: 8, name: 'Importée' })),
    adminUploadBoardAssets: jest.fn(() => of({ mapImageUrl: 'm', svgOverlayUrl: 'o' })),
  };
  const dialogRef = { close: jest.fn() };
  const dialog = { open: jest.fn() };

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: ApiService, useValue: api },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatDialog, useValue: dialog },
      ],
    });
  });

  function component(): CreateGameDialogComponent {
    return TestBed.runInInjectionContext(() => new CreateGameDialogComponent());
  }

  it('charge les presets et sélectionne l’unique carte', async () => {
    const page = component();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(page.presets()).toHaveLength(1);
    expect(page.presetKey()).toBe('main');
    expect(page.canCreate()).toBe(false);

    page.name.set('  Ma partie  ');
    expect(page.canCreate()).toBe(true);
  });

  it('crée la partie avec le nom nettoyé et ferme le dialogue', async () => {
    const page = component();
    await new Promise((resolve) => setTimeout(resolve, 0));
    page.name.set('  Ma partie  ');

    await page.create();

    expect(api.createBoard).toHaveBeenCalledWith(
      expect.objectContaining({ id: 0, name: 'Ma partie', presetKey: 'main' }),
    );
    expect(dialogRef.close).toHaveBeenCalledWith({ id: 7, name: 'Ma partie' });
    expect(page.creating()).toBe(false);
  });

  it('importe un board et ferme sur le board créé', async () => {
    dialog.open.mockReturnValueOnce({
      afterClosed: () => of({ file: new File(['{}'], 'board.json', { type: 'application/json' }) }),
    });
    const page = component();

    await page.importBoard();

    expect(api.adminImportBoard).toHaveBeenCalledWith(
      expect.any(File),
      undefined,
      undefined,
      undefined,
    );
    expect(dialogRef.close).toHaveBeenCalledWith({ id: 8, name: 'Importée' });
  });

  it('annule sans créer', () => {
    dialog.open.mockReturnValueOnce({ afterClosed: () => of(undefined) });
    const page = component();

    page.cancel();

    expect(api.createBoard).not.toHaveBeenCalled();
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('expose l’erreur de chargement des presets', async () => {
    api.adminGetBoardPresets.mockReturnValueOnce(
      throwError(() => ({ error: { detail: 'Catalogue indisponible' } })),
    );
    const page = component();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(page.error()).toBe('Catalogue indisponible');
  });
});
