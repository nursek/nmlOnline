import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { CharacterAbilitiesDialogComponent } from './character-abilities-dialog.component';
import type { GameCharacter } from '../../models';

describe('CharacterAbilitiesDialogComponent', () => {
  const dialogRef = { close: jest.fn() };

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { playerName: 'lurio', character: { id: 1 } as GameCharacter },
        },
      ],
    });
  });

  function component(): CharacterAbilitiesDialogComponent {
    return TestBed.runInInjectionContext(() => new CharacterAbilitiesDialogComponent());
  }

  it('expose les trois emplacements de capacités et leurs libellés', () => {
    const page = component();

    expect(page.abilityList()).toHaveLength(3);
    expect(page.kindLabel('character')).toBeTruthy();
    expect(page.kindLabel('army')).toBeTruthy();
    expect(page.kindLabel('territory')).toBeTruthy();
  });

  it('ferme la fenêtre', () => {
    component().close();
    expect(dialogRef.close).toHaveBeenCalled();
  });
});
