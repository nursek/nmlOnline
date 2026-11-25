import { TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { BoardImportDialogComponent } from './board-import-dialog.component';

describe('BoardImportDialogComponent', () => {
  const dialogRef = { close: jest.fn() };
  const file = new File(['{}'], 'board.json', { type: 'application/json' });
  const map = new File(['img'], 'map.png', { type: 'image/png' });
  const svg = new File(['svg'], 'overlay.svg', { type: 'image/svg+xml' });

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [{ provide: MatDialogRef, useValue: dialogRef }],
    });
  });

  function component(): BoardImportDialogComponent {
    return TestBed.runInInjectionContext(() => new BoardImportDialogComponent());
  }

  function choose(page: BoardImportDialogComponent, key: 'file' | 'mapImage' | 'svgOverlay', value: File | null) {
    page.onFile({ target: { files: value ? [value] : [] } } as unknown as Event, key);
  }

  it('exige le board et un couple image/SVG cohérent', () => {
    const page = component();
    expect(page.canConfirm()).toBe(false);

    choose(page, 'file', file);
    expect(page.canConfirm()).toBe(true);

    choose(page, 'mapImage', map);
    expect(page.canConfirm()).toBe(false);

    choose(page, 'svgOverlay', svg);
    expect(page.canConfirm()).toBe(true);
  });

  it('renvoie le board avec ses assets', () => {
    const page = component();
    choose(page, 'file', file);
    choose(page, 'mapImage', map);
    choose(page, 'svgOverlay', svg);

    page.confirm();

    expect(dialogRef.close).toHaveBeenCalledWith({ file, mapImage: map, svgOverlay: svg });
  });

  it('renvoie le board sans assets quand ils sont absents', () => {
    const page = component();
    choose(page, 'file', file);

    page.confirm();

    expect(dialogRef.close).toHaveBeenCalledWith({
      file,
      mapImage: undefined,
      svgOverlay: undefined,
    });
  });

  it('annule sans importer', () => {
    component().cancel();
    expect(dialogRef.close).toHaveBeenCalled();
  });
});
