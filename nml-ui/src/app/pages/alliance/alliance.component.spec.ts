import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { AllianceComponent } from './alliance.component';
import { AllianceStateService } from '../../services/alliance-state.service';
import { PlayerService } from '../../services/player.service';
import { ApiService } from '../../services/api.service';

describe('AllianceComponent', () => {
  const state = {
    me: signal(null),
    loading: signal(false),
    error: signal<string | null>(null),
    loadMe: jest.fn().mockResolvedValue(undefined),
    propose: jest.fn().mockResolvedValue(true),
    accept: jest.fn().mockResolvedValue(true),
    decline: jest.fn().mockResolvedValue(true),
    withdraw: jest.fn().mockResolvedValue(true),
    betray: jest.fn().mockResolvedValue(true),
  };
  const playerService = {
    player: signal({ id: 1, name: 'lurio' }),
    loadCurrent: jest.fn().mockResolvedValue(undefined),
  };
  const api = {
    getPlayerSummaries: jest.fn(() => of({ content: [{ id: 2, name: 'nursek' }] })),
  };
  const snackBar = { open: jest.fn() };
  const dialog = { open: jest.fn().mockReturnValue({ afterClosed: () => of(true) }) };
  const dialogRef = { close: jest.fn() };

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: AllianceStateService, useValue: state },
        { provide: PlayerService, useValue: playerService },
        { provide: ApiService, useValue: api },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialog, useValue: dialog },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    });
  });

  function component(): AllianceComponent {
    return TestBed.runInInjectionContext(() => new AllianceComponent());
  }

  it('propose une alliance à la cible choisie', async () => {
    const page = component();
    page.targetPlayerId.set(2);

    await page.propose();

    expect(state.propose).toHaveBeenCalledWith('ALLIANCE', 2);
    expect(snackBar.open).toHaveBeenCalledWith('Proposition envoyée', 'OK', { duration: 3000 });
  });

  it('ignore une proposition sans cible', async () => {
    await component().propose();
    expect(state.propose).not.toHaveBeenCalled();
  });

  it('accepte, refuse et retire les propositions', async () => {
    const page = component();

    await page.accept(10);
    await page.decline(11);
    await page.withdraw(12);

    expect(state.accept).toHaveBeenCalledWith(10);
    expect(state.decline).toHaveBeenCalledWith(11);
    expect(state.withdraw).toHaveBeenCalledWith(12);
    expect(snackBar.open).toHaveBeenCalledTimes(3);
  });

  it('ne notifie pas quand le service refuse l’action', async () => {
    state.accept.mockResolvedValueOnce(false);

    await component().accept(10);

    expect(snackBar.open).not.toHaveBeenCalled();
  });

  it('trahit seulement après confirmation', async () => {
    dialog.open.mockReturnValueOnce({ afterClosed: () => of(false) });
    const page = component();
    await page.betray(3, 'nursek');
    expect(state.betray).not.toHaveBeenCalled();

    await page.betray(3, 'nursek');
    expect(state.betray).toHaveBeenCalledWith(3);
    expect(snackBar.open).toHaveBeenCalledWith(
      'Trahison de nursek — annonce au tour suivant',
      'OK',
      { duration: 3000 },
    );
  });

  it('charge les joueurs et ferme la fenêtre', async () => {
    const page = component();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(api.getPlayerSummaries).toHaveBeenCalled();
    expect(page.players()).toEqual([{ id: 2, name: 'nursek' }]);

    page.close();
    expect(dialogRef.close).toHaveBeenCalled();
  });
});
