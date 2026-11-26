import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { PartiesComponent } from './parties.component';
import { InstanceService } from '../../services/instance.service';
import { AuthService } from '../../services/auth.service';
import type { Board, GameInstance } from '../../models';

describe('PartiesComponent', () => {
  const instances = signal<GameInstance[]>([
    {
      id: 1,
      name: 'Carte',
      currentTurn: 0,
      playerCount: 2,
      playerId: 1,
      playerName: 'lurio',
      eliminated: false,
      role: 'PLAYER',
      participant: true,
    },
    {
      id: 2,
      name: 'Autre',
      currentTurn: 3,
      playerCount: 2,
      playerId: null,
      playerName: null,
      eliminated: false,
      role: 'MJ',
      participant: false,
    },
  ]);
  const instanceService = {
    instances,
    loading: signal(false),
    error: signal<string | null>(null),
    selectedId: signal(1),
    refresh: jest.fn().mockResolvedValue(instances()),
    enter: jest.fn(),
  };
  const auth = { isAdmin: signal(false) };
  const dialog = { open: jest.fn() };

  beforeEach(() => {
    jest.clearAllMocks();
    instanceService.refresh.mockResolvedValue(instances());
    TestBed.configureTestingModule({
      providers: [
        { provide: InstanceService, useValue: instanceService },
        { provide: AuthService, useValue: auth },
        { provide: MatDialog, useValue: dialog },
      ],
    });
  });

  function component(): PartiesComponent {
    return TestBed.runInInjectionContext(() => new PartiesComponent());
  }

  it('rafraîchit les parties et entre dans une partie', () => {
    const page = component();
    expect(instanceService.refresh).toHaveBeenCalled();

    page.join(instances()[1]);

    expect(instanceService.enter).toHaveBeenCalledWith(instances()[1]);
  });

  it('adapte le libellé selon le tour et le rôle', () => {
    const page = component();
    const [preparation, running] = instances();

    expect(page.actionLabel(preparation)).toBe('Entrer en préparation');
    expect(page.actionLabel({ ...preparation, role: 'ADMIN' })).toBe('Préparer');
    expect(page.actionLabel({ ...preparation, role: 'MJ' })).toBe('Administrer');
    expect(page.actionLabel(running)).toBe('Administrer');
    expect(page.actionLabel({ ...running, role: 'PLAYER' })).toBe('Jouer');
  });

  it('crée une partie et rejoint celle qui vient d’être ouverte', async () => {
    dialog.open.mockReturnValueOnce({ afterClosed: () => of({ id: 2 } as Board) });
    const page = component();

    page.createGame();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(instanceService.refresh).toHaveBeenCalledTimes(2);
    expect(instanceService.enter).toHaveBeenCalledWith(instances()[1]);
  });

  it('ne rafraîchit pas si la création est annulée', async () => {
    dialog.open.mockReturnValueOnce({ afterClosed: () => of(undefined) });
    const page = component();
    instanceService.refresh.mockClear();

    page.createGame();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(instanceService.refresh).not.toHaveBeenCalled();
  });
});
